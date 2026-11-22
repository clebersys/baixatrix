# Plano: suporte a vídeos do YouTube

## Visão geral

Integrar o app Android ao backend próprio FastAPI/`yt-dlp` existente para resolver
links de vídeos e Shorts do YouTube, exibir a prévia e reutilizar o fluxo de
download atual. Os links serão enviados somente ao endereço configurado pelo
usuário; Instagram e Kwai permanecem locais.

## Decisões de arquitetura

- Manter o contrato `GET /resolve?url=...` e validar host/protocolo no backend
  antes de chamar `yt-dlp`, evitando que o endpoint aceite URLs arbitrárias.
- Reutilizar formatos progressivos MP4 para não introduzir mesclagem nem novas
  dependências no app.
- Guardar URL do backend e chave opcional nas preferências privadas do app; não
  incluir chaves ou endereço pessoal nos arquivos versionados.
- Exigir endpoint HTTPS no app para não expor a chave ou os links na rede; o
  backend pessoal precisará ser acessível por HTTPS (por exemplo, via proxy
  reverso ou túnel privado).
- Não enviar cookies de conta YouTube. Manter o uso opcional de cookies somente
  para fluxos existentes do backend que não sejam YouTube.

## Tarefas

### Fase 1: contrato e validação do backend
- [x] Tarefa 1: restringir URLs no backend e testar formatos progressivos.

### Checkpoint 1
- [x] Testes do backend passam; links fora dos hosts permitidos são rejeitados antes
  de iniciar extração.

### Fase 2: reconhecimento e resolução Android
- [x] Tarefa 2: adicionar plataforma YouTube e resolver a resposta do backend com
  validação defensiva.
- [x] Tarefa 3: integrar configuração persistente do backend à tela de configurações e ao
  fluxo de resolução, incluindo aviso de envio do link.

### Checkpoint 2
- [x] Testes unitários Android passam; build debug conclui; erro de configuração
  ausente é claro e Instagram/Kwai seguem funcionando.

### Fase 3: documentação e verificação
- [x] Tarefa 4: documentar configuração HTTPS, chaves, limitações e uso responsável;
  executar a suíte completa e revisar as alterações.

### Checkpoint final
- [x] APK debug compila; testes Android e backend passam; revisar diff e confirmar
  que nenhum segredo foi adicionado.

## Riscos e mitigação

| Risco | Impacto | Mitigação |
|---|---|---|
| O YouTube deixar de oferecer formatos MP4 progressivos | Algumas resoluções podem não aparecer | Retornar apenas formatos com áudio/vídeo juntos e explicar a limitação; não implementar mesclagem sem nova aprovação |
| URLs diretas expirarem | Prévia funciona, download pode falhar depois | Tratar falha com orientação para resolver o link novamente |
| Backend HTTP expor a chave/link | Interceptação na rede | Validar HTTPS no app e documentar proxy/túnel HTTPS |
| Resposta externa malformada | Crash ou URL de mídia inesperada | Validar esquema HTTPS e campos obrigatórios antes de criar a mídia |

## Perguntas em aberto

- Nenhuma. O usuário aprovou o escopo pessoal fora da Play Store e a especificação.
