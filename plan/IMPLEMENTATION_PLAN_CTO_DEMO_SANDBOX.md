# Plano de ação — Fluxo CTO CRIAR DEMO / DELETAR DEMO

## 0. Preparação obrigatória

### 0.1 Validar branches

```bash
cd /home/deploy/apps/Kronos-Tech-Solutions-KTS
git fetch --all --prune
git switch homolog
git status --short

cd /home/deploy/apps/Kronos-Tech-Solution-User-Plataform
git fetch --all --prune
git switch homolog
git status --short

cd /home/deploy/apps/kronos-business
git fetch --all --prune
git switch main
git status --short
```

Abortar se houver alteração local não entendida.

### 0.2 Criar branch de trabalho

Back-end:

```bash
cd /home/deploy/apps/Kronos-Tech-Solutions-KTS
git switch -c feature/cto-demo-sandbox
```

Front-end:

```bash
cd /home/deploy/apps/Kronos-Tech-Solution-User-Plataform
git switch -c feature/cto-demo-sandbox
```

Docs:

```bash
cd /home/deploy/apps/kronos-business
git switch -c docs/cto-demo-sandbox
```

## 1. Mapeamento técnico

### 1.1 Backend

Entregar tabela de mapeamento com:

- entidades;
- tabelas;
- FKs;
- repositories;
- services;
- controllers;
- enums;
- migrations;
- storage;
- AWS/Rekognition;
- sessão/cache/rate-limit.

### 1.2 Frontend

Entregar tabela de mapeamento com:

- rotas;
- guard de role;
- service HTTP;
- contexto de autenticação;
- área de administração CTO;
- componentes UI;
- testes.

### 1.3 Documentação

Entregar lista de documentos que precisarão atualização.

## 2. Design técnico backend

### 2.1 Modelo sandbox

Adicionar metadado técnico na empresa:

```text
company_type = SANDBOX
sandbox_key = KRONOS_TESTE
```

ou equivalente compatível.

### 2.2 Auditoria

Criar tabela de auditoria técnica:

```text
tb_demo_job_audit
```

Sem dados pessoais.

### 2.3 Lock

Implementar lock transacional/persistente.

Nome lógico:

```text
KRONOS_DEMO_SANDBOX_LOCK
```

### 2.4 Config

Adicionar `kronos.demo.*` em properties.

### 2.5 API

Implementar endpoints CTO:

```http
POST   /api/cto/demo/create
DELETE /api/cto/demo
GET    /api/cto/demo/status
POST   /api/cto/demo/validate
```

## 3. Implementação backend

### Task BE-01 — Configuração

- Criar properties.
- Default desativado.
- Adicionar kill switch.
- Testar bind de config.

### Task BE-02 — Migration

- Adicionar campos sandbox.
- Criar auditoria.
- Criar índice único.
- Criar lock table se necessário.

### Task BE-03 — DTOs

Criar responses:

- `DemoCreateResponse`;
- `DemoPurgeResponse`;
- `DemoStatusResponse`;
- `DemoValidationResponse`;
- `DemoValidationIssue`;
- `DemoOperationCounters`.

### Task BE-04 — Seed service

Criar serviço de criação:

- purge prévio;
- company;
- user;
- employee;
- point records;
- documents;
- biometric consent synthetic;
- requests;
- audit;
- validation.

### Task BE-05 — Purge service

Criar serviço de purge:

- idempotente;
- FK-aware;
- orphans-aware;
- file-aware;
- session/cache-aware;
- auditado.

### Task BE-06 — Storage sandbox

Criar roteamento por empresa:

```text
Company sandbox -> local sandbox storage
Company real -> provider atual
```

### Task BE-07 — Guard integrações externas

Bloquear para sandbox:

- AWS S3;
- Rekognition;
- e-mail real;
- WhatsApp;
- webhooks;
- geocoding externo, se aplicável.

### Task BE-08 — Controller CTO

Endpoints protegidos por role `CTO`.

### Task BE-09 — Testes backend

Criar testes unitários/integrados.

### Task BE-10 — Build backend

```bash
./gradlew clean test
```

## 4. Implementação frontend

### Task FE-01 — Tipos

Criar tipos de resposta do demo sandbox.

### Task FE-02 — Service

Criar service HTTP:

- `getDemoStatus`;
- `createDemo`;
- `deleteDemo`;
- `validateDemo`.

### Task FE-03 — Hook

Criar hook com React Query:

- query status;
- mutations create/delete/validate;
- invalidation.

### Task FE-04 — Página/Componente CTO

Adicionar UI em área CTO.

### Task FE-05 — Rotas

Atualizar:

- `APP_PATHS`;
- `APP_ROUTE_META`;
- `App.tsx`;
- menu/sidebar se existir.

### Task FE-06 — Banner sandbox

Mostrar banner quando empresa ativa for sandbox.

### Task FE-07 — Bloqueios visuais

Desabilitar:

- alterar username;
- alterar senha;
- integrações externas;
- configurações globais.

### Task FE-08 — Testes frontend

- CTO vê;
- outros não;
- create/delete/status;
- loading/erro/sucesso;
- banner.

### Task FE-09 — Build frontend

```bash
npm run lint
npm run test
npm run build
```

## 5. Documentação

### Task DOC-01 — Contratos API

Atualizar `06-contratos-api.md`.

### Task DOC-02 — Fluxos

Atualizar fluxos de aplicação e entradas/saídas.

### Task DOC-03 — Segurança/autorização

Atualizar documentos de segurança e permissões.

### Task DOC-04 — LGPD

Documentar dados sintéticos, sem face real, sem Rekognition real.

### Task DOC-05 — Ambientes/deploy

Documentar variáveis `KRONOS_DEMO_*`.

### Task DOC-06 — Troubleshooting

Adicionar cenários:

- lock preso;
- purge parcial;
- user órfão;
- pasta antiga;
- documento órfão;
- cache residual.

### Task DOC-07 — ADR

Criar ADR se houver novo roteador de storage ou modelo `company_type`.

## 6. Validação manual

### 6.1 Criar demo

1. Login como CTO.
2. Acessar tela admin.
3. Clicar `CRIAR DEMO`.
4. Confirmar sucesso.
5. Login com:
   - usuário: `kronos_teste`
   - senha: `kronos_teste#`
6. Validar que enxerga apenas `Kronos Teste`.
7. Validar documentos, ponto e solicitações.

### 6.2 Gerar dado posterior

Com `kronos_teste`:

- enviar documento sintético;
- criar aviso, se permitido;
- criar solicitação adicional;
- criar ajuste de registro;
- alterar dado permitido de employee, se existir.

### 6.3 Deletar demo

1. Login como CTO.
2. Clicar `DELETAR DEMO`.
3. Confirmar.
4. Validar resultado limpo.
5. Tentar login com `kronos_teste`.
6. Confirmar bloqueio.
7. Validar pasta local removida.

### 6.4 Rodar purge novamente

1. Clicar `DELETAR DEMO` novamente.
2. Resultado deve ser sucesso/idempotente, não erro fatal.

## 7. Critérios de aceite

| Critério | Obrigatório |
|---|---|
| Só CTO cria/deleta | Sim |
| Manager demo limitado | Sim |
| Storage local só sandbox | Sim |
| Provider global preservado | Sim |
| Sem AWS/Rekognition para sandbox | Sim |
| Dados sintéticos | Sim |
| Purge idempotente | Sim |
| Lock | Sim |
| Kill switch | Sim |
| Auditoria | Sim |
| Validação pós-purge | Sim |
| Testes backend | Sim |
| Testes frontend | Sim |
| Docs atualizadas | Sim |
