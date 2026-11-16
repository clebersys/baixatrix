# Backend independente de desenvolvimento

Este backend FastAPI + `yt-dlp` é uma ferramenta independente para testes e
desenvolvimento. **Ele não está integrado ao aplicativo BaixaTrix 2.0**: o app
não tem opção para configurar um servidor nem envia links para este serviço.

## Rodando com Docker

```bash
cd backend
docker compose up -d --build
```

O serviço sobe em `http://SEU_IP:8000`.

## Rodando sem Docker

```bash
cd backend
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 8000
```

## Cookies (Instagram)

O Instagram bloqueia boa parte dos vídeos para quem não está logado. Para o
servidor resolver com uma conta sua:

1. Exporte os cookies do Instagram no formato Netscape (extensão
   *Get cookies.txt LOCALLY*, por exemplo).
2. Salve como `backend/data/cookies.txt`.
3. Suba o serviço com `YTDLP_COOKIES=/data/cookies.txt` (já é o padrão no
   `docker-compose.yml`).

Cookies podem permitir que o `yt-dlp` acesse conteúdo que a conta consegue ver,
inclusive posts privados. Isso é comportamento do backend externo, não suporte
nativo do BaixaTrix. Use somente conteúdo seu ou autorizado; mantenha o servidor
privado e nunca compartilhe o arquivo de cookies. Esta integração não está
disponível na interface do aplicativo.

## API

```
GET /resolve?url=https://www.instagram.com/reel/XXXX/
```

```json
{
  "status": "ok",
  "url": "https://.../video.mp4",
  "variants": [{ "url": "https://.../video.mp4", "height": 1080 }],
  "title": "...",
  "author": "...",
  "thumbnail": "https://...",
  "duration": 12.5
}
```

Em caso de erro: HTTP 422 com `{"status":"error","text":"..."}`.
Se `API_KEY` estiver definida, envie o cabeçalho `X-Api-Key`.
