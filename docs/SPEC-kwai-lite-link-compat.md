# Spec: compatibilidade com links do Kwai Lite

## Objetivo

Permitir que o BaixaTrix analise links compartilhados pelo Kwai Lite. O app
deve reutilizar o extrator Kwai existente quando o link aponta para os domínios
e formatos já suportados, sem apresentar Kwai Lite como uma plataforma técnica
separada.

## Tech stack

- Kotlin, Android e Jetpack Compose.
- Resolução de link por `LinkParser` e `KwaiResolver`.
- Modelo de saída `ResolvedMedia`.

## Comandos

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

## Estrutura do projeto

- Parser: `app/src/main/java/com/cleber/baixatrix/util/LinkParser.kt`
- Resolver: `app/src/main/java/com/cleber/baixatrix/data/resolver/KwaiResolver.kt`
- Testes: `app/src/test/java/com/cleber/baixatrix/util/LinkParserTest.kt`

## Estilo de código

Seguir o padrão Kotlin existente: hosts aceitos ficam explícitos no parser;
links curtos são resolvidos por redirecionamento HTTP; o extrator retorna
`ResolvedMedia` e erros amigáveis, sem lógica de UI no resolver.

## Estratégia de testes

- Testar `https://kwai-video.com/p/0Cy4wPFd`, que em verificação HTTP redirecionou
  para uma página pública `kwai.com/@.../video/...`.
- Testar o texto compartilhado completo `Esse clipe me faz querer assistir
  novamente! https://kwai-video.com/p/0Cy4wPFd` e confirmar que o parser extrai
  somente a URL.
- Testar outros hosts e caminhos que o Kwai Lite compartilhe, se houver.
- Reutilizar testes de parser unitários para cada novo formato confirmado.
- Fazer teste manual com link público real do Kwai Lite para confirmar que
  prévia, download, divisão e compartilhamento continuam funcionando.

## Limites

- Sempre: tratar o link como Kwai se ele usar um formato público já servido pelo
  Kwai (`kwai-video.com/p/...`, `kwai.com`, `kwai.app`, `kwai.video` ou `k.kwai`).
- Ask first: adicionar um host novo que não possa ser confirmado como link de
  compartilhamento do Kwai Lite.
- Nunca: autenticar, capturar cookies ou tentar acessar vídeos privados.

## Critérios de sucesso

- Um link público compartilhado pelo Kwai Lite é reconhecido e resolvido.
- A frase introdutória do compartilhamento é ignorada pelo parser de URL.
- A plataforma continua identificada como Kwai na interface.
- Não há regressão nos links Kwai já suportados.

## Verificação ainda necessária

O redirecionamento do exemplo foi confirmado, mas ainda é necessário validar
que a página final entrega o arquivo de vídeo ao resolver Kwai.
