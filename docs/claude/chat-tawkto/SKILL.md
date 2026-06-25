# Skill — TAWK.to + Back-end Kronos

Feature de suporte por chat em produção usando TAWK.to modo free.

Branch: `chat`.
Base: `homolog`.

## Objetivo

Implementar back-end para configuração, bootstrap, eventos externos, FAQ contextual, auditoria, métricas e testes.

## Leitura inicial

- build.gradle
- .env.example
- ApiPaths.java
- FaqController.java
- FaqUseCase.java
- FaqArticleRepository.java
- AuditLogRepository.java

## Entregas

- configuração por ambiente;
- bootstrap para front-end;
- integração com FAQ;
- registro de eventos;
- auditoria;
- métricas;
- testes.

## Validação

- ./gradlew test
- ./gradlew bootJar
