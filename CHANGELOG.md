# Changelog

## [2.3.0] - 2026-11-22

### Adicionado
- Suporte pessoal a vídeos e Shorts do YouTube por meio de backend `yt-dlp`
  configurável por HTTPS.
- Prévia e download dos formatos MP4 progressivos disponíveis, sem combinar
  faixas separadas.
- Configuração privada do endereço e da chave do backend no app.

### Segurança e privacidade
- O backend valida os hosts de origem e não usa cookies para links do YouTube.
- A porta do backend Docker fica vinculada a `127.0.0.1`; o acesso do app deve
  passar por HTTPS.
- O recurso do YouTube é destinado a uso pessoal fora da Play Store. Baixe
  somente vídeos próprios ou autorizados e respeite os termos da plataforma.
