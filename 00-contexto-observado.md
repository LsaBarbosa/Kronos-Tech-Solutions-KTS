# Contexto observado — Redis no Kronos

## Estado técnico relevante

- O back-end `prod-redis` usa Java 21, Spring Boot 3.5.3, Gradle, Spring Web MVC, Spring Security, JWT, CSRF, cookie HttpOnly, Spring Data JPA, Flyway, PostgreSQL em produção e H2 em testes.
- O `build.gradle` atual contém Actuator, Web, WebFlux, Validation, Security, Mail, Integration, JPA, Flyway, PostgreSQL, H2, Micrometer/Prometheus/OpenTelemetry, AWS S3/Rekognition e Testcontainers PostgreSQL.
- O `build.gradle` ainda precisa receber as dependências Redis: `spring-boot-starter-data-redis`, `spring-boot-starter-cache` e testes Redis com `GenericContainer`.
- O `application.yml` e `application-prod.yml` já possuem configurações de banco, segurança, cookies, CSRF, observabilidade e buckets, mas ainda precisam receber configuração Redis explícita.
- O front-end `PROD_HOSTINGER_v2` usa React/Vite, Axios, TanStack Query, Vitest e Playwright. Redis não deve aparecer no front-end; deve ser transparente por trás dos endpoints atuais.

## Pontos de código já identificados como alvos naturais

### `AuthenticationRateLimitService`

Atualmente usa estruturas em memória:

- `ConcurrentHashMap` para buckets de requisição;
- `ConcurrentHashMap` para falhas por username;
- `Deque<Instant>` para janelas móveis.

Problema em produção:

- os dados somem em restart;
- não existe coordenação entre réplicas futuras;
- cooldown por usuário fica limitado à instância atual.

Direção:

- substituir por porta `RateLimitStore` com implementação Redis;
- preservar comportamento público e mensagens atuais;
- sanitizar chaves com hash/HMAC para CPF, e-mail, username e IP.

### `BiometricProtectionService`

Atualmente usa bucket em memória para:

- `/auth/login-face`;
- `/records/checkin`;
- cadastro/atualização biométrica.

Direção:

- mover contadores para Redis;
- não armazenar imagem facial em Redis;
- usar Redis apenas para contagem/TTL/lock de curta duração.

### Blacklist de JWT

Existe entidade JPA `BlacklistedTokenEntity` em `tb_blacklisted_token` com `tokenHash` e `expiresAt`.

Direção:

- implementar `TokenBlacklistProvider` com Redis e TTL até expiração do JWT;
- manter contrato da porta;
- manter fallback JPA apenas se a configuração exigir, não como caminho principal em produção;
- nunca armazenar token cru, somente hash.

### Recuperação de senha

Fluxo atual gera token temporário, envia e-mail e remove após uso.

Direção:

- migrar token temporário para Redis com TTL;
- armazenar hash do token, não token cru;
- manter neutralidade de `/auth/recover-password` para não permitir enumeração.

### `POST /records/checkin`

Fluxo sensível e caro:

- NTP;
- biometria;
- geolocalização;
- busca de ponto aberto;
- NSR;
- AFD;
- comprovante PDF.

Direção:

- usar Redis lock/idempotência por colaborador e data para evitar duplo clique/concorrência;
- não mover `TimeRecord`, NSR, AFD ou comprovante para Redis;
- invalidar caches curtos de `today/recent/requests` após alteração de ponto.

## Endpoints priorizados

| Prioridade | Endpoint/fluxo | Uso Redis |
|---:|---|---|
| P0 | `POST /auth/login` | rate limit por IP/username e cooldown |
| P0 | `POST /auth/login-face` | rate limit biométrico público |
| P0 | `POST /auth/recover-password` | rate limit + token temporário com TTL |
| P0 | `POST /auth/reset-password` | validação/remoção do token Redis |
| P0 | `POST /auth/logout` | blacklist JWT Redis com TTL |
| P0 | `POST /auth/refresh` | consulta blacklist + blacklist do token antigo |
| P0 | `POST /records/checkin` | lock/idempotência curta por colaborador/data |
| P1 | `GET /records/me/today` | cache curto e invalidação no checkin/update |
| P1 | `GET /records/me/recent` | cache curto e invalidação no checkin/update |
| P1 | `GET /records/me/requests` | cache curto e invalidação em solicitações/aprovações |
| P1 | `GET /dashboard/summary` | cache curto por usuário/papel/empresa |
| P1 | `GET /users/own-profile` | cache-aside com invalidação em update/toggle/password |
| P1 | `GET /employee/own-profile` | cache-aside com invalidação em update/toggle |
| P1 | `GET /employee?active=` | cache por tenant/filtro para MANAGER |
| P1 | `GET /users/search?active=` | cache por tenant/filtro, sem vazar dados cross-tenant |
| P1 | `GET /companies/{cnpj}` | cache para CTO, chave com hash de CNPJ |
| P1 | `POST /geolocation/resolve` | cache de resposta HERE/ViaCEP por hash de endereço |
| P2 | `GET /public/privacy/*` | cache institucional curto/médio |
| P2 | `GET /legal/espelho-ponto` | avaliar apenas metadados/cache controlado; não cachear PDF legal sem análise |

## Não escopar nesta implementação

- Redis no front-end.
- Redis como fila de eventos fiscais.
- Redis como substituto de PostgreSQL.
- Redis para armazenar imagem facial/base64.
- Redis para AFD/AEJ/NSR/documentos legais como dado primário.
- Expor `6379` publicamente na internet.
