# Publicando na Play Store

O código, o AAB assinado e todos os textos/artes já estão prontos neste
repositório. O que falta depende da sua conta de desenvolvedor — o Play Console
não tem API pública de publicação sem um projeto no Google Cloud configurado,
então esses passos são manuais.

## 1. Conta de desenvolvedor

1. Acesse <https://play.google.com/console> com a conta Google que será dona do
   app.
2. Pague a taxa única de **US$ 25** e conclua a verificação de identidade
   (documento + endereço). Para conta pessoal, o Google também exige teste
   fechado com pelo menos **12 testadores por 14 dias** antes de liberar para
   produção em contas criadas depois de nov/2023.

## 2. Criar o app

- Nome: `BaixaVídeo — Instagram e Kwai`
- Idioma padrão: Português (Brasil)
- Tipo: Aplicativo · Gratuito
- Declarações: app não é para crianças; contém anúncios = **não**

## 3. Enviar o AAB

1. Gere/atualize o pacote: `./gradlew bundleRelease`
2. Copie `app/build/outputs/bundle/release/app-release.aab` (ou use
   `dist/BaixaVideo-1.2.1.aab`).
3. Play Console → **Testar e lançar → Teste interno → Criar nova versão** →
   envie o `.aab`.
4. Ative o **Play App Signing** (recomendado). Guarde a keystore atual: ela
   assina o upload; o Google reassina a distribuição.

## 4. Preencher a ficha

Use `store/ficha-play.md` como fonte: nome, descrições, categoria, e-mail,
política de privacidade, classificação de conteúdo, segurança dos dados e
público-alvo.

Artes prontas em `store/`:

- `icone-512.png` → ícone do app
- `grafico-destaque-1024x500.png` → gráfico de destaque
- `capturas/01-inicio.jpg` a `05-compartilhar-lote.jpg` → capturas de celular
  (1080 × 2160), incluindo divisão em 30s e compartilhamento em lote

## 5. Política de privacidade hospedada

A página está em `docs/privacidade.html` e é publicada pelo GitHub Pages:

1. No repositório: **Settings → Pages → Source: Deploy from a branch →
   Branch `main` / pasta `/docs`** (ou rode o comando abaixo).
2. URL final: <https://clebersys.github.io/baixavideo/privacidade.html>

```bash
gh api -X POST repos/clebersys/baixavideo/pages \
  -f 'source[branch]=main' -f 'source[path]=/docs'
```

## 6. Antes de mandar para revisão

- [ ] Teste interno instalado em pelo menos 2 aparelhos reais
- [ ] Fluxo completo: colar link, prévia, download, biblioteca, compartilhar
- [ ] Confirmar que o AAB sobe sem erro de assinatura (`jarsigner -verify`)
- [ ] Preencher o questionário de classificação e o Data safety
- [ ] Revisar a política de privacidade publicada (link abre sem login)

## 7. Revisão e riscos de política — leia com atenção

O Google costuma **rejeitar** apps que baixam conteúdo de redes sociais com
base em:

- **Direitos autorais / propriedade intelectual** — baixar vídeos de terceiros
  pode ser visto como facilitar violação.
- **Termos de uso da plataforma** — Instagram e Kwai proíbem download por
  meios não oficiais.
- **Conteúdo gerado por usuário sem moderação**.

Isso não impede o envio, mas aumenta a chance de rejeição. Se for rejeitado,
as saídas usuais são: reposicionar o app como ferramenta para **baixar apenas
conteúdo próprio** do criador (ex.: "salve seus próprios Reels"), incluir
aviso claro de uso responsável (já existe na ficha e no README) e remover
menções a "sem marca d'água".

Alternativas de distribuição que não dependem da Play Store:

- APK direto pelas **Releases do GitHub** (`gh release create`), que já está
  pronto em `dist/`;
- **F-Droid** (exige build reprodutível e código 100% livre — o projeto é
  MIT, então é viável).

## Publicando releases no GitHub (já pronto)

```bash
cd /home/cleber/Documentos/BaixaVideo
gh release create v1.2.1 dist/BaixaVideo-1.2.1.apk dist/BaixaVideo-1.2.1.apk.sha256 \
  --title "BaixaVídeo 1.2.1" \
  --notes "Correção visual dos botões de compartilhamento: rótulos completos em telas estreitas. Inclui APK/AAB assinados, checksums e capturas atualizadas."
```
