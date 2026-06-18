# Rules — Arquitetura Redis no Kronos

## RA-001 — PostgreSQL continua sendo fonte da verdade

Redis é infraestrutura auxiliar. O dado durável permanece em PostgreSQL/S3 conforme domínio atual.

Nunca mover para Redis como fonte primária:

- `tb_time_records`;
- `tb_afd_entry`;
- sequencial fiscal NSR;
- `tb_document`;
- `tb_audit_logs`;
- consentimentos legais;
- requisições LGPD;
- incidentes de segurança;
- documentos binários;
- imagens biométricas.

## RA-002 — Usar ports/adapters

Toda integração Redis deve respeitar a arquitetura hexagonal do projeto.

Modelo esperado:

```text
Service / UseCase
  ↓
Port de aplicação
  ↓
Redis adapter em adapter/out/redis ou infrastructure/redis
  ↓
StringRedisTemplate / RedisTemplate / CacheManager
```

Evitar:

- controller chamando Redis diretamente;
- service montando chave Redis manualmente;
- lógica de negócio em configuração Redis;
- dependência direta de Redis em domínio puro.

## RA-003 — Chaves padronizadas

Criar `RedisKeyFactory` central.

Formato obrigatório:

```text
kronos:{profile}:{module}:{purpose}:{identifierHash}
```

Exemplos:

```text
kronos:prod:auth:rate:login:ip:{hmacIp}
kronos:prod:auth:rate:login:user:{hmacUsername}
kronos:prod:auth:blacklist:jwt:{tokenSha256}
kronos:prod:auth:password-reset:{tokenSha256}
kronos:prod:bio:rate:checkin:{hmacEmployeeIp}
kronos:prod:records:lock:checkin:{employeeId}:{yyyyMMdd}
kronos:prod:cache:dashboard:{role}:{companyId}:{userId}
```

## RA-004 — Não usar PII em chave

Não escrever em chave Redis:

- CPF cru;
- e-mail cru;
- username cru;
- IP cru;
- token cru;
- nome do colaborador;
- conteúdo de documento;
- base64 de face.

Usar hash/HMAC com segredo:

```text
REDIS_KEY_HASH_SECRET
```

Fallback aceitável:

```text
LGPD_LOG_HASH_SECRET
```

## RA-005 — TTL obrigatório

Toda chave Redis criada pelo Kronos deve possuir TTL, exceto chaves estritamente técnicas e justificadas.

Tabela inicial:

| Chave | TTL sugerido |
|---|---:|
| rate limit login IP | janela configurada |
| rate limit login username | janela/cooldown configurado |
| rate limit recovery CPF/e-mail/IP | janela configurada |
| rate limit biometria | janela configurada |
| password reset token | 15 min ou configuração existente |
| JWT blacklist | diferença entre `expiresAt` e agora |
| checkin lock | 30-90 s |
| idempotência checkin | 60-180 s |
| dashboard summary | 30-60 s |
| own profile | 2-5 min |
| employee/user list | 30-120 s |
| geolocation resolve | 1-7 dias |
| public privacy/legal texts | 5-30 min |

## RA-006 — Serialização segura

Preferir:

- `StringRedisTemplate` para counters, locks, tokens, blacklist;
- `RedisTemplate<String, Object>` ou CacheManager com Jackson para cache de DTOs;
- DTOs pequenos e sem dados sensíveis desnecessários;
- `disableCachingNullValues()`.

Evitar:

- Java native serialization;
- entidades JPA serializadas;
- objetos lazy/proxy Hibernate;
- token cru;
- senha/hash de senha;
- documento binário;
- face/base64.

## RA-007 — Cache-aside com invalidação explícita

Fluxo padrão:

```text
GET:
  tenta Redis
  se miss, busca PostgreSQL/service
  grava DTO no Redis com TTL
  retorna DTO

WRITE:
  executa regra e persiste no PostgreSQL
  invalida chaves relacionadas
  retorna resposta original
```

## RA-008 — Falha do Redis por categoria

| Categoria | Modo de falha |
|---|---|
| Cache de consulta | fail-open: buscar no PostgreSQL e registrar métrica/log |
| Rate limit público | configurável; padrão prod pode ser fail-closed para endpoints sensíveis |
| JWT blacklist | fail-closed em produção |
| Password reset token | fail-closed |
| Checkin lock/idempotência | fail-closed para evitar duplicidade fiscal |
| Geolocation cache | fail-open: chama integração externa |

Adicionar propriedades:

```yaml
kronos:
  redis:
    enabled: true
    security-fail-closed: true
    cache-fail-open: true
```

## RA-009 — Observabilidade obrigatória

Adicionar métricas para:

- Redis hit/miss por cache;
- Redis error count por operação;
- lock acquired/lock denied;
- rate limit allowed/blocked;
- blacklist check success/failure;
- password reset token created/validated/deleted.

Logs devem ser estruturados e sem PII.

## RA-010 — Testes não devem exigir Redis em toda suíte

O profile `test` deve continuar rodando testes unitários sem Redis real.

Criar:

- fake/in-memory implementation para testes unitários;
- testes de integração Redis com Testcontainers `GenericContainer("redis:7.4-alpine")`;
- testes de fallback/cache quando Redis indisponível.
