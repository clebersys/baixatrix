# Changelog

## [2.4.0] - 2026-11-22

### Alterado
- A extração de vídeos públicos do YouTube agora acontece diretamente no
  aparelho, sem URL de backend, chave de API ou configuração extra.
- Quando o YouTube fornece faixas separadas, o app combina H.264/HEVC + AAC no
  aparelho, sem reencodificar.
- A licença do projeto passou para GPL-3.0-or-later para compatibilidade com o
  NewPipe Extractor.

### Limitações
- O app precisa de MP4 progressivo ou faixas H.264/HEVC + AAC compatíveis para montar
  o arquivo. Mantenha-o aberto durante a combinação local.
- Use somente conteúdo próprio ou autorizado e respeite os termos do YouTube.

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
