# Rules — Segurança, LGPD e observabilidade

## RSO-001 — Redis não pode vazar dados pessoais

Não armazenar PII crua em:

- chave;
- valor;
- log;
- métrica label;
- exception message.

Dados proibidos em Redis, salvo justificativa formal e TTL curto:

- CPF;
- e-mail;
- username;
- IP;
- user-agent completo;
- token JWT cru;
- token de reset cru;
- imagem facial;
- geolocalização precisa ligada diretamente ao titular;
- documento ou PDF.

## RSO-002 — Hash/HMAC obrigatório para identificadores sensíveis

Criar componente reutilizável:

```java
RedisKeyHasher.hmacSha256(String raw)
```

Regras:

- segredo via env;
- saída curta aceitável: prefixo de 32 ou 48 chars hex/base64url;
- nunca logar valor original;
- nunca usar `String.hashCode()` para dado sensível.

## RSO-003 — Token cru nunca entra no Redis

Para JWT blacklist:

```text
key = kronos:prod:auth:blacklist:jwt:{sha256(rawToken)}
value = "1" ou metadata mínima
TTL = expiração original do token
```

Para reset token:

```text
key = kronos:prod:auth:password-reset:{sha256(resetToken)}
value = userId
TTL = configuração de recuperação
```

## RSO-004 — Consentimentos legais continuam duráveis

Consentimentos biométricos e evidências continuam no banco/S3/documentos atuais.
Redis pode cachear leitura de termo público ou status por poucos minutos, mas a decisão de validade precisa respeitar o banco e as regras de versão/hash.

## RSO-005 — Checkin é fluxo fiscal sensível

Redis pode impedir concorrência, mas não pode gerar ou persistir a verdade fiscal.

Obrigatório:

- lock por colaborador/data;
- owner token no lock;
- release seguro por Lua comparando owner;
- TTL curto;
- persistência final no PostgreSQL;
- invalidação de caches após sucesso.

## RSO-006 — Métricas sem cardinalidade explosiva

Não usar labels com:

- userId;
- employeeId;
- companyId;
- CPF;
- e-mail;
- username;
- IP;
- token hash.

Labels permitidas:

- `operation`;
- `result`;
- `cache_name` controlado;
- `reason` controlado;
- `profile` se necessário.

## RSO-007 — Logs auditáveis, mas minimizados

Usar padrão:

```text
event=redis_operation operation=auth_rate_limit result=allowed
```

Não usar:

```text
cpf=...
email=...
token=...
username=...
ip=...
```

Quando necessário, usar referência minimizada gerada por componente próprio ou `PrivacyLogReferenceService`.

## RSO-008 — Segurança de produção Hostinger

Redis em produção deve:

- rodar localmente na VPS;
- não expor porta 6379 publicamente;
- exigir senha forte;
- usar bind local ou rede Docker interna;
- ter volume persistente somente para AOF quando necessário;
- usar `protected-mode yes`;
- usar firewall bloqueando 6379 externo;
- ser monitorado por healthcheck.

## RSO-009 — Eviction policy conservadora

Como Redis armazenará blacklist, reset token, locks e rate limit, não usar política que possa remover silenciosamente chaves críticas.

Preferir:

```text
maxmemory-policy noeviction
```

Cache deve ter TTL curto e falhar aberto quando Redis recusar escrita por memória.

## RSO-010 — Revisão final obrigatória

Antes de concluir:

- procurar `ConcurrentHashMap` remanescente nos rate limits;
- procurar chaves com CPF/e-mail/username/IP cru;
- procurar token cru em Redis;
- verificar TTL em todas as gravações;
- validar que `./gradlew test` passa;
- validar que front continua sem mudança obrigatória de contrato;
- atualizar docs do `kronos-business` quando a implementação estabilizar.
