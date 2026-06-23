# Prompt mestre para Claude Code — Kronos CTO Demo Sandbox

Você é o agente de execução do projeto Kronos. Implemente o fluxo CTO para `CRIAR DEMO` e `DELETAR DEMO`.

## Contexto obrigatório

Repositórios locais:

```text
Os 3 repos estão dentro dessa pasta
/home/kronos/Documentos/Codigin/kronos/local
```

Branches obrigatórias:

```text
Backend: homolog
Frontend: homolog
Docs: main
```

## Objetivo

Criar uma funcionalidade segura, produtiva e idempotente para que um usuário com role `CTO` consiga:

1. clicar em `CRIAR DEMO`;
2. o sistema criar automaticamente uma empresa sandbox `Kronos Teste`;
3. criar usuário `kronos_teste` com senha inicial `kronos_teste#` e role `MANAGER`;
4. criar employee vinculado ao usuário;
5. criar registros de ponto do mês vigente;
6. criar um documento sintético para cada tipo de documento;
7. criar `BIOMETRIC_CONSENT_TERM` sintético;
8. criar uma solicitação pendente de férias;
9. criar uma solicitação pendente de abono;
10. criar uma solicitação pendente de ajuste de registro;
11. clicar em `DELETAR DEMO`;
12. o sistema deletar todos os dados e arquivos da sandbox;
13. o sistema validar que não ficou resíduo;
14. uma execução futura conseguir limpar qualquer sandbox suja parcial.

## Regras críticas

- Não altere o provider global para local.
- Empresas reais continuam com storage normal do sistema.
- Empresa sandbox usa storage local na VPS.
- Caminho base da sandbox: `/opt/kronos/sandbox/kronos-teste`.
- Não usar AWS S3 para sandbox.
- Não usar Rekognition real para sandbox.
- Não capturar face real.
- Não armazenar imagem real.
- Não usar dados reais.
- `BIOMETRIC_CONSENT_TERM` deve ser sintético.
- Não permitir que `kronos_teste` altere username ou senha.
- Não permitir que `kronos_teste` crie integrações externas.
- Não permitir que `kronos_teste` altere configurações globais.
- Não permitir vazamento entre tenants.
- Não remover empresa real.
- Não deletar com base apenas no nome `Kronos Teste`.
- Use `sandboxKey` técnico, preferencialmente `KRONOS_TESTE`.
- Implementar lock de execução.
- Implementar kill switch.
- Implementar auditoria técnica mínima.
- Invalidar refresh tokens, sessões, cookies, cache de permissões, cache de company/user, rate-limit e contexto em memória conforme existir no projeto.
- Se JWT for totalmente stateless e não houver revogação server-side, implemente a melhor mitigação compatível e documente a limitação.

## Passo 1 — Validar branches

Execute:

```bash
cd /home/deploy/apps/Kronos-Tech-Solutions-KTS
git branch --show-current
git status --short

cd /home/deploy/apps/Kronos-Tech-Solution-User-Plataform
git branch --show-current
git status --short

cd /home/deploy/apps/kronos-business
git branch --show-current
git status --short
```

Não continue se as branches não forem:

```text
homolog
homolog
main
```

Não sobrescreva alterações locais sem explicar.

## Passo 2 — Ler regras e plano

Leia estes arquivos no backend após instalar o pacote:

```text
.claude/rules/kronos-demo-sandbox.rules.md
.claude/skills/kronos-demo-sandbox/SKILL.md
.claude/agents/kronos-demo-sandbox-architect.md
.claude/agents/kronos-backend-demo-agent.md
.claude/agents/kronos-frontend-demo-agent.md
.claude/agents/kronos-qa-security-agent.md
docs_index/REPOSITORY_READING_MAP.md
docs_index/DEMO_SANDBOX_CONTRACT.md
plan/IMPLEMENTATION_PLAN_CTO_DEMO_SANDBOX.md
```

## Passo 3 — Mapear arquivos reais antes de implementar

No backend:

```bash
cd /home/deploy/apps/Kronos-Tech-Solutions-KTS

sed -n '1,240p' build.gradle
sed -n '1,260p' src/main/resources/application.yml
find src/main/resources/db/migration -type f | sort | tail -40

rg -n "class Company|@Entity.*Company|CompanyRepository|companyId|company_id|sandbox|tenant" src/main/java src/main/resources
rg -n "class User|@Entity.*User|UserRepository|username|PasswordEncoder|Role|MANAGER|CTO|PARTNER" src/main/java src/main/resources
rg -n "class Employee|@Entity.*Employee|EmployeeRepository|employeeId|employee_id" src/main/java src/main/resources
rg -n "DocumentType|Document|BIOMETRIC_CONSENT_TERM|Storage|StorageService|S3|bucket|Rekognition|face|biometric" src/main/java src/main/resources
rg -n "Vacation|Ferias|Férias|TimeOff|Abono|Manual|Adjustment|PointRecord|Registro|Ponto" src/main/java src/main/resources
rg -n "Jwt|JWT|Refresh|Session|Cookie|Redis|Cache|RateLimit|Permission|SecurityFilterChain|PreAuthorize" src/main/java src/main/resources
rg -n "Mail|Webhook|WhatsApp|Here|Geocode|AWS|S3|Rekognition" src/main/java src/main/resources
```

No front-end:

