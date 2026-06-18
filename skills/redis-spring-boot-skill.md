# Skill — Implementar Redis com Spring Boot no Kronos

## Quando usar

Use esta skill para implementar Redis no back-end `Kronos-Tech-Solutions-KTS`, branch `prod-redis`, respeitando a arquitetura hexagonal do projeto.

## Objetivo técnico

Adicionar Redis para:

- cache-aside;
- rate limit distribuído;
- tokens temporários;
- blacklist JWT;
- locks/idempotência;
- health/observabilidade.

## Dependências Gradle

Adicionar em `dependencies`:

```gradle
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
implementation 'org.springframework.boot:spring-boot-starter-cache'
testImplementation 'org.testcontainers:junit-jupiter'
```

Usar `GenericContainer` para Redis em testes de integração.

## Configuração base sugerida

Criar `RedisProperties` tipado com `@ConfigurationProperties`:

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: ${REDIS_DATABASE:0}
      timeout: ${REDIS_TIMEOUT:2s}
  cache:
    type: redis
    redis:
      time-to-live: ${REDIS_CACHE_DEFAULT_TTL:5m}
      cache-null-values: false

kronos:
  redis:
    enabled: ${REDIS_ENABLED:true}
    key-prefix: ${REDIS_KEY_PREFIX:kronos}
    key-hash-secret: ${REDIS_KEY_HASH_SECRET:${LGPD_LOG_HASH_SECRET:local-dev-redis-key-secret}}
    security-fail-closed: ${REDIS_SECURITY_FAIL_CLOSED:true}
    cache-fail-open: ${REDIS_CACHE_FAIL_OPEN:true}
    lock:
      checkin-ttl-seconds: ${REDIS_CHECKIN_LOCK_TTL_SECONDS:60}
    ttl:
      password-reset-minutes: ${REDIS_PASSWORD_RESET_TTL_MINUTES:15}
      dashboard-summary-seconds: ${REDIS_DASHBOARD_SUMMARY_TTL_SECONDS:60}
      own-profile-seconds: ${REDIS_OWN_PROFILE_TTL_SECONDS:300}
      records-today-seconds: ${REDIS_RECORDS_TODAY_TTL_SECONDS:30}
      records-recent-seconds: ${REDIS_RECORDS_RECENT_TTL_SECONDS:60}
      geolocation-days: ${REDIS_GEOLOCATION_TTL_DAYS:7}
```

## Classes/portas sugeridas

```text
application/port/out/provider/
  RateLimitStore.java
  CacheProvider.java
  DistributedLockProvider.java
  IdempotencyProvider.java

infrastructure/redis/
  RedisConfig.java
  RedisProperties.java
  RedisKeyFactory.java
  RedisKeyHasher.java
  RedisRateLimitStore.java
  RedisCacheProvider.java
  RedisDistributedLockProvider.java
  RedisTokenBlacklistProvider.java
  RedisPasswordResetTokenProvider.java
```

Se o projeto já usa outro pacote para infraestrutura, seguir o padrão existente.

## Rate limit

Implementar contador por janela com Redis.

Estratégia simples:

- chave por identificador HMAC;
- `INCR`;
- `EXPIRE` na primeira criação;
- bloquear quando contador exceder limite;
- para cooldown de username, usar chave separada `blockedUntil` com TTL.

Pseudo:

```java
long count = redis.opsForValue().increment(key);
if (count == 1) redis.expire(key, window);
if (count > limit) throw TooManyRequestsException;
```

Para cooldown:

```text
kronos:prod:auth:rate:login:user:block:{hmacUsername}
TTL = cooldown calculado
```

## Blacklist JWT

Implementar provider Redis mantendo a mesma porta.

Fluxo:

```text
addToBlacklist(rawToken, expiration):
  tokenHash = sha256(rawToken)
  ttl = expiration - now
  SET key "1" EX ttl

isBlacklisted(rawToken):
  EXISTS key
```

Não armazenar token cru.

## Password reset token

Fluxo:

```text
generateAndSaveToken(userId):
  token = random secure
  SET password-reset:{sha256(token)} userId EX ttl
  return token

validateToken(token):
  GET password-reset:{sha256(token)}

deleteToken(token):
  DEL password-reset:{sha256(token)}
```

## Distributed lock para checkin

Usar `SET key value NX PX ttl`.

Valor:

```text
owner = UUID.randomUUID().toString()
```

Release com Lua:

```lua
if redis.call('get', KEYS[1]) == ARGV[1] then
  return redis.call('del', KEYS[1])
else
  return 0
end
```

Não liberar lock que não pertence à execução atual.

## Cache-aside

Aplicar em consultas idempotentes e seguras:

- dashboard summary;
- own profile;
- employee/user lists por tenant;
- status do ponto do dia;
- registros recentes;
- geolocation resolve;
- textos públicos.

Invalidação:

- alterações de usuário invalidam caches de usuário/dashboard;
- alterações de colaborador invalidam employee/profile/dashboard;
- checkin/update/approve/reject invalidam records today/recent/requests/dashboard;
- atualização de termos invalidaria public privacy/legal text.

## Health

Adicionar health indicator Redis se não vier automaticamente.

O health administrativo deve mostrar status agregado sem expor senha, host sensível ou dados internos.

## Observabilidade

Instrumentar:

- cache hit/miss;
- lock acquired/denied/error;
- rate limit allowed/blocked;
- blacklist added/check;
- password reset token lifecycle;
- Redis unavailable.

Evitar labels com PII.
