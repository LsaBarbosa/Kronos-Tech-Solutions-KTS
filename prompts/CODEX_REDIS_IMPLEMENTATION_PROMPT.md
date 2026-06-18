# Prompt principal para CODEX — Implementar Redis no Kronos

Você é o agente de execução, implementação e revisão da tarefa Redis do projeto Kronos.

## Contexto obrigatório

Repositórios e branches:

- Back-end: `LsaBarbosa/Kronos-Tech-Solutions-KTS`, branch `prod-redis`.
- Front-end: `LsaBarbosa/Kronos-Tech-Solution-User-Plataform`, branch `PROD_HOSTINGER_v2`.
- Documentação: `LsaBarbosa/kronos-business`, branch `main`.

Objetivo:

- Implementar Redis nos endpoints específicos do Kronos.
- Redis rodará localmente dentro da VPS Hostinger em produção.
- Redis não deve ser exposto publicamente.
- Redis não substitui PostgreSQL.
- Redis deve ser transparente para o front-end.

## Antes de implementar

Leia obrigatoriamente os arquivos deste pacote:

1. `README.md`
2. `00-contexto-observado.md`
3. `rules/redis-architecture-rules.md`
4. `rules/security-lgpd-observability-rules.md`
5. `skills/redis-spring-boot-skill.md`
6. `skills/hostinger-redis-prod-skill.md`
7. `plan/redis-action-plan.md`
8. `checklists/review-checklist.md`

Depois, leia no back-end:

1. `build.gradle`
2. `src/main/resources/application.yml`
3. `src/main/resources/application-prod.yml`
4. `src/main/resources/application-test.yml`, se existir
5. `docker-compose.yml`, `Dockerfile`, `.env.example`, `deploy/hostinger-nginx.conf`, se existirem
6. `src/main/java/com/kts/kronos/application/security/AuthenticationRateLimitService.java`
7. `src/main/java/com/kts/kronos/application/security/BiometricProtectionService.java`
8. `src/main/java/com/kts/kronos/application/service/AuthService.java`
9. `src/main/java/com/kts/kronos/adapter/out/security/JwtAuthenticationFilter.java`
10. `src/main/java/com/kts/kronos/adapter/out/security/AuthCookieService.java`
11. `src/main/java/com/kts/kronos/adapter/out/security/JwtUtils.java`
12. Todas as portas `TokenBlacklistProvider`, `PasswordResetTokenProvider`, `UserProvider`, `EmployeeProvider`, `CompanyProvider`, `TimeRecordProvider`
13. Implementações JPA atuais dessas portas
14. `TimeRecordService`, `DashboardService`, `UserService`, `EmployeeService`, `CompanyService`, `PublicPrivacyService`, `GeolocationService`, `AcceptTermsService`
15. `KronosMetrics`, `KronosTracing`, `PlatformHealthService`
16. Testes existentes relacionados a auth, biometria, ponto, dashboard, usuários, colaboradores e LGPD

Leia no front-end:

1. `package.json`
2. API client/Axios
3. Configuração TanStack Query
4. Chamadas para `/auth/*`, `/records/*`, `/dashboard/summary`, `/users/own-profile`, `/employee/own-profile`
5. OpenAPI/tipos gerados, se existirem

Leia na documentação:

1. Documento de arquitetura de pastas/projeto
2. Documento de fluxos de aplicação
3. Documento de regras de negócio
4. Documento de entidades
5. Documento de entradas/saídas
6. Documento mais recente de estado atual de produção, se existir

## Implementação esperada

### 1. Fundação Redis

- Adicione dependências:
  - `org.springframework.boot:spring-boot-starter-data-redis`
  - `org.springframework.boot:spring-boot-starter-cache`
- Crie configuração Redis tipada.
- Crie `RedisKeyFactory`.
- Crie `RedisKeyHasher` com HMAC-SHA256.
- Configure serializers seguros.
- Configure TTL default.
- Configure propriedades por env.
- Garanta que profile `test` não dependa de Redis real para todos os testes.

### 2. Rate limit com Redis

Refatore:

- `AuthenticationRateLimitService`
- `BiometricProtectionService`

Endpoints afetados:

- `POST /auth/login`
- `POST /auth/login-face`
- `POST /auth/recover-password`
- `GET /employee/check-cpf?cpf=`
- `GET /users/check-username?username=`
- `GET /companies/check-cnpj?cnpj=`
- `POST /records/checkin`
- `POST /employee/manager/{employeeId}/biometric-enrollment`

