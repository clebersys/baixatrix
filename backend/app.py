"""
Backend pessoal do BaixaTrix para resolver links permitidos com yt-dlp.

O app pode enviar links do YouTube a este serviço configurado pelo usuário. O
endpoint também preserva os resolvedores existentes para Instagram e Kwai.

Uso:
    GET /resolve?url=https://www.instagram.com/reel/XXXX/

Resposta:
    {
      "status": "ok",
      "url": "https://.../video.mp4",
      "title": "...",
      "author": "...",
      "thumbnail": "https://...",
      "duration": 12.5,
      "variants": [{"url": "...", "height": 1080}]
    }
"""

import os
import re
from typing import Any
from urllib.parse import parse_qs, urlsplit

from fastapi import FastAPI, HTTPException, Query, Request
from fastapi.responses import JSONResponse
from yt_dlp import YoutubeDL

app = FastAPI(title="BaixaTrix Resolver", version="1.0.0")

COOKIES_FILE = os.environ.get("YTDLP_COOKIES", "")
API_KEY = os.environ.get("API_KEY", "")


YOUTUBE_HOSTS = {
    "youtube.com",
    "www.youtube.com",
    "m.youtube.com",
    "music.youtube.com",
    "youtu.be",
}
INSTAGRAM_HOSTS = {"instagram.com", "www.instagram.com", "instagr.am"}
KWAI_HOSTS = {
    "kwai.com",
    "www.kwai.com",
    "k.kwai.com",
    "kwai.app",
    "kwai.video",
    "kwai-video.com",
}


def validate_source_url(url: str) -> str:
    """Allow only known video platform URLs before passing input to yt-dlp."""
    if len(url) > 2048:
        raise HTTPException(status_code=422, detail="Link inválido")

    try:
        parsed = urlsplit(url)
        host = (parsed.hostname or "").lower().rstrip(".")
        port = parsed.port
    except ValueError as error:
        raise HTTPException(status_code=422, detail="Link inválido") from error

    if (
        parsed.scheme.lower() != "https"
        or not host
        or parsed.username is not None
        or parsed.password is not None
        or port not in (None, 443)
    ):
        raise HTTPException(status_code=422, detail="Link inválido")

    if host in YOUTUBE_HOSTS:
        path = parsed.path.rstrip("/")
        segments = [segment for segment in path.split("/") if segment]
        video_id = None
        if path == "/watch":
            video_id = (parse_qs(parsed.query).get("v") or [None])[0]
        elif host == "youtu.be" and segments:
            video_id = segments[0]
        elif len(segments) >= 2 and segments[0] in {"shorts", "live", "embed"}:
            video_id = segments[1]
        if video_id and re.fullmatch(r"[A-Za-z0-9_-]{11}", video_id):
            return "youtube"

    if host in INSTAGRAM_HOSTS:
        return "instagram"
    if host in KWAI_HOSTS:
        return "kwai"

    raise HTTPException(status_code=422, detail="Plataforma de vídeo não suportada")


def ydl_options(use_cookies: bool = True) -> dict[str, Any]:
    options: dict[str, Any] = {
        "quiet": True,
        "no_warnings": True,
        "noplaylist": True,
        "skip_download": True,
        "socket_timeout": 25,
        "format": "bv*+ba/b",
    }
    if use_cookies and COOKIES_FILE and os.path.exists(COOKIES_FILE):
        options["cookiefile"] = COOKIES_FILE
    return options


def pick_variants(info: dict[str, Any]) -> list[dict[str, Any]]:
    """Prefere MP4 com áudio embutido; mantém as melhores resoluções."""
    formats = info.get("formats") or []
    progressive = [
        f
        for f in formats
        if f.get("url")
        and f.get("vcodec") not in (None, "none")
        and f.get("acodec") not in (None, "none")
        and (f.get("ext") == "mp4" or str(f.get("url", "")).endswith(".mp4"))
    ]
    progressive.sort(key=lambda f: f.get("height") or 0, reverse=True)

    variants = []
    seen_heights = set()
    for item in progressive:
        height = item.get("height") or 0
        if height in seen_heights:
            continue
        seen_heights.add(height)
        variants.append(
            {
                "url": item["url"],
                "height": height or None,
                "width": item.get("width") or None,
                "bitrate": item.get("tbr"),
                "mimeType": "video/mp4",
            }
        )
    return variants


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.get("/resolve")
def resolve(
    request: Request,
    url: str = Query(..., min_length=1, max_length=2048, description="Link do vídeo"),
) -> JSONResponse:
    if API_KEY and request.headers.get("x-api-key") != API_KEY:
        raise HTTPException(status_code=401, detail="API key inválida")

    source_platform = validate_source_url(url)

    try:
        with YoutubeDL(ydl_options(use_cookies=source_platform != "youtube")) as ydl:
            info = ydl.extract_info(url, download=False)
    except Exception as error:  # noqa: BLE001 - repassamos a mensagem ao app
        return JSONResponse(
            status_code=422,
            content={"status": "error", "text": str(error)[:300]},
        )

    if info.get("_type") == "playlist" and info.get("entries"):
        info = info["entries"][0]

    variants = pick_variants(info)
    direct_url = info.get("url")
    if source_platform != "youtube" and not variants and direct_url:
        variants = [{"url": direct_url, "height": info.get("height")}]

    if not variants:
        return JSONResponse(
            status_code=422,
            content={
                "status": "error",
                "text": "Nenhum formato MP4 progressivo com áudio e vídeo foi encontrado.",
            },
        )

    return JSONResponse(
        content={
            "status": "ok",
            "url": variants[0]["url"],
            "variants": variants,
            "title": (info.get("title") or "")[:140] or None,
            "author": info.get("uploader") or info.get("channel"),
            "thumbnail": info.get("thumbnail"),
            "duration": info.get("duration"),
            "platform": source_platform,
        }
    )
