# Backend do BaixaTrix (uso pessoal)

Este backend FastAPI + `yt-dlp` pode resolver links das plataformas suportadas,
incluindo YouTube, para o app BaixaTrix. O link enviado pelo app será recebido
por este servidor configurado pelo próprio usuário. Use somente vídeos seus ou
que você tenha autorização para baixar; respeite os direitos autorais e os
termos das plataformas.

O container inclui Deno para resolver os desafios JavaScript exigidos pelo
YouTube e instala o grupo `yt-dlp[default]`, que contém os scripts EJS usados
pelo extrator. Referências oficiais: [guia EJS do yt-dlp](https://github.com/yt-dlp/yt-dlp/wiki/EJS)
e [imagem Docker do Deno](https://hub.docker.com/r/denoland/deno).

## Rodando com Docker

```bash
cd backend
cp .env.example .env
# Edite .env e substitua API_KEY por um segredo gerado com: openssl rand -hex 32
docker compose up -d --build
```

O serviço escuta somente em `127.0.0.1:8000`. Configure um proxy reverso ou
túnel privado com HTTPS para disponibilizá-lo ao celular. Não exponha a porta
8000 diretamente à Internet. Configure no app a URL HTTPS pública/privada do
proxy e a mesma `API_KEY` definida em `.env`.

## Rodando sem Docker

Instale primeiro o Deno 2.3+ e deixe `deno` no `PATH` ([instruções oficiais](https://docs.deno.com/runtime/getting_started/installation/)).
O yt-dlp usa esse runtime e os scripts `yt-dlp-ejs` para resolver vídeos do YouTube.

```bash
cd backend
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
export API_KEY="$(openssl rand -hex 32)"
uvicorn app:app --host 127.0.0.1 --port 8000
```

Para conectar o app, ainda é necessário expor esse serviço por um proxy reverso
ou túnel privado com HTTPS e configurar a `API_KEY` no ambiente.

## Cookies (Instagram)

O Instagram bloqueia boa parte dos vídeos para quem não está logado. Para o
servidor resolver com uma conta sua:

1. Exporte os cookies do Instagram no formato Netscape (extensão
   *Get cookies.txt LOCALLY*, por exemplo).
2. Salve como `backend/data/cookies.txt`.
3. Suba o serviço com `YTDLP_COOKIES=/data/cookies.txt` (já é o padrão no
   `docker-compose.yml`).

Cookies podem permitir que o `yt-dlp` acesse conteúdo que a conta consegue ver,
inclusive posts privados. Use-os somente para provedores existentes que você
tem autorização para acessar, mantenha o servidor privado e nunca compartilhe o
arquivo. O backend desativa cookies para links do YouTube e não oferece acesso a
vídeos privados do YouTube.

## YouTube

- São aceitos links de vídeo/Shorts dos hosts `youtube.com`, `m.youtube.com`,
  `music.youtube.com` e `youtu.be`.
- O backend retorna somente MP4 progressivo com áudio e vídeo no mesmo arquivo.
  A disponibilidade de resoluções depende dos formatos oferecidos pelo YouTube;
  faixas separadas não são combinadas.
- O backend rejeita esquemas e hosts não permitidos antes de chamar `yt-dlp`.
- Links expirados podem exigir que você resolva o vídeo novamente no app.

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

## Testes

```bash
python3 -m unittest discover -s tests
```
