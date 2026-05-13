# Backlog de Correções — Kronos `PROD_HOSTINGER`

> Origem: auditoria de produção da branch `PROD_HOSTINGER`.
> Objetivo: organizar correções e melhorias por prioridade para preparar a branch para produção.

# Prioridade P1 — Segurança Alta, Produção e Integridade

## Objetivo

Corrigir riscos altos que não necessariamente impedem o build, mas tornam a aplicação insegura ou frágil para produção.

## Critério para encerrar P1

A prioridade P1 só deve ser considerada concluída quando:

- existir profile produtivo seguro;
- reverse proxy estiver configurado corretamente;
- migrations versionadas existirem;
- constraints críticas estiverem no banco;
- houver proteção contra brute force;
- recuperação de senha tiver rate limit;
- login facial tiver liveness obrigatório em produção;
- erros críticos não vazarem detalhes técnicos.

---

# KRN-P1-008 — Criar profile `application-prod.yml`

## Tipo

Configuração / Produção

## Severidade

Alta

## Problema

Não existe `application-prod.yml`. Apenas `application.yml` foi encontrado.

Isso aumenta risco de usar configurações de desenvolvimento em produção.

## Arquivos envolvidos

| Arquivo | Ação |
|---|---|
| `application.yml` | Manter defaults neutros |
| `application-prod.yml` | Criar configuração segura |
| Dockerfile/deploy | Ativar profile `prod` |

## Configurações mínimas esperadas

```yaml
server:
  forward-headers-strategy: framework

logging:
  level:
    org.springframework.security: INFO
    org.hibernate.SQL: WARN

springdoc:
  swagger-ui:
    enabled: false
  api-docs:
    enabled: false
```

## Critérios de aceite

- Profile `prod` existe.
- `mail.smtp.debug=false` em produção.
- `org.springframework.security` não fica `DEBUG`.
- Swagger fica desabilitado em produção.
- Actuator fica restrito.
- Forward headers são configurados.

## Testes obrigatórios

- Aplicação sobe com `SPRING_PROFILES_ACTIVE=prod`.
- Logs não exibem debug de segurança.
- Swagger não abre em prod.
- Healthcheck continua funcionando.

---

# KRN-P1-009 — Configurar reverse proxy / Hostinger

## Tipo

Deploy / Cookies / Proxy

## Severidade

Alta

## Problema

Falta `server.forward-headers-strategy`.

Isso pode quebrar HTTPS, cookies `Secure` e captura de IP real atrás de proxy.

## Arquivos envolvidos

| Arquivo | Ação |
|---|---|
| `application-prod.yml` | Configurar forward headers |
| Configuração Hostinger/Nginx | Garantir headers |
| `SecurityConfig.java` | Validar origem e CORS |

## Configuração esperada

```yaml
server:
  forward-headers-strategy: framework
```

No proxy:

```nginx
proxy_set_header X-Forwarded-Proto https;
proxy_set_header X-Forwarded-Host $host;
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
```

## Critérios de aceite

- App reconhece HTTPS real atrás do proxy.
- Cookie `Secure` funciona.
- IP real é capturado corretamente em logs/auditoria.
- CORS continua restrito ao domínio real do front-end.

---

# KRN-P1-010 — Adicionar Flyway ou Liquibase

## Tipo

Banco / Produção

## Severidade

Alta

## Problema

Não foram encontradas migrations versionadas.

Isso bloqueia reprodutibilidade, auditoria e controle do schema produtivo.

## Arquivos envolvidos

| Arquivo | Ação |
|---|---|
| `build.gradle` | Adicionar Flyway ou Liquibase |
| `src/main/resources/db/migration` | Criar migrations |
| `application.yml` | Configurar migration |
| Entidades JPA | Alinhar com schema |

## Implementação recomendada

Usar Flyway:

```gradle
implementation 'org.flywaydb:flyway-core'
implementation 'org.flywaydb:flyway-database-postgresql'
```

Criar estrutura:

```text
src/main/resources/db/migration/V1__initial_schema.sql
src/main/resources/db/migration/V2__constraints_indexes.sql
src/main/resources/db/migration/V3__soft_delete_fields.sql
```

## Critérios de aceite

- Banco vazio é criado apenas via migrations.
- `ddl-auto` não é usado para criar schema em produção.
- CI executa migrations em ambiente de teste.
- Schema local e produtivo são reproduzíveis.

## Testes obrigatórios

- Subir banco limpo e rodar aplicação.
- Validar criação de tabelas.
- Validar constraints.
- Validar rollback manual documentado, se usar Flyway Community.

---

# KRN-P1-011 — Criar constraints críticas

## Tipo

Banco / Integridade

## Severidade

Média / Alta

## Problema

Há risco de duplicidade por corrida, especialmente em CNPJ, CPF e username, caso não haja constraints reais no banco.

## Constraints recomendadas

| Tabela | Campo | Constraint |
|---|---|---|
| `tb_company` | `company_cnpj` | unique |
| `tb_employee` | `cpf` | unique ou unique parcial conforme regra |
| `tb_user` | `username` | unique lower-case |
| `tb_company_nsr` | `company_id` | primary key/unique |
| `tb_password_reset_token` | `user_id` | unique para token ativo |

## Critérios de aceite

- Corrida de criação de empresa não permite CNPJ duplicado.
- Erro de duplicidade retorna `409`.
- Migrations criam índices únicos.
- Services tratam `DataIntegrityViolationException`.

## Testes obrigatórios

- Criar empresa duplicada retorna `409`.
- Criar usuário duplicado retorna `409`.
- Criar colaborador duplicado retorna `409`.

---

# KRN-P1-012 — Padronizar response de erro global

## Tipo

Contrato API / Segurança / Observabilidade

## Severidade

Alta / Média

## Problema

O handler de erros usa `ex.getMessage()` diretamente em alguns fluxos e há JSON manual em filtro.

Isso pode vazar mensagens técnicas e gerar respostas inconsistentes.

## Arquivos envolvidos

| Arquivo | Ação |
|---|---|
| `RestExceptionHandler.java` | Criar padrão final |
| `ProblemDetail.java` | Incluir `code`, `path`, `timestamp` |
| `TermsValidationFilter.java` | Parar de escrever JSON manual |
| Exceptions customizadas | Mapear corretamente |

## Response padrão sugerido

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Dados inválidos.",
  "status": 400,
  "path": "/auth/login",
  "timestamp": "2026-05-13T21:00:00Z",
  "validationErrors": [
    {
      "field": "username",
      "message": "Username é obrigatório."
    }
  ]
}
```

## Critérios de aceite

- Nenhum erro retorna stack trace.
- Mensagens técnicas não vazam para o cliente.
- JSON inválido retorna `400`.
- Enum inválido retorna `400`.
- Acesso negado retorna `403`.
- Token ausente/inválido retorna `401`.
- Erro inesperado retorna `500` genérico com log interno.

## Testes obrigatórios

- JSON inválido.
- DTO inválido.
- Path param inválido.
- Role insuficiente.
- Exception genérica.
- TermsValidationFilter usando padrão global.

---

# KRN-P1-013 — Rate limit para login

## Tipo

Segurança / Brute force

## Severidade

Alta

## Problema

Login por senha não tem rate limit ou bloqueio progressivo.

## Implementação recomendada

Usar Bucket4j, Redis ou cache local inicialmente.

Chaves:

- IP
- username normalizado
- combinação IP + username

## Política inicial sugerida

| Evento | Limite |
|---|---|
| Login por IP | 10 tentativas por minuto |
| Login por username | 5 tentativas por 5 minutos |
| Bloqueio progressivo | 5, 15, 30 minutos |

## Critérios de aceite

- Excesso de login retorna `429`.
- Falhas são auditadas.
- Sucesso limpa contador do usuário.
- Não vaza se username existe.

## Testes obrigatórios

- 5 falhas bloqueiam username.
- IP com muitas tentativas recebe `429`.
- Login correto após bloqueio ainda falha até expirar cooldown.

---

# KRN-P1-014 — Rate limit para recuperação de senha

## Tipo

Segurança / Abuse prevention

## Severidade

Alta

## Problema

O fluxo é neutro, mas sem rate limit pode ser usado para abuso de envio de e-mail.

## Regras sugeridas

| Chave | Limite |
|---|---|
| CPF | 3 solicitações por hora |
| E-mail | 3 solicitações por hora |
| IP | 10 solicitações por hora |

## Critérios de aceite

- Resposta continua neutra para evitar enumeração.
- Abuso não dispara e-mail.
- Evento é auditado internamente.
- Token anterior é substituído ou invalidado conforme regra.

## Testes obrigatórios

- Muitas tentativas para o mesmo CPF não enviam novos e-mails.
- Muitas tentativas pelo mesmo IP são bloqueadas.
- Resposta externa continua neutra.

---

# KRN-P1-015 — Política de login facial com liveness

## Tipo

Segurança / Biometria

## Severidade

Alta

## Problema

`liveness-required` está `false` por padrão.

## Implementação esperada

Em produção:

```yaml
kronos:
  face:
    liveness-required: true
```

Ou bloquear login facial quando liveness não estiver configurado.

## Critérios de aceite

- Produção não permite login facial sem liveness.
- Ambiente dev/test pode desabilitar explicitamente.
- Falha de liveness retorna `401` ou `403` conforme padrão.
- Evento é auditado.

## Testes obrigatórios

- Login facial sem liveness em prod falha.
- Login facial com liveness válido funciona.
- Erro não vaza detalhe técnico.

---

# Checklist final P1

- [ ] Existe `application-prod.yml`.
- [ ] Logs de produção não usam DEBUG.
- [ ] Proxy/forward headers configurados.
- [ ] Flyway ou Liquibase configurado.
- [ ] Migrations iniciais criadas.
- [ ] Constraints críticas criadas.
- [ ] Handler global padronizado.
- [ ] Login tem rate limit.
- [ ] Recuperação de senha tem rate limit.
- [ ] Liveness facial obrigatório em produção.
