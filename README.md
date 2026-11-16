# BaixaTrix

Aplicativo Android para baixar vídeos do **Instagram** e do **Kwai** a partir de
um link. Cole o link, veja a prévia do vídeo no player e só então baixe — em
qualidade original, sem marca d'água adicional.

<p align="center">
  <img src="docs/tela-inicial.png" width="240" alt="Tela inicial" />
  <img src="docs/previa.png" width="240" alt="Prévia do vídeo" />
  <img src="docs/biblioteca.png" width="240" alt="Meus vídeos" />
</p>

<p align="center">
  <img src="docs/partes-30s.png" width="300" alt="Vídeo dividido em partes de 30 segundos" />
  <img src="docs/compartilhar-lote.png" width="300" alt="Compartilhamento das partes em lote" />
</p>

<p align="center">
  <a href="https://github.com/clebersys/baixatrix/releases/latest">Baixar o APK</a> ·
  <a href="docs/privacidade.html">Política de privacidade</a> ·
  <a href="store/publicacao-play.md">Publicação na Play Store</a>
</p>

## Funcionalidades

- **Colar link** com um toque (ou digitando) — aceita links de `instagram.com/p`,
  `/reel`, `/reels`, `/tv` e `kwai.com`, `k.kwai.com`, `kwai.app`.
- **Prévia antes de baixar** com player embutido (ExoPlayer), capa, autor,
  duração e seletor de qualidade quando o site oferece mais de uma.
- **Download em segundo plano** pelo DownloadManager: continua se você sair do
  app, aparece na notificação e é retomado se a conexão cair.
- **Baixar e dividir em partes de 30s**: corta o vídeo nos keyframes sem
  reencodificar, preservando a qualidade original. Depois, envia todas as partes
  juntas para outro app ou para o WhatsApp como documentos (sem recompressão).
- **Meus vídeos**: biblioteca dos arquivos salvos com miniatura, tamanho e data;
  abrir, compartilhar e excluir.
- **Compartilhar para o app**: recebe links enviados de outros aplicativos
  (Instagram → Compartilhar → BaixaTrix).
- **Sobre**: pasta de downloads, orientações de uso e versão do aplicativo.
- Tema claro/escuro, Material 3 e nada de anúncios ou rastreamento.

Os arquivos ficam em **`Filmes/BaixaTrix`** e aparecem na galeria.
As partes divididas ficam no armazenamento específico do app até serem
compartilhadas; mantenha o BaixaTrix aberto enquanto o processamento termina.

## Como a extração funciona

O app resolve tudo no próprio aparelho, sem depender de serviços de terceiros:

| Plataforma | Estratégia |
|---|---|
| **Kwai** | A página do vídeo entrega o MP4 no HTML (`<a-video-player src=…>`). O app segue o link curto (`k.kwai.com`), pega o vídeo principal (ignorando recomendações) e usa capa/título do Open Graph. |
| **Instagram** | Para posts públicos, o app tenta a página de *embed* (`/embed/captioned/`) e as tags Open Graph. O app não solicita, aceita nem lê cookies ou `sessionid` do Instagram. |

### Limitações reais (leia antes de usar)

- **Posts privados não são suportados pelo BaixaTrix.** O app não solicita,
  aceita nem importa cookies ou `sessionid` do Instagram.
- Alguns posts **públicos** podem não ser resolvidos se o Instagram exigir login.
- Instagram e Kwai mudam o layout com frequência. Os extratores foram testados
  em novembro de 2026; se pararem de funcionar, a atualização é pontual nos
  arquivos `data/resolver/*.kt`.

## Requisitos

- Android 8.0 (API 26) ou superior.
- Android SDK 36, JDK 17+ e Gradle 8.13 (o wrapper já cuida do Gradle).

## Instalando no aparelho

O APK assinado pronto para uso fica em `dist/`:

```bash
adb install -r dist/BaixaTrix-2.0.0.apk
sha256sum -c dist/BaixaTrix-2.0.0.apk.sha256   # confere a integridade
```

**BaixaTrix 2.0.0 usa um novo identificador Android**
(`com.cleber.baixatrix`). Ele é instalado como um app separado do antigo
BaixaVídeo; a pasta de download também mudou para `Filmes/BaixaTrix`.

Também é possível copiar o APK para o celular e abrir o arquivo (é preciso
autorizar "instalar apps desconhecidos" para o app de arquivos). Na primeira vez
que abrir **Meus vídeos**, o Android pede acesso a fotos e vídeos — sem essa
permissão a lista fica vazia, mas o download continua funcionando.

A versão mais recente também está em
[Releases](https://github.com/clebersys/baixatrix/releases/latest).

## Compilando

```bash
./gradlew assembleDebug          # APK de teste
./gradlew assembleRelease        # APK assinado (usa keystore.properties)
./gradlew bundleRelease          # AAB para a Play Store
./gradlew testDebugUnitTest      # testes de unidade
```

O APK de release sai em `app/build/outputs/apk/release/`.

### Assinatura

Copie `keystore.properties.example` para `keystore.properties` e aponte para a
sua chave:

```properties
storeFile=keystore/baixavideo-release.jks
storePassword=...
keyAlias=baixavideo
keyPassword=...
```

O arquivo `keystore.properties` e a pasta `keystore/` estão no `.gitignore`.
**Guarde a senha e o `.jks`**: sem eles não é possível publicar atualizações.

## Estrutura

```
app/src/main/java/com/cleber/baixatrix/
├── MainActivity.kt                 # entrada, edge-to-edge, link compartilhado
├── BaixaTrixApplication.kt        # Coil (miniaturas de vídeo)
├── data/
│   ├── MediaRepository.kt          # orquestra os extratores locais
│   ├── DownloadRepository.kt       # DownloadManager + progresso
│   ├── LibraryRepository.kt        # MediaStore (Meus vídeos)
│   ├── model/ResolvedMedia.kt      # mídia resolvida + variantes
│   └── resolver/                   # Instagram e Kwai
├── net/Http.kt                     # OkHttp compartilhado
├── ui/                             # Compose: Home, Biblioteca, Ajustes
└── util/                           # links, HTML/JSON, formatação, intents
backend/                            # utilitário independente de desenvolvimento, não integrado ao app
```

## Publicação

O pacote para a Play Store está pronto:

- **AAB assinado**: `dist/BaixaTrix-2.0.0.aab` (`./gradlew bundleRelease`)
- **Ficha pronta** (título, descrições, categoria, Data safety): [`store/ficha-play.md`](store/ficha-play.md)
- **Artes**: ícone 512, gráfico de destaque 1024×500 e capturas em [`store/`](store/)
- **Passo a passo do Play Console**: [`store/publicacao-play.md`](store/publicacao-play.md)

> A versão 2.0.0 usa o novo `applicationId` `com.cleber.baixatrix`. Se o antigo
> BaixaVídeo já tiver uma ficha no Play Console, esta versão precisa ser criada
> como um app/listing separado — não pode atualizar o pacote antigo.

A política de privacidade é publicada pelo GitHub Pages em
<https://clebersys.github.io/baixatrix/privacidade.html>.

## Privacidade e uso responsável

O app não tem configuração de servidor de extração e não envia seus links a
servidores do desenvolvedor: Instagram e Kwai são acessados diretamente do
aparelho. A pasta `backend/` é uma ferramenta independente para desenvolvedores
e não é usada pelo aplicativo.

Baixe apenas vídeos seus ou que você tenha autorização para usar. Respeite os
direitos autorais e os Termos de Uso das plataformas.
