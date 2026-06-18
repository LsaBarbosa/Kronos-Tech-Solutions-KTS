# Plano de ação — Implementação Redis no Kronos

## ÉPICO 00 — Auditoria inicial e baseline

### História 00.01 — Confirmar branches e estado atual

Tasks:

1. Fazer checkout do back-end em `prod-redis`.
2. Fazer checkout do front-end em `PROD_HOSTINGER_v2`.
3. Fazer checkout da documentação em `main`.
4. Rodar baseline:
   ```bash
   ./gradlew clean test
   npm run lint
   npm run test
   npm run build
   ```
5. Registrar falhas existentes antes de implementar Redis.

Critério de aceite:

- Branches confirmadas.
- Baseline registrado.
- Nenhuma alteração iniciada sem saber estado inicial.

### História 00.02 — Mapear classes atuais

Tasks:

1. Mapear rate limit em memória.
2. Mapear providers de token reset e blacklist.
3. Mapear fluxo de checkin.
4. Mapear consultas candidatas a cache.
5. Mapear configuração de deploy Hostinger.

Critério de aceite:

- Lista de arquivos alvo criada.
- Riscos anotados.

---

## ÉPICO 01 — Fundação Redis

### História 01.01 — Adicionar dependências Redis

Tasks:

1. Alterar `build.gradle`.
2. Adicionar `spring-boot-starter-data-redis`.
3. Adicionar `spring-boot-starter-cache`.
4. Garantir que testes existentes compilam.

Critério de aceite:

- Projeto compila.
- Dependências gerenciadas pelo Spring Boot BOM.

### História 01.02 — Criar configuração Redis

Tasks:

1. Criar `RedisProperties` com `@ConfigurationProperties`.
2. Criar `RedisConfig`.
3. Configurar `StringRedisTemplate`.
4. Configurar `RedisCacheConfiguration`.
5. Configurar TTL default e serializers.
6. Adicionar propriedades em `application.yml`, `application-prod.yml`, `.env.example`.

Critério de aceite:

- App sobe com Redis habilitado.
- App sobe em teste sem exigir Redis real para testes unitários.

### História 01.03 — Criar factory de chaves e hasher

Tasks:

1. Criar `RedisKeyFactory`.
2. Criar `RedisKeyHasher` com HMAC-SHA256.
3. Criar testes de chaves.
4. Garantir que CPF/e-mail/username/IP não aparecem crus.

Critério de aceite:

- Todas as chaves passam por factory.
- Testes cobrem PII.

---

## ÉPICO 02 — Rate limit Redis

### História 02.01 — Criar porta `RateLimitStore`

Tasks:

1. Definir métodos genéricos de consumo de janela.
2. Definir método de cooldown/bloqueio.
3. Criar implementação in-memory/fake para testes.
4. Criar implementação Redis.

Critério de aceite:

- Rate limit desacoplado da estrutura concreta.

### História 02.02 — Refatorar `AuthenticationRateLimitService`

Tasks:

1. Remover buckets `ConcurrentHashMap`.
2. Migrar login IP para Redis.
3. Migrar falhas username/cooldown para Redis.
4. Migrar recovery CPF/e-mail/IP para Redis.
5. Migrar admin-check para Redis.
6. Preservar mensagens atuais.

Critério de aceite:

- Endpoints de auth retornam 429 quando limite excede.
- Sucesso de login limpa falhas por username.
- Não há PII crua em chave.

### História 02.03 — Refatorar `BiometricProtectionService`

Tasks:

1. Remover bucket em memória.
2. Migrar login facial para Redis.
3. Migrar checkin biométrico para Redis.
4. Migrar enrollment para Redis.
5. Preservar liveness conforme configuração oficial do projeto.

Critério de aceite:

- Rate limit biométrico distribuído funcionando.
- Nenhuma imagem facial armazenada em Redis.

---

## ÉPICO 03 — Tokens temporários e blacklist

### História 03.01 — Implementar blacklist JWT com Redis

Tasks:

1. Implementar provider Redis da porta existente.
2. Usar hash SHA-256 do token.
3. TTL = expiração original do JWT.
4. Ajustar composição de beans por profile/config.
5. Testar logout e refresh.

Critério de aceite:

- Token deslogado não é aceito.
- Token antigo do refresh é revogado.
- Nenhum token cru é salvo.

### História 03.02 — Implementar password reset token com Redis

Tasks:

1. Implementar provider Redis.
2. Gerar token seguro.
3. Salvar hash do token com userId e TTL.
4. Validar token.
5. Deletar após uso.
6. Testar expiração.

Critério de aceite:

- Reset token é temporário e uso único.
- Recover password continua neutro.

---

## ÉPICO 04 — Lock/idempotência em checkin

### História 04.01 — Criar `DistributedLockProvider`

Tasks:

1. Implementar acquire com `SET NX PX`.
2. Implementar release com Lua e owner token.
3. Criar testes de concorrência.

Critério de aceite:

- Lock seguro por owner.

### História 04.02 — Aplicar lock no checkin

Tasks:

1. Identificar método principal de checkin no `TimeRecordService`.
2. Adicionar lock por `employeeId + data` ao redor do fluxo atual.
3. Retornar erro padronizado para operação concorrente.
4. Invalidar caches após sucesso.

Critério de aceite:

- Duplo clique não cria marcação duplicada.
- Fluxo fiscal permanece no PostgreSQL/serviços atuais.

---

## ÉPICO 05 — Cache dos endpoints específicos

### História 05.01 — Cache do dashboard

Tasks:

1. Cachear `GET /dashboard/summary` por usuário/papel/tenant.
2. TTL curto.
3. Invalidar em mudanças relevantes ou aceitar expiração curta.

Critério de aceite:

- Hit/miss mensurado.
- Sem vazamento cross-tenant.

### História 05.02 — Cache de perfil próprio

Tasks:

1. Cachear `/users/own-profile`.
2. Cachear `/employee/own-profile`.
3. Invalidar em update, troca de senha, toggle active, revogação sensível.

Critério de aceite:

- Dados atualizados após escrita.

### História 05.03 — Cache de listas gerenciais

Tasks:

1. Cachear `/employee?active=` por tenant e filtro.
2. Cachear `/users/search?active=` por tenant e filtro.
3. Invalidar em create/update/delete/toggle.

Critério de aceite:

- MANAGER não acessa cache de outro tenant.

### História 05.04 — Cache de ponto do portal

Tasks:

1. Cachear `/records/me/today`.
2. Cachear `/records/me/recent`.
3. Cachear `/records/me/requests`.
4. Invalidar em checkin, update, approval, vacation/time-off changes.

Critério de aceite:

- Após bater ponto, status atualizado corretamente.

### História 05.05 — Cache de geolocalização

Tasks:

1. Cachear resposta de `POST /geolocation/resolve` por hash de endereço/CEP.
2. TTL de 1 a 7 dias.
3. Não incluir chave HERE em log/cache.

Critério de aceite:

- Segunda consulta igual evita chamada externa.

### História 05.06 — Cache de privacidade pública

Tasks:

1. Cachear `/public/privacy/processing-catalog`.
2. Cachear `/public/privacy/policy`.
3. Cachear `/public/privacy/biometric-term`.
4. TTL médio.

Critério de aceite:

- Conteúdo público sem dado pessoal.

---

## ÉPICO 06 — Deploy Hostinger

### História 06.01 — Adicionar Redis ao deploy

Tasks:

1. Atualizar `docker-compose.yml` ou documentação systemd.
2. Adicionar Redis local com senha.
3. Garantir `127.0.0.1` ou rede Docker interna.
4. Adicionar volume e healthcheck.
5. Atualizar `.env.example`.

Critério de aceite:

- Redis funciona localmente.
- Porta não está pública.

### História 06.02 — Configurar profile prod

Tasks:

1. Adicionar envs no arquivo de produção.
2. Validar conexão Redis no startup.
3. Expor health sem detalhes sensíveis.
4. Confirmar Actuator/Prometheus.

Critério de aceite:

- App sobe em prod com Redis.

---

## ÉPICO 07 — Testes, revisão e documentação

### História 07.01 — Testes automatizados

Tasks:

1. Unit tests de key factory.
2. Unit tests de rate limit.
3. Unit tests de blacklist.
4. Unit tests de reset token.
5. Unit tests de lock.
6. Integration tests com Redis Testcontainers.
7. Testes de endpoints críticos.

Critério de aceite:

- `./gradlew clean test` passa.

### História 07.02 — Validação front-end

Tasks:

1. Rodar lint/test/build.
2. Validar que contrato HTTP não mudou.
3. Validar tratamento de 429.

Critério de aceite:

- Front continua funcionando sem saber que Redis existe.

### História 07.03 — Atualizar documentação

Tasks:

1. Atualizar `kronos-business/main` com arquitetura Redis.
2. Atualizar fluxos afetados.
3. Atualizar regras de negócio.
4. Atualizar deploy Hostinger.
5. Adicionar operação/rollback.

Critério de aceite:

- Documentação reflete código final.
