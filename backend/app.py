"""
Servidor de extração opcional do BaixaVídeo.

O app tenta extrair o vídeo sozinho (Instagram e Kwai). Quando o Instagram
passa a exigir login, este servidor resolve o link com yt-dlp — inclusive
usando cookies de uma conta sua — e devolve o arquivo direto para o celular.

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
from typing import Any

from fastapi import FastAPI, HTTPException, Query, Request
from fastapi.responses import JSONResponse
from yt_dlp import YoutubeDL

app = FastAPI(title="BaixaVídeo Resolver", version="1.0.0")

COOKIES_FILE = os.environ.get("YTDLP_COOKIES", "")
API_KEY = os.environ.get("API_KEY", "")


def ydl_options() -> dict[str, Any]:
    options: dict[str, Any] = {
        "quiet": True,
        "no_warnings": True,
        "noplaylist": True,
        "skip_download": True,
        "nocheckcertificate": True,
        "socket_timeout": 25,
        "format": "bv*+ba/b",
    }
    if COOKIES_FILE and os.path.exists(COOKIES_FILE):
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
    url: str = Query(..., description="Link do vídeo"),
) -> JSONResponse:
    if API_KEY and request.headers.get("x-api-key") != API_KEY:
        raise HTTPException(status_code=401, detail="API key inválida")

    try:
        with YoutubeDL(ydl_options()) as ydl:
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
    if not variants and direct_url:
        variants = [{"url": direct_url, "height": info.get("height")}]

    if not variants:
        return JSONResponse(
            status_code=422,
            content={
                "status": "error",
                "text": "Nenhum arquivo de vídeo encontrado (conteúdo privado ou removido).",
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
            "platform": info.get("extractor_key"),
        }
    )
