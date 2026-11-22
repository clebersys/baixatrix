# Plano: extração local de vídeos do YouTube

## Visão geral

Substituir a configuração de backend da versão 2.3.0 por extração local via
NewPipe Extractor. O app continuará usando DownloadManager, player e biblioteca
existentes; apenas a resolução do YouTube muda. Publicar como 2.4.0 no GitHub,
fora da Play Store.

## Decisões de arquitetura

- Usar NewPipe Extractor 0.27.1, a versão oficial mais recente consultada, e um
  downloader OkHttp sem cookies.
- Inicializar o extractor uma vez no `Application` e executar chamadas de rede
  em `Dispatchers.IO`.
- Usar streams muxed quando disponíveis; caso contrário, parear MP4 H.264/HEVC
  progressive com AAC M4A progressive e multiplexar localmente via MediaMuxer.
  Não reencodificar nem aceitar codecs não compatíveis.
- Manter o servidor de `backend/` como ferramenta independente, mas remover toda
  dependência de servidor/chave da interface e do caminho do app.
- Alterar a licença do projeto para GPL-3.0-or-later, compatível com a biblioteca.
- Release final: APK assinado com a mesma keystore, versão 2.4.0/versionCode 10,
  tag `v2.4.0`, APK e SHA-256 anexados à GitHub Release; nenhum AAB ou Play Store.

## Tarefas

### Fase 1: dependência e inicialização do extractor
- [x] Adicionar JitPack, NewPipe Extractor, desugaring NIO e regras de shrinker.
- [x] Implementar downloader OkHttp e inicialização única no app.

### Checkpoint 1
- [x] APK compila e os testes unitários passam.

### Fase 2: fluxo local YouTube
- [x] Trocar resolução HTTP do backend por `StreamInfo` local.
- [x] Mapear MP4 progressive e parear faixas H.264/HEVC + AAC, validando URLs/thumbnail.
- [x] Adicionar mux local sem reencodificação e preview de faixas separadas.
- [x] Testar seleção, erros e manutenção do comportamento Instagram/Kwai.

### Fase 3: remoção da configuração e conformidade
- [x] Remover campos e armazenamento do backend na tela Configurações.
- [x] Atualizar README e políticas para declarar processamento local.
- [x] Alterar LICENSE para GPL-3.0-or-later e registrar NewPipe em terceiros.

### Checkpoint 2
- [x] Nenhuma URL ou chave de backend é necessária no app.
- [x] Testes unitários passam; o build release ainda será verificado.

### Fase 4: release
- [ ] Atualizar changelog e versionCode/versionName para 10/2.4.0.
- [ ] Construir APK assinado, verificar versão, assinatura e SHA-256.
- [ ] Commit, tag anotada, push de `main`/tag, GitHub Release e verificar assets.
- [ ] Restaurar a conta GitHub ativa anterior após publicação.

## Riscos e mitigação

| Risco | Impacto | Mitigação |
|---|---|---|
| YouTube muda APIs internas | Extração pode parar para alguns links | Usar versão estável mantida do NewPipe Extractor; mostrar erro recuperável |
| Vídeo só oferece faixas adaptativas | Download depende de mux local | Combinar faixas compatíveis sem reencodificar; rejeitar codecs não suportados |
| CDN rejeita headers | Player/download pode retornar 403 | Usar o User-Agent alinhado ao downloader NewPipe |
| Relicenciamento | Publicação sob MIT deixa de ser correta com a dependência GPL | Alterar a licença antes de distribuir a versão 2.4.0 |

## Open questions

- Nenhuma. O usuário aprovou extração local, GPL-3.0-or-later e publicação no GitHub.
