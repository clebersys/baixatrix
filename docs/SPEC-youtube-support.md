# Spec: extração local de vídeos do YouTube

## Objetivo

Permitir colar ou compartilhar no BaixaTrix um link de vídeo público do YouTube e
ver a prévia/baixar sem configurar backend, URL de servidor ou chave. A extração
será executada no próprio Android com NewPipe Extractor.

Critérios de aceite:

- `youtube.com`, `m.youtube.com`, `music.youtube.com` e `youtu.be` válidos são
  reconhecidos pelo app; domínios parecidos, links HTTP, playlists isoladas e IDs
  malformados são recusados.
- O app resolve localmente vídeos públicos sem configuração de servidor e sem
  cookies/sessão de conta.
- A prévia usa streams progressivos. O download usa MP4 com áudio embutido ou
  combina localmente H.264/HEVC + AAC sem reencodificar; outros formatos são recusados.
- URLs de mídia e miniaturas da resposta do extractor são validadas antes do uso.
- Instagram e Kwai continuam resolvidos localmente sem regressões.
- A interface não pede configuração de backend para YouTube e informa que o
  processamento acontece no aparelho.
- A próxima versão será 2.4.0 (versionCode 10), com APK assinado e checksum em
  uma GitHub Release; não publicar na Play Store.

## Stack e contrato

- Android: Kotlin, Jetpack Compose, OkHttp, JUnit 4 já usados pelo projeto.
- Extração: NewPipe Extractor 0.27.1 via JitPack, com downloader OkHttp local.
- Mux: `MediaExtractor`/`MediaMuxer` do Android para combinar H.264 + AAC sem
  reencodificar quando não houver stream MP4 com áudio embutido.
- Compatibilidade: minSdk 26 mantida com `desugar_jdk_libs_nio` conforme o README
  oficial do extractor.
- Somente metadados e formatos públicos são solicitados. Não adicionar cookies,
  login ou acesso a vídeos privados.
- A licença do projeto passa de MIT para GPL-3.0-or-later para compatibilidade
  com NewPipe Extractor; o autor atual do projeto autorizou essa mudança.

## Comandos

```bash
./gradlew testDebugUnitTest
./gradlew assembleRelease
cd backend && python3 -m unittest discover -s tests
```

## Estrutura

- `app/src/main/java/com/cleber/baixatrix/data/resolver/YoutubeResolver.kt`:
  extração local e seleção de formatos.
- `app/src/main/java/com/cleber/baixatrix/data/processing/YoutubeStreamMuxer.kt`:
  mux local de faixas remotas sem reencodificar.
- `app/src/main/java/com/cleber/baixatrix/data/resolver/NewPipeDownloader.kt`:
  implementação OkHttp do contrato Downloader do extractor.
- `app/src/main/java/com/cleber/baixatrix/BaixaTrixApplication.kt`: inicialização
  única do NewPipe Extractor.
- Testes Android em `app/src/test/`; backend permanece como ferramenta opcional
  independente, não usado pelo app.

## Estilo

Manter os resolvedores implementando `PlatformResolver`, usar corrotinas para
isolar a extração de rede em IO, traduzir falhas externas para `ResolveException`
e não registrar links ou dados externos em logs.

## Estratégia de testes

- Testes unitários para links válidos/inválidos e seleção de MP4 progressivo ou
  pares H.264/HEVC + AAC com URLs válidas.
- Testes de segurança para recusar URLs de mídia fora de HTTPS/`googlevideo.com`
  e miniaturas fora de `ytimg.com`.
- Executar todos os testes Android e montar APK release assinado.
- Verificar assinatura, versão e continuidade da chave em relação à 2.3.0.
- Tentar smoke test manual em aparelho com vídeo público autorizado. O teste
  automatizado no host atual recebeu `SignInConfirmNotBotException` do YouTube
  para o IP do ambiente; isso não prova o comportamento em rede residencial.

## Limites

- Sempre: extração no aparelho, validação dos URLs retornados, uso sem cookies,
  mensagens claras quando não houver formato compatível e download apenas de
  conteúdo próprio ou autorizado.
- Ask first: reencodificação de áudio/vídeo, cookies/login, suporte a
  conteúdo privado, publicação na Play Store ou mudança de licença futura.
- Nunca: pedir backend para o fluxo YouTube, incluir credenciais, ou contornar
  controles de acesso/DRM.

## Fontes oficiais

- [README do NewPipe Extractor](https://github.com/TeamNewPipe/NewPipeExtractor/blob/v0.27.1/README.md)
- [Release 0.27.1 e notas de compatibilidade](https://github.com/TeamNewPipe/NewPipeExtractor/releases/tag/v0.27.1)
- [Android: API desugaring para `java.nio`](https://android-developers.googleblog.com/2023/02/api-desugaring-supporting-android-13-and-java-nio.html)

## Perguntas em aberto

- Nenhuma. O usuário autorizou extração local, relicenciamento GPL-3.0-or-later e
  publicação da nova versão no GitHub.
