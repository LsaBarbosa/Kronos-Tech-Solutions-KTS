# Agent — Backend Redis Agent

## Responsabilidade

Implementar Redis no back-end Java/Spring Boot, mantendo arquitetura hexagonal.

## Tarefas

1. Adicionar dependências Redis em `build.gradle`.
2. Criar configuração Redis tipada.
3. Criar `RedisKeyFactory` e `RedisKeyHasher`.
4. Criar ports para rate limit, cache, lock e idempotência quando necessário.
5. Implementar adapters Redis.
6. Refatorar `AuthenticationRateLimitService`.
7. Refatorar `BiometricProtectionService`.
8. Implementar `TokenBlacklistProvider` Redis.
9. Implementar `PasswordResetTokenProvider` Redis.
10. Aplicar lock/idempotência em `TimeRecordService` no checkin.
11. Aplicar cache-aside em endpoints P1.
12. Instrumentar métricas e logs.
13. Criar testes unitários e integração.

## Arquivos prováveis

```text
src/main/java/com/kts/kronos/infrastructure/redis/*
src/main/java/com/kts/kronos/application/port/out/provider/*
src/main/java/com/kts/kronos/application/security/AuthenticationRateLimitService.java
src/main/java/com/kts/kronos/application/security/BiometricProtectionService.java
src/main/java/com/kts/kronos/application/service/AuthService.java
src/main/java/com/kts/kronos/application/service/TimeRecordService.java
src/main/java/com/kts/kronos/application/service/DashboardService.java
src/main/resources/application.yml
src/main/resources/application-prod.yml
src/test/java/com/kts/kronos/**
```

## Cuidados

- Preservar mensagens de erro atuais.
- Preservar exceções esperadas pelo `RestExceptionHandler`.
- Usar TTL em toda escrita.
- Não serializar JPA Entity.
- Não cachear resposta cross-tenant sem company/user no escopo da chave.
