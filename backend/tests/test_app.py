import unittest
import json
from types import SimpleNamespace
from unittest.mock import patch

from fastapi import HTTPException

import app


class UrlValidationTests(unittest.TestCase):
    def test_accepts_supported_youtube_video_hosts(self):
        for url in (
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://m.youtube.com/shorts/dQw4w9WgXcQ",
            "https://music.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtu.be/dQw4w9WgXcQ",
        ):
            with self.subTest(url=url):
                self.assertEqual(app.validate_source_url(url), "youtube")

    def test_preserves_existing_instagram_and_kwai_sources(self):
        self.assertEqual(
            app.validate_source_url("https://www.instagram.com/reel/abc123/"),
            "instagram",
        )
        self.assertEqual(
            app.validate_source_url("https://kwai-video.com/p/abc123"),
            "kwai",
        )

    def test_rejects_non_platform_or_malformed_urls(self):
        for url in (
            "http://www.youtube.com/watch?v=abc123",
            "https://youtube.com.example.org/watch?v=abc123",
            "https://youtube.com@127.0.0.1/watch?v=abc123",
            "https://127.0.0.1:8000/",
            "https://youtube.com:8443/watch?v=abc123",
            "https://www.youtube.com/watch?v=short",
            "https://www.youtube.com/playlist?list=PL1234567890",
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ&x=" + "a" * 2048,
            "file:///etc/passwd",
            "not a url",
        ):
            with self.subTest(url=url):
                with self.assertRaises(HTTPException):
                    app.validate_source_url(url)


class FormatTests(unittest.TestCase):
    def test_selects_only_progressive_mp4_with_audio_and_video(self):
        variants = app.pick_variants(
            {
                "formats": [
                    {"url": "https://cdn.example/1080.mp4", "height": 1080,
                     "ext": "mp4", "vcodec": "avc1", "acodec": "none"},
                    {"url": "https://cdn.example/audio.m4a", "ext": "m4a",
                     "vcodec": "none", "acodec": "mp4a"},
                    {"url": "https://cdn.example/720.webm", "height": 720,
                     "ext": "webm", "vcodec": "vp9", "acodec": "opus"},
                    {"url": "https://cdn.example/360.mp4", "height": 360,
                     "ext": "mp4", "vcodec": "avc1", "acodec": "mp4a"},
                ]
            }
        )

        self.assertEqual([variant["height"] for variant in variants], [360])

    def test_youtube_does_not_read_cookie_file(self):
        with patch.object(app, "COOKIES_FILE", "/tmp/youtube-cookies.txt"):
            self.assertNotIn("cookiefile", app.ydl_options(use_cookies=False))


class ResolveBoundaryTests(unittest.TestCase):
    def test_unsupported_url_is_rejected_before_extractor_runs(self):
        request = SimpleNamespace(headers={})
        with patch.object(app, "YoutubeDL") as youtube_dl:
            with self.assertRaises(HTTPException):
                app.resolve(request, "https://127.0.0.1:8000/secret")

        youtube_dl.assert_not_called()

    def test_youtube_resolution_skips_cookies_and_uses_platform_label(self):
        request = SimpleNamespace(headers={})
        captured = {}

        class FakeYoutubeDL:
            def __init__(self, options):
                captured["options"] = options

            def __enter__(self):
                return self

            def __exit__(self, *_args):
                return None

            def extract_info(self, _url, download):
                captured["download"] = download
                return {
                    "title": "Autorizado",
                    "formats": [
                        {
                            "url": "https://cdn.example/video.mp4",
                            "ext": "mp4",
                            "height": 720,
                            "vcodec": "avc1",
                            "acodec": "mp4a",
                        }
                    ],
                }

        with patch.object(app, "COOKIES_FILE", "/tmp/youtube-cookies.txt"):
            with patch.object(app, "YoutubeDL", FakeYoutubeDL):
                response = app.resolve(
                    request,
                    "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                )

        payload = json.loads(response.body)
        self.assertNotIn("cookiefile", captured["options"])
        self.assertFalse(captured["download"])
        self.assertEqual(payload["platform"], "youtube")
        self.assertEqual(payload["variants"][0]["height"], 720)

    def test_existing_instagram_direct_url_fallback_remains_available(self):
        request = SimpleNamespace(headers={})

        class FakeYoutubeDL:
            def __init__(self, _options):
                pass

            def __enter__(self):
                return self

            def __exit__(self, *_args):
                return None

            def extract_info(self, _url, download):
                return {"url": "https://cdn.example/reel.mp4", "title": "Reel"}

        with patch.object(app, "YoutubeDL", FakeYoutubeDL):
            response = app.resolve(
                request,
                "https://www.instagram.com/reel/ABC123/",
            )

        payload = json.loads(response.body)
        self.assertEqual(payload["platform"], "instagram")
        self.assertEqual(payload["url"], "https://cdn.example/reel.mp4")

    def test_youtube_rejects_separate_formats_even_with_top_level_url(self):
        request = SimpleNamespace(headers={})

        class FakeYoutubeDL:
            def __init__(self, _options):
                pass

            def __enter__(self):
                return self

            def __exit__(self, *_args):
                return None

            def extract_info(self, _url, download):
                return {
                    "url": "https://cdn.example/video-only.mp4",
                    "formats": [
                        {
                            "url": "https://cdn.example/video-only.mp4",
                            "ext": "mp4",
                            "height": 1080,
                            "vcodec": "avc1",
                            "acodec": "none",
                        }
                    ],
                }

        with patch.object(app, "YoutubeDL", FakeYoutubeDL):
            response = app.resolve(
                request,
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            )

        self.assertEqual(response.status_code, 422)


if __name__ == "__main__":
    unittest.main()