Regras:

- Não usar `ConcurrentHashMap` para rate limit em produção.
- Usar Redis `INCR` + `EXPIRE`.
- Usar cooldown por username com TTL.
- HMAC para CPF/e-mail/username/IP.
- Preservar exceções e mensagens atuais.

### 3. Blacklist JWT com Redis

Refatore provider de blacklist mantendo porta atual.

Regras:

- `key = blacklist:{sha256(rawToken)}`.
- TTL = expiração original do JWT.
- Não armazenar token cru.
- `POST /auth/logout` adiciona token à blacklist.
- `POST /auth/refresh` rejeita token em blacklist e adiciona token antigo após refresh.
- `JwtAuthenticationFilter`, se checa blacklist, deve usar o provider.

### 4. Password reset token com Redis

Refatore provider de token de recuperação mantendo porta atual.

Regras:

- Gerar token seguro.
- Salvar hash do token com userId e TTL.
- Validar por hash.
- Remover após uso.
- Não permitir enumeração em `/auth/recover-password`.

### 5. Lock/idempotência do checkin

Implementar provider de lock Redis.

Aplicar em `POST /records/checkin`.

Regras:

- Lock por `employeeId + data`.
- `SET NX PX`.
- Owner token único.
- Release com Lua validando owner.
- TTL curto.
- Não mover `TimeRecord`, NSR, AFD, PDF ou documento para Redis.
- Invalidar caches de ponto e dashboard após sucesso.

### 6. Cache dos endpoints específicos

Implementar cache-aside nos endpoints:

- `GET /dashboard/summary`
- `GET /records/me/today`
- `GET /records/me/recent`
- `GET /records/me/requests`
- `GET /employee/own-profile`
- `GET /users/own-profile`
- `GET /employee?active=`
- `GET /users/search?active=`
- `GET /companies/{cnpj}`
- `POST /geolocation/resolve`
- `GET /public/privacy/processing-catalog`
- `GET /public/privacy/policy`
- `GET /public/privacy/biometric-term`

Regras:

- Cachear DTO, não Entity JPA.
- Chave com escopo correto: usuário, role, tenant, filtros.
- TTL curto/médio.
- Invalidação explícita em escritas.
- Fail-open para cache.
- Não vazar dados entre tenants.

### 7. Deploy Hostinger

Atualize deploy/configuração:

- Redis local na VPS.
- Sem exposição pública da porta 6379.
- Senha via env.
- Healthcheck.
- Volume persistente se usar AOF.
- `.env.example` com variáveis sem segredo real.
- Documentar rollback.

### 8. Observabilidade

Adicionar métricas/logs para:

- Redis hit/miss;
- rate limit allowed/blocked;
- blacklist added/check;
- reset token created/validated/deleted;
- lock acquired/denied/released;
- Redis unavailable;
- cache invalidated.

Não usar labels com PII ou IDs de alta cardinalidade.

## Validação obrigatória

Rode:

```bash
./gradlew clean test
./gradlew unitTest
./gradlew dataJpaTest
```

Se o front-end for alterado ou se o contrato for validado:

```bash
npm run lint
npm run test
npm run build
```

Testes mínimos:

- key factory não vaza PII;
- rate limit bloqueia após limite e expira;
- cooldown username funciona;
- token blacklist expira com TTL;
- reset token é uso único;
- checkin lock impede concorrência;
- cache hit/miss/invalidação;
- Redis indisponível segue configuração.

## Proibições

Não faça:

- Redis no front-end;
- Redis como banco principal;
- Redis para imagem facial;
- Redis para documento/PDF;
- Redis para AFD/AEJ/NSR como fonte de verdade;
- token cru em Redis;
- CPF/e-mail/username/IP cru em chave;
- logs com PII;
- alteração de contrato HTTP sem justificativa;
- exposição pública da porta 6379;
- `@EnableCaching` na classe main se isso tornar cache obrigatório para todos os testes.

## Relatório final esperado

Ao terminar, responda com:

1. Arquivos alterados.
2. Endpoints impactados.
3. Chaves Redis criadas e TTLs.
4. Variáveis de ambiente novas.
5. Comandos executados e resultados.
6. Testes adicionados.
7. Riscos remanescentes.
8. Instruções de deploy Hostinger.
9. Instruções de rollback.