```bash
cd /home/deploy/apps/Kronos-Tech-Solution-User-Plataform

sed -n '1,240p' package.json
sed -n '1,260p' src/App.tsx
sed -n '1,360p' src/config/app-routes.ts

rg -n "APP_PATHS|APP_ROUTE_META|RoleRoute|ProtectedRoute|CTO|MANAGER|PARTNER" src
rg -n "AuthContext|user.role|role|activeCompany|selectedCompany|company|isSandbox" src
rg -n "axios|apiClient|http|service|useMutation|useQuery|queryClient" src
rg -n "Administracao|Empresa|Dashboard|Button|Card|AlertDialog|Dialog|toast|Badge|Banner" src
```

Na documentação:

```bash
cd /home/deploy/apps/kronos-business

sed -n '1,220p' README.md
sed -n '1,220p' 01-visao-negocio.md

rg -n "CTO|MANAGER|empresa|documento|ponto|férias|ferias|abono|LGPD|storage|AWS|Rekognition|sessão|token|cache|migration|scheduler|integrações" .
```

Crie um resumo interno do mapeamento antes de alterar arquivos.

## Passo 4 — Implementar backend

Implemente os itens abaixo em código real, adaptando aos nomes existentes do projeto.

### 4.1 Configuração

Adicionar propriedades:

```yaml
kronos:
  demo:
    enabled: ${KRONOS_DEMO_ENABLED:false}
    kill-switch: ${KRONOS_DEMO_KILL_SWITCH:false}
    sandbox-key: ${KRONOS_DEMO_SANDBOX_KEY:KRONOS_TESTE}
    company-name: ${KRONOS_DEMO_COMPANY_NAME:Kronos Teste}
    username: ${KRONOS_DEMO_USERNAME:kronos_teste}
    initial-password: ${KRONOS_DEMO_INITIAL_PASSWORD:kronos_teste#}
    local-storage-root: ${KRONOS_DEMO_LOCAL_STORAGE_ROOT:/opt/kronos/sandbox/kronos-teste}
    lock-timeout: ${KRONOS_DEMO_LOCK_TIMEOUT:PT5M}
```

Default precisa ser seguro.

### 4.2 Migration

Criar migration Flyway com:

- campo/enum para empresa sandbox;
- `sandbox_key`;
- índice único;
- tabela de auditoria técnica;
- lock table se necessário.

Não assumir nome de tabela. Use os nomes reais.

### 4.3 Services

Criar services isolados:

```text
DemoSandboxCreateService
DemoSandboxPurgeService
DemoSandboxValidationService
DemoSandboxAuditService
DemoSandboxLockService
DemoSandboxDataFactory
DemoSandboxSessionInvalidationService
SandboxExternalIntegrationGuard
```

Adaptar nomes ao padrão real.

### 4.4 Seed

Criar:

- company `Kronos Teste`;
- user `kronos_teste`;
- password hash usando `PasswordEncoder`;
- role `MANAGER`;
- employee vinculado;
- registros do mês vigente;
- documentos sintéticos por enum/tabela real;
- termo biométrico sintético;
- férias pendente;
- abono pendente;
- ajuste de registro pendente.

### 4.5 Purge

O purge deve limpar:

- documentos;
- arquivos locais;
- assinaturas;
- termo biométrico;
- solicitações;
- aprovações;
- ponto;
- employees;
- vínculos user-company;
- refresh tokens;
- sessões;
- caches;
- rate-limit;
- user `kronos_teste`;
- company sandbox;
- pasta `/opt/kronos/sandbox/kronos-teste`.

Deve limpar também resíduos parciais.

### 4.6 Storage

Implementar roteamento por company:

```text
sandbox -> local /opt/kronos/sandbox/kronos-teste/company/{companyId}
real -> provider atual
```

Não mudar `kronos.storage.provider`.

### 4.7 API CTO

Criar endpoints:

```http
POST   /api/cto/demo/create
DELETE /api/cto/demo
GET    /api/cto/demo/status
POST   /api/cto/demo/validate
```

Proteção:

```text
role CTO
```

Usar padrão real do projeto.

### 4.8 Testes backend

Criar testes suficientes para:

- create idempotente;
- purge idempotente;
- sandbox suja;
- não apagar empresa real;
- storage sandbox;
- bloqueio AWS/Rekognition;
- segurança CTO;
- validação pós-purge.

Execute:

```bash
./gradlew clean test
```

## Passo 5 — Implementar frontend

Depois do backend:

1. Criar tipos.
2. Criar service HTTP.
3. Criar hook com query/mutation.
4. Criar tela/área CTO.
5. Adicionar rota restrita.
6. Adicionar botões `CRIAR DEMO` e `DELETAR DEMO`.
7. Adicionar confirmação para deleção.
8. Adicionar status/resultado/validação.
9. Adicionar banner para empresa sandbox.
10. Bloquear visualmente ações proibidas para sandbox.

Execute:

```bash
npm run lint
npm run test
npm run build
```

## Passo 6 — Atualizar documentação

No `kronos-business`, atualizar:

```text
06-contratos-api.md
04-fluxos-aplicacao.md
05-entradas-saidas-fluxos.md
10-regras-negocio.md
11-autenticacao-seguranca.md
12-autorizacao-permissoes.md
13-integracoes-externas.md
14-banco-dados-migrations.md
15-lgpd-privacidade.md
17-ambientes-variaveis.md
19-troubleshooting.md
```

Criar ADR se houver nova decisão estrutural, por exemplo:

```text
ADR-cto-demo-sandbox-storage-routing.md
```

## Passo 7 — Relatório final

Ao concluir, responda com:

1. resumo do que foi implementado;
2. arquivos alterados por repositório;
3. migrations criadas;
4. endpoints criados;
5. variáveis de ambiente novas;
6. testes criados;
7. comandos executados e resultado;
8. como testar manualmente;
9. riscos/limitações;
10. rollback.
