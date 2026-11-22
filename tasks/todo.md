# Tarefas: extração local do YouTube e release 2.4.0

## Tarefa 1: dependência e downloader local — concluída
- [x] Adicionar NewPipe Extractor 0.27.1, JitPack, desugaring NIO e regras R8.
- [x] Implementar downloader OkHttp e inicialização única no app.
- [x] `./gradlew testDebugUnitTest` passou.

## Tarefa 2: extração local e mux — concluída
- [x] Obter StreamInfo localmente; selecionar streams MP4/muxed ou pares H.264/HEVC + AAC.
- [x] Validar URLs e combinar localmente com MediaExtractor/MediaMuxer sem reencodificar.
- [x] Atualizar preview para tocar vídeo + áudio em streams separados.
- [x] Testes unitários de seleção e validação passaram.
- [ ] Smoke test real em aparelho: sem dispositivo conectado; o IP de build recebeu `SignInConfirmNotBotException` do YouTube.

## Tarefa 3: remover configuração e atualizar licença/docs — concluída
- [x] Remover campos e preferências do backend da tela Configurações e do fluxo app.
- [x] Atualizar README, privacidade e avisos para extração local.
- [x] Mudar licença para GPL-3.0-or-later e registrar NewPipe Extractor.

## Checkpoint 2
- [x] App não pede URL/chave/backend.
- [x] Testes unitários Android passaram.

## Tarefa 4: release 2.4.0 — em andamento
- [ ] Atualizar versionCode/versionName e changelog.
- [ ] Montar APK release assinado e validar assinatura/versão/SHA-256.
- [ ] Commit, tag, push e GitHub Release com APK + checksum.
- [ ] Verificar assets e restaurar conta GitHub ativa anterior.

## Checkpoint final
- [ ] Release GitHub verificada; não publicar na Play Store.
