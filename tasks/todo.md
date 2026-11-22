# Tarefas: suporte a vídeos do YouTube

## Tarefa 1: restringir e testar o backend — concluída
- [x] Validar esquema/host antes de invocar `yt-dlp`; cookies não são usados para YouTube.
- [x] Testar URLs aceitas/recusadas, formatos progressivos e fallback de Instagram.
- [x] Testes: 9 passaram com `PYTHONPATH=/tmp/baixatrix-backend-deps312 python3 -m unittest discover -s tests`.
- [x] `docker compose config` validado com chave de teste.
- [x] Imagem Docker construída e Deno/EJS conferidos no container.

## Tarefa 2: reconhecer links YouTube no Android — concluída
- [x] Reconhecer watch, Shorts, youtu.be e hosts previstos; recusar domínios falsos, HTTP e links sem ID.
- [x] Testes unitários cobrem links YouTube e preservam Instagram/Kwai.
- [x] `./gradlew testDebugUnitTest` passou.

## Tarefa 3: resolver pelo backend e configurar no app — concluída
- [x] Guardar URL HTTPS e chave opcional nas preferências privadas do app.
- [x] Resolver pelo contrato existente e validar formato, CDN e miniatura da resposta.
- [x] Exibir os campos de configuração e avisar que links YouTube são enviados ao backend.
- [x] `./gradlew testDebugUnitTest` e `./gradlew assembleDebug` passaram.

## Tarefa 4: documentação e revisão final — concluída
- [x] Documentar deploy, HTTPS, EJS/Deno, configuração, limitações e uso autorizado.
- [x] `./gradlew testDebugUnitTest` e `./gradlew assembleDebug` passaram.
- [x] 9 testes Python passaram; `py_compile` passou.
- [x] Docker Compose foi validado e imagem construída com Deno/EJS conferidos.
- [x] `git diff --check` passou; nenhum endpoint pessoal ou segredo foi adicionado.

## Checkpoint final
- [x] Critérios implementados; release não foi gerada nem publicada.
