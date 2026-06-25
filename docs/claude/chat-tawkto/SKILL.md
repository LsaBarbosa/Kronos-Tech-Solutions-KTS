# Skill — TAWK.to + Back-end Kronos

## Contexto

Feature de suporte por chat em produção usando **TAWK.to modo free** com integração completa no back-end da Kronos.

Branch de execução: `chat`.
Branch analisada como base: `homolog`.
Repositório de documentação de negócio: `LsaBarbosa/kronos-business`, branch `main`.

## Objetivo

Implementar uma integração robusta com TAWK.to, sem criar um chat próprio concorrente, usando o back-end para:

- fornecer configuração segura ao front-end;
- gerar identidade segura do visitante;
- relacionar chat com usuário, empresa, perfil e tela atual;
- receber webhooks do TAWK.to;
- validar assinatura dos webhooks;
- persistir eventos relevantes;
- criar trilha de auditoria;
- alimentar suporte com contexto e FAQ;
- expor métricas e logs operacionais;
- manter conformidade LGPD.

## Referências obrigatórias

Consultar antes de implementar:

- TAWK.to JavaScript API: `https://developer.tawk.to/jsapi/`
- TAWK.to Webhooks: `https://developer.tawk.to/webhooks/`

Pontos obrigatórios dessas referências:

- `Tawk_API.login(data, callback)` usa `userId` e `hash`.
- O `hash` do login é HMAC SHA256 de `userId` usando a Site API Key do TAWK.to.
- Webhooks são configurados no Admin do TAWK.to e possuem secret próprio.
- Webhooks enviam `X-Tawk-Signature`.
- A validação do webhook usa HMAC SHA1 sobre o corpo bruto da requisição.
- Webhooks podem ser reenviados por até 12 horas.
- `X-Hook-Event-Id` deve ser usado para idempotência.
- Eventos suportados: chat start, chat end, chat transcript created e ticket created.

## Arquivos que devem ser lidos primeiro

### Estrutura e dependências

- `build.gradle`
- `.env.example`
- `src/main/java/com/kts/kronos/KronosApplication.java`

### Rotas, segurança e padrões REST

- `src/main/java/com/kts/kronos/constants/ApiPaths.java`
- `src/main/java/com/kts/kronos/adapter/in/web/http/AuthController.java`
- `src/main/java/com/kts/kronos/adapter/in/web/http/UserController.java`
- `src/main/java/com/kts/kronos/adapter/in/web/http/CompanyController.java`
- `src/main/java/com/kts/kronos/adapter/in/web/exceptions/RestExceptionHandler.java`
- `src/main/java/com/kts/kronos/adapter/in/web/exceptions/ProblemDetail.java`

### FAQ consolidado

- `src/main/java/com/kts/kronos/adapter/in/web/http/FaqController.java`
- `src/main/java/com/kts/kronos/application/port/in/usecase/FaqUseCase.java`
- `src/main/java/com/kts/kronos/adapter/out/persistence/FaqArticleRepository.java`
- `src/main/java/com/kts/kronos/adapter/out/persistence/impl/FaqProviderImpl.java`

### Auditoria, LGPD e observabilidade

- `src/main/java/com/kts/kronos/adapter/out/persistence/AuditLogRepository.java`
- `src/main/java/com/kts/kronos/adapter/out/persistence/entity/AuditLogEntity.java`
- `src/main/java/com/kts/kronos/adapter/in/web/http/LgpdController.java`
- `src/main/java/com/kts/kronos/adapter/in/web/http/FrontendObservabilityController.java`
- `src/main/java/com/kts/kronos/adapter/in/web/http/SecurityIncidentController.java`

## Entregáveis back-end

### 1. Configuração

Criar propriedades fortemente tipadas para:

- habilitar/desabilitar chat por ambiente;
- `propertyId` público;
- `widgetId` público;
- Site API Key secreta para secure mode/login hash;
- webhook secret;
- origem pública permitida;
- retenção de eventos;
- limites de payload;
- flags de transcript e ticket sync;
- modo estrito de LGPD.

Atualizar `.env.example` sem inserir valores reais.

### 2. Endpoint de bootstrap do chat

Criar endpoint autenticado para o front-end obter:

