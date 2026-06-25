# Skill — TAWK.to + Back-end Kronos

Feature de suporte por chat em produção usando TAWK.to modo free.

Branch: `chat`.
Base: `homolog`.
Documentação: `kronos-business`, branch `main`.

## Objetivo

Implementar back-end para configuração, bootstrap, contexto do usuário, FAQ contextual, auditoria, métricas e testes.

## Leitura inicial

- `build.gradle`
- `.env.example`
- `src/main/java/com/kts/kronos/constants/ApiPaths.java`
- `src/main/java/com/kts/kronos/adapter/in/web/http/FaqController.java`
- `src/main/java/com/kts/kronos/application/port/in/usecase/FaqUseCase.java`
- `src/main/java/com/kts/kronos/adapter/out/persistence/FaqArticleRepository.java`
- `src/main/java/com/kts/kronos/adapter/out/persistence/AuditLogRepository.java`
- `src/main/java/com/kts/kronos/adapter/in/web/http/FrontendObservabilityController.java`
- `src/main/java/com/kts/kronos/adapter/in/web/http/LgpdController.java`

## Entregas

- configuração por ambiente;
- bootstrap para front-end;
- identificação do usuário logado com dados permitidos;
- registro operacional dos eventos do atendimento;
- integração com FAQ consolidado;
- auditoria;
- métricas;
- testes.

## Agents

- backend-chat-orchestrator: coordena a execução.
- repo-mapper: mapeia arquitetura existente.
- config-agent: configura ambientes e bootstrap.
- identity-agent: prepara identidade permitida do usuário.
- faq-agent: conecta FAQ contextual.
- audit-agent: registra auditoria e retenção.
- observability-agent: adiciona métricas e logs.
- test-agent: cria testes.
- review-agent: revisa entrega final.

## Tasks

1. Confirmar branch `chat`.
2. Mapear padrões do back-end.
3. Criar configuração por ambiente.
4. Criar contrato de bootstrap.
5. Integrar com FAQ.
6. Registrar eventos de suporte.
7. Criar auditoria e métricas.
8. Criar testes.
9. Validar build.

## Validação

- `./gradlew test`
- `./gradlew bootJar`

## Critério de pronto

- back-end compila;
- testes passam;
- `.env.example` atualizado;
- front recebe bootstrap;
- FAQ contextual conectado;
- auditoria e métricas criadas;
- documentação operacional atualizada.
