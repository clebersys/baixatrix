# Spec: suporte a vídeos do YouTube

## Objetivo

Adicionar ao BaixaTrix, para uso pessoal fora da Play Store, a resolução e o
download de links de vídeos e Shorts do YouTube. O app enviará o link ao backend
`yt-dlp` que o usuário administra; Instagram e Kwai continuarão sendo resolvidos
localmente.

Critérios de aceite:

- Links `youtube.com`, `m.youtube.com`, `music.youtube.com` e `youtu.be` são
  reconhecidos; domínios parecidos ou URLs não HTTPS/HTTP são recusados.
- O backend só resolve hosts conhecidos (YouTube para esta integração e os
  provedores já documentados no backend) e devolve formatos MP4 progressivos
  (áudio e vídeo juntos), compatíveis com o DownloadManager atual.
- O app mostra prévia e permite baixar usando o fluxo existente.
- O endereço do backend (e chave, se configurada) pode ser informado nas opções;
  o usuário é avisado que o link é enviado a esse servidor.
- Sem backend configurado, a mensagem de erro orienta a configurar o serviço.
- Instagram/Kwai e seus testes existentes continuam funcionando.
- Não são usados cookies do YouTube, formatos privados ou faixas separadas que
  dependam de mesclagem.

## Stack e contrato

- Android: Kotlin, Jetpack Compose, OkHttp e JUnit 4 já usados pelo projeto.
- Backend: FastAPI + `yt-dlp` já usados em `backend/`.
- Contrato existente `GET /resolve?url=<URL>`: resposta `status=ok` com `url`,
  `variants`, `title`, `author`, `thumbnail`, `duration` e `platform`; erros
  retornam `status=error` e `text`. A chave, quando configurada no serviço, é
  enviada no cabeçalho `X-Api-Key`.

## Comandos

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
cd backend && python3 -m unittest discover -s tests
```

## Estrutura

- `app/src/main/java/com/cleber/baixatrix/util/LinkParser.kt`: detecção de links.
- `app/src/main/java/com/cleber/baixatrix/data/resolver/`: resolução YouTube via
  backend, sem alterar os resolvedores locais existentes.
- `app/src/main/java/com/cleber/baixatrix/ui/SettingsScreen.kt`: configuração do
  endereço/chave e aviso de privacidade.
- `backend/app.py`: validação estrita de hosts e extração YouTube sem cookies.
- Testes Android em `app/src/test/`; testes do backend em `backend/tests/`.

## Estilo

Manter Kotlin idiomático e o estilo atual do app: funções puras para parsing,
resolvedores implementando `PlatformResolver`, mensagens em português e validação
de resposta externa antes de criar `ResolvedMedia`.

## Estratégia de testes

- Testes unitários para hosts/vídeos Shorts válidos, falsos domínios e URLs
  inválidas; garantir que Instagram/Kwai não mudaram.
- Testes de API para entradas não YouTube recusadas e formatos separados/sem
  áudio excluídos.
- Executar testes unitários Android e backend, seguido do APK debug.
- Teste manual com URL pública autorizada e backend próprio; verificar prévia,
  download e ausência de regressão nos links existentes.

## Limites

- Sempre: validar URLs no app e no servidor; nunca registrar a chave; informar
  claramente o envio do link ao backend; baixar somente vídeos próprios ou com
  autorização.
- Ask first: novas dependências, cookies/autenticação de conta YouTube, mesclagem
  de faixas de áudio/vídeo e configuração de publicação na Play Store.
- Nunca: aceitar hosts arbitrários no endpoint de resolução, incluir chaves no
  repositório, ou tentar contornar controles de acesso/conteúdo privado.

## Perguntas em aberto

- Nenhuma. O backend será configurável nas opções e a chave será opcional para
  manter compatibilidade com a configuração atual do backend.