- status da integração;
- `propertyId`;
- `widgetId`;
- `userId` canônico para TAWK.to;
- `hash` calculado no servidor;
- nome e e-mail autorizados para identificação;
- empresa ativa;
- papel/perfil;
- screen key sugerida;
- tags permitidas;
- atributos customizados permitidos;
- configuração de experiência desktop/mobile.

Não retornar Site API Key, webhook secret, CPF, tokens, dados biométricos, documentos ou dados sensíveis.

### 3. Identidade segura

Definir `userId` estável, não sensível e rastreável internamente.

Recomendação:

- usar UUID interno de usuário quando existir;
- não usar CPF;
- não usar e-mail como identificador primário;
- preservar separação por empresa ativa quando o usuário tiver multiempresa.

### 4. Webhook receiver

Criar endpoint público com validação forte:

- capturar corpo bruto da requisição;
- validar `X-Tawk-Signature` via HMAC SHA1;
- rejeitar payload sem assinatura;
- rejeitar payload acima do limite;
- aplicar idempotência por `X-Hook-Event-Id`;
- responder 2xx apenas após persistência mínima segura;
- não processar duas vezes o mesmo evento;
- não logar mensagem integral em nível INFO.

### 5. Persistência

Criar entidades/tabelas para registrar no mínimo:

- `hookEventId`;
- `eventType`;
- `chatId`;
- `ticketId` quando houver;
- `userId` interno quando mapeável;
- `companyId` ativo quando mapeável;
- `propertyId`;
- `domain`;
- `referrer` sanitizado;
- status de processamento;
- timestamp externo;
- timestamp de recebimento;
- hash do payload bruto;
- payload sanitizado/minimizado;
- motivo de rejeição quando aplicável.

Usar Flyway para migration.

### 6. Auditoria e LGPD

Registrar eventos de auditoria para:

- chat iniciado;
- chat encerrado;
- transcript recebido;
- ticket criado;
- falha de assinatura;
- payload rejeitado por tamanho/formato;
- tentativa duplicada;
- vínculo com usuário/empresa.

Regras LGPD:

- minimização de dados;
- retenção configurável;
- não persistir anexos do TAWK.to diretamente;
- não persistir documentos sensíveis no payload;
- mascarar e-mail quando aparecer em logs;
- nunca persistir CPF vindo do chat;
- permitir limpeza futura por política de retenção.

### 7. FAQ e suporte contextual

Aproveitar o FAQ existente:

- expor endpoint para o front pedir sugestões contextuais de suporte/chat;
- reutilizar screen keys já usadas pelo FAQ;
- sugerir artigos antes de abrir chat quando fizer sentido;
- registrar quando o usuário abriu chat após visualizar FAQ;
- permitir tags por módulo, exemplo: `dashboard`, `documents`, `time-records`, `employees`, `privacy`, `signature`.

### 8. Observabilidade

Adicionar:

- métricas por evento recebido;
- contador de assinatura inválida;
- contador de duplicidade por `X-Hook-Event-Id`;
- contador de chat/ticket por empresa;
- tempo de processamento de webhook;
- logs estruturados com correlation id;
- alertas/documentação para aumento de erro 4xx/5xx no webhook.

### 9. Testes

Criar testes para:

- cálculo de hash de login;
- validação de assinatura HMAC SHA1;
- rejeição de assinatura inválida;
- idempotência por hook event id;
- payload grande rejeitado;
- payload desconhecido aceito com status rastreável ou rejeitado conforme regra definida;
- endpoint autenticado não expõe secrets;
- FAQ contextual não aceita papel/perfil vindo do cliente.

## Fora do escopo

- Não contratar serviço pago.
- Não integrar WhatsApp API.
- Não criar chat próprio com WebSocket.
- Não enviar dados sensíveis ao TAWK.to.
- Não implementar IA neste ciclo, salvo preparação estrutural para futura evolução.

## Critério de pronto

A feature só estará pronta quando:

- back-end compila;
- testes passam;
- endpoints estão documentados;
- `.env.example` atualizado;
- secrets não aparecem em logs nem no front;
- webhook valida assinatura;
- eventos são idempotentes;
- front consegue carregar widget com identidade segura;
- FAQ contextual está conectado à experiência do chat;
- LGPD e retenção foram consideradas;
- há plano claro de configuração no painel TAWK.to.