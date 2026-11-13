# Servidor de extração do BaixaVídeo

O app resolve Instagram e Kwai sozinho na maioria dos casos. Este servidor é a
alternativa para quando o Instagram exige login: ele usa `yt-dlp` (com os seus
cookies, se você fornecer) e devolve o link direto do arquivo de vídeo.

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

## Configurando no app

1. Abra o **BaixaVídeo** → **Ajustes**.
2. Em *Servidor de extração*, coloque `http://SEU_IP:8000/resolve`.
3. Salve. A partir daí, se a extração local falhar, o app usa o servidor.

Em redes móveis o celular não enxerga o IP da sua LAN. Para usar fora de casa,
publique o serviço com um túnel (Cloudflare Tunnel, Tailscale, ngrok) e use a
URL pública com HTTPS.

## Cookies (Instagram)

O Instagram bloqueia boa parte dos vídeos para quem não está logado. Para o
servidor resolver com uma conta sua:

1. Exporte os cookies do Instagram no formato Netscape (extensão
   *Get cookies.txt LOCALLY*, por exemplo).
2. Salve como `backend/data/cookies.txt`.
3. Suba o serviço com `YTDLP_COOKIES=/data/cookies.txt` (já é o padrão no
   `docker-compose.yml`).

Use somente uma conta sua. Cookies dão acesso à conta: mantenha o servidor
privado e nunca compartilhe o arquivo.

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
