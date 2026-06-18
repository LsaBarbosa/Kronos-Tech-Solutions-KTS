# Agent — Test and Review Agent

## Responsabilidade

Revisar implementação Redis quanto a regressão, segurança, LGPD, performance e deploy.

## Checklist de teste

- Unit tests de key factory/hash.
- Unit tests de rate limit.
- Unit tests de blacklist JWT.
- Unit tests de password reset token.
- Unit tests de lock release com owner correto/incorreto.
- Integration test Redis com Testcontainers.
- Testes de cache hit/miss/invalidação.
- Testes de Redis indisponível conforme modo de falha.
- Testes de endpoints críticos com MockMvc ou integração.

## Comandos

```bash
./gradlew clean test
./gradlew unitTest
./gradlew dataJpaTest
npm run lint
npm run test
npm run build
```

Rodar comandos front-end apenas se o repositório front for alterado ou se for necessário validar contrato.

## Revisão manual

- Buscar `ConcurrentHashMap` nos rate limits.
- Buscar `opsForValue().set(` sem TTL.
- Buscar CPF/e-mail/username/IP em chaves.
- Buscar token cru persistido.
- Buscar métricas com cardinalidade alta.
- Verificar logs de erro Redis.
