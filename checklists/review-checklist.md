# Checklist final — Redis Kronos

## Branches

- [ ] Back-end em `prod-redis`.
- [ ] Front-end em `PROD_HOSTINGER_v2`.
- [ ] Documentação em `main`.

## Build/configuração

- [ ] `spring-boot-starter-data-redis` adicionado.
- [ ] `spring-boot-starter-cache` adicionado.
- [ ] Configuração Redis por env.
- [ ] `.env.example` atualizado sem segredos reais.
- [ ] `application-prod.yml` com Redis.
- [ ] Profile `test` não exige Redis real para toda suíte.

## Segurança/LGPD

- [ ] Nenhuma chave Redis contém CPF cru.
- [ ] Nenhuma chave Redis contém e-mail cru.
- [ ] Nenhuma chave Redis contém username cru.
- [ ] Nenhuma chave Redis contém IP cru.
- [ ] Nenhum token cru é armazenado.
- [ ] Nenhuma imagem/base64 facial é armazenada.
- [ ] Nenhum documento/PDF é armazenado.
- [ ] Logs não vazam PII.
- [ ] Métricas não têm labels de alta cardinalidade.

## TTL/chaves

- [ ] Toda chave Redis tem TTL.
- [ ] Blacklist JWT usa TTL até expiração do JWT.
- [ ] Reset token usa TTL curto.
- [ ] Locks usam TTL curto.
- [ ] Cache usa TTL curto/médio.
- [ ] Chaves passam por `RedisKeyFactory`.

## Rate limit

- [ ] `AuthenticationRateLimitService` não depende de bucket em memória para prod.
- [ ] `BiometricProtectionService` não depende de bucket em memória para prod.
- [ ] Login IP limitado.
- [ ] Login username/cooldown limitado.
- [ ] Recovery CPF/e-mail/IP limitado.
- [ ] Biometria limitada.
- [ ] Admin-check limitado.

## Tokens

- [ ] Logout adiciona JWT hash à blacklist Redis.
- [ ] Refresh rejeita token em blacklist.
- [ ] Refresh adiciona token antigo à blacklist.
- [ ] Reset token é validado por hash.
- [ ] Reset token é removido após uso.

## Checkin

- [ ] Lock por employee/data.
- [ ] `SET NX PX` usado.
- [ ] Release por Lua validando owner.
- [ ] Fluxo fiscal continua no PostgreSQL.
- [ ] Caches de ponto/dashboard invalidados após sucesso.

## Cache

- [ ] Dashboard cacheado com escopo correto.
- [ ] Own profile cacheado e invalidado.
- [ ] Lists por tenant cacheadas e invalidadas.
- [ ] Records today/recent/requests cacheados e invalidados.
- [ ] Geolocation cacheado sem chave externa em log.
- [ ] Public privacy cacheado sem dado pessoal.

## Hostinger

- [ ] Redis roda localmente na VPS.
- [ ] Porta 6379 não pública.
- [ ] Senha forte via env.
- [ ] Healthcheck configurado.
- [ ] Volume/AOF configurado se necessário.
- [ ] Rollback documentado.

## Testes

- [ ] `./gradlew clean test` executado.
- [ ] Testes de Redis com Testcontainers.
- [ ] Testes de falha Redis.
- [ ] Testes de chave sem PII.
- [ ] Testes de lock concorrente.
- [ ] Testes de token TTL.
- [ ] Testes de cache/invalidação.
- [ ] Front lint/test/build se alterado.

## Documentação

- [ ] Arquitetura Redis documentada.
- [ ] Fluxos alterados documentados.
- [ ] Variáveis de produção documentadas.
- [ ] Deploy Hostinger documentado.
- [ ] Rollback documentado.
