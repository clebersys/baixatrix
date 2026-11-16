# Ficha da Play Store — BaixaTrix 2.0.0

Arquivos de mídia em `store/`:

| Arquivo | Uso na Play Console |
|---|---|
| `icone-512.png` | Ícone do app (512 × 512 px) |
| `grafico-destaque-1024x500.png` | Gráfico de destaque (1024 × 500 px) |
| `capturas/01-inicio.jpg` | Captura de celular 1 |
| `capturas/02-previa.jpg` | Captura de celular 2 |
| `capturas/03-biblioteca.jpg` | Captura de celular 3 |
| `capturas/04-divisao-30s.jpg` | Download e divisão em partes de 30s |
| `capturas/05-compartilhar-lote.jpg` | Compartilhamento das partes em lote |

Artefato de upload: `app/build/outputs/bundle/release/app-release.aab`
(ou `dist/BaixaTrix-2.0.0.aab` — o AAB é assinado com a mesma keystore do APK).

---

## Nome do app (máx. 30)

```
BaixaTrix — Instagram e Kwai
```

## Descrição curta (máx. 80)

```
Salve vídeos do Instagram e do Kwai com prévia antes de baixar.
```

## Descrição completa (máx. 4000)

```
BaixaTrix é o jeito mais direto de guardar aquele vídeo do Instagram ou do Kwai: cole o link, assista à prévia no player e só então baixe — em qualidade original e sem marca d'água adicional.

COMO FUNCIONA
1. Copie o link do vídeo no Instagram ou no Kwai.
2. Abra o BaixaTrix: o link é detectado automaticamente (ou toque em Colar).
3. Veja a prévia no player, confira autor, duração e qualidade.
4. Toque em Baixar vídeo. Pronto: o arquivo vai para Filmes/BaixaTrix.

DESTAQUES
• Prévia antes de baixar — nada é salvo sem você ver o vídeo.
• Download em segundo plano: continua mesmo se você fechar o app, com notificação de progresso e retomada automática.
• Baixar e dividir em partes de 30s sem reencodificar; compartilhe todas as partes de uma vez ou envie ao WhatsApp como documentos.
• Meus vídeos: biblioteca com miniaturas, tamanho e data, para abrir, compartilhar ou excluir.
• Recebe links de outros apps: no Instagram, toque em Compartilhar → BaixaTrix.
• Seletor de qualidade quando o site oferece mais de uma versão.
• Tema claro e escuro, Material 3, em português.

PRIVACIDADE
Nenhum cadastro, nenhum anúncio, nenhum rastreamento. Os links são resolvidos no próprio aparelho e os vídeos ficam só na sua memória. O cookie do Instagram, opcional, é guardado apenas no armazenamento local do app e pode ser apagado em Ajustes.

CONTEÚDO PRIVADO E LOGIN
O resolver nativo do BaixaTrix não oferece suporte a downloads de posts privados, e o app nunca captura automaticamente cookies ou sessões do Instagram. Para posts públicos que o Instagram temporariamente bloqueia sem login, o usuário pode inserir manualmente o sessionid da própria conta em Ajustes. Um backend próprio com cookies é um serviço externo e pode acessar posts privados que essa conta consiga ver; esse comportamento não é suporte nativo do BaixaTrix.

USO RESPONSÁVEL
Baixe apenas vídeos seus ou que você tenha autorização para usar. Respeite direitos autorais e os Termos de Uso do Instagram e do Kwai. O BaixaTrix não é afiliado a essas plataformas.

Requisitos: Android 8.0 ou superior.
```

## Categoria e tags

- Categoria: **Ferramentas** (Tools)
- Tags: download de vídeo, Instagram, Kwai, player
- E-mail de contato: clebersys@users.noreply.github.com
- Site: https://github.com/clebersys/baixatrix
- Política de privacidade: https://clebersys.github.io/baixatrix/privacidade.html

## Classificação de conteúdo (questionário)

- Categoria do app: Utilitário / Ferramenta
- Violência, sexo, linguagem, drogas, jogos de azar: **não** (o conteúdo é
  criado por terceiros; o app apenas baixa arquivos que o usuário escolhe)
- Compartilhamento de localização: não
- Permite compras digitais: não
- Resultado esperado: **Livre / 12 anos** conforme as respostas

## Segurança dos dados (Data safety)

- Coleta ou compartilha dados? **Não**
- Dados criptografados em trânsito: sim (HTTPS com Instagram/Kwai)
- Usuário pode solicitar exclusão: sim (apagar a pasta e desinstalar)
- Sem anúncios, sem SDKs de analytics
- Observação: se o usuário configurar um servidor de extração próprio, os
  links vão para esse servidor — informe isso no campo de observações.

## Público-alvo

- Faixa: **18 anos ou mais** (evita exigências de conteúdo infantil)
- O app não é projetado para crianças.

## Novidades desta versão (release notes)

```
Apresentamos BaixaTrix!
• nova marca e identidade visual em todo o aplicativo
• pasta de downloads Filmes/BaixaTrix
• botões de compartilhamento com rótulos completos em telas estreitas
```
