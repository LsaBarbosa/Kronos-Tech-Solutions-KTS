# SPEC — Backlog de Correção e Adequação LGPD do Kronos

**Projeto:** Kronos  
**Branches-alvo:**  
- Back-end: `feature/lgpd-compliance`
- Front-end: `feature/lgpd-compliance`

**Data da especificação:** 2026-05-22  
**Responsável técnico sugerido:** Lucas / Engenharia Kronos  
**Natureza:** backlog técnico-funcional para adequação LGPD.  
**Observação:** este documento é uma especificação técnica de produto e engenharia. Não substitui parecer jurídico.

---

## 1. Objetivo

Adequar o Kronos aos requisitos técnicos e operacionais relacionados à LGPD, com foco em:

1. Garantir tratamento adequado de dados pessoais e dados pessoais sensíveis.
2. Corrigir riscos identificados no fluxo de biometria, exportação, anonimização, retenção, incidentes e segregação multi-tenant.
3. Criar evidências técnicas de conformidade.
4. Preparar o sistema para auditoria interna, revisão jurídica e operação em produção.

---

## 2. Escopo da adequação

### 2.1 Incluído

- Consentimento biométrico.
- Cadastro e uso de biometria facial.
- Registro de ponto com biometria e geolocalização.
- Revogação de consentimento.
- Solicitações LGPD.
- Exportação de dados do titular.
- Anonimização e pseudonimização.
- Retenção e descarte.
- Inventário de tratamento.
- RIPD para biometria/geolocalização.
- Incidentes de segurança.
- Segurança de sessão e cookies.
- Evidências técnicas e logs de execução.
- Testes automatizados de conformidade.

### 2.2 Fora do escopo técnico imediato

- Emissão de parecer jurídico definitivo.
- Definição final de base legal sem validação com contador/jurídico/DPO.
- Comunicação real à ANPD, salvo implementação de fluxo e evidência.
- Contratos comerciais com operadores terceiros.
- Registro público de política jurídica fora do sistema.

---

## 3. Diagnóstico resumido

A branch `feature/lgpd-compliance` já contém uma base relevante:

- Consentimento biométrico com termo versionado.
- Revogação biométrica.
- Solicitações LGPD.
- Painel administrativo LGPD.
- SLA inicial.
- Inventário de tratamento.
- Retenção com executor e processadores.
- Anonimização com executor e processadores.
- Logs de retenção e anonimização.
- Cookies HTTP-only.
- CSRF.
- Métricas e observabilidade.

Porém, ainda existem pontos que exigem correção/validação antes de produção:

| Risco | Severidade | Descrição |
|---|---:|---|
| Cadastro biométrico por gestor sem aceite prévio do titular | P0 | `createEmployee` e `updateEmployee` ainda podem processar `faceImageBase64` sem comprovação de consentimento ativo do colaborador. |
| Possível vazamento multi-tenant em listagem/admin LGPD | P0 | Manager não pode listar solicitações de outras empresas, mesmo que omita `companyId` ou force parâmetro externo. |
| Exportação de audit logs com identificador incorreto | P0 | Exportação deve usar `userId`, não `employeeId`, para buscar logs de usuário. |
| Anonimização precisa de prova de execução por domínio | P0 | Existem processadores, mas é necessário garantir integração, resultado, rollback lógico e testes por domínio. |
| Retenção precisa de validação real por domínio | P0 | Existem executor/processadores, mas é necessário provar `DRY_RUN` e `APPLY`, com logs, escopo e preservação legal. |
| Inventário front/back precisa validação contratual | P1 | Rotas existem, mas contrato, payloads, permissões e atualização por UUID/processCode precisam ser alinhados. |
| Liveness deve ser obrigatório em produção | P1 | O default técnico não pode permitir produção sem vivacidade. |
| RIPD e documentação de alto risco | P1 | Necessário para biometria, geolocalização e controle de jornada. |
| Incidentes precisam workflow completo | P1 | Deve haver avaliação de risco, decisão de comunicação, prazo e evidências. |

---

## 4. Princípios técnicos obrigatórios

### 4.1 Privacy by design

Toda nova funcionalidade que trate dados pessoais deve declarar:

- dado tratado;
- finalidade;
- base legal;
- titular;
- origem;
- retenção;
- compartilhamento;
- medida de segurança;
- forma de exclusão, anonimização ou preservação.

### 4.2 Least privilege

Nenhum usuário pode acessar dados fora do seu papel:

- `PARTNER`: apenas os próprios dados.
- `MANAGER`: apenas dados da empresa vinculada.
- `CTO`: acesso administrativo global quando estritamente necessário.

### 4.3 Biometria como dado sensível

Biometria facial deve ter fluxo próprio, com consentimento ou base legal formalmente documentada, evidência, revogação e alternativa operacional.

### 4.4 Evidência técnica

Toda ação sensível deve gerar evidência:

- criação de solicitação LGPD;
- exportação de dados;
- revogação biométrica;
- anonimização;
- retenção;
- incidente;
- alteração de status;
- rejeição de solicitação;
- comunicação ao titular.

---

# 5. Backlog por Sprints

---

# Sprint 0 — Baseline, congelamento e contrato técnico

## Objetivo

Estabilizar a base antes das correções, garantindo que back-end e front-end estejam sincronizados, testáveis e com contrato mínimo documentado.

## Resultado esperado

Uma baseline confiável da branch `feature/lgpd-compliance`, com CI executando, contratos conhecidos e riscos mapeados.

---

## LGPD-S00-01 — Criar baseline técnica da branch

**Prioridade:** P0  
**Tipo:** Infra / Gestão técnica  
**Backend:** Sim  
**Frontend:** Sim

### Descrição

Criar um ponto de controle da branch `feature/lgpd-compliance`, registrando commit SHA do back-end e front-end, resultado de testes e pendências conhecidas.

### Tarefas técnicas

- Registrar SHA atual do back-end.
- Registrar SHA atual do front-end.
- Rodar build do back-end.
- Rodar testes do back-end.
- Rodar lint/test/build do front-end.
- Registrar falhas abertas.
- Criar arquivo `docs/legal/lgpd-baseline-validation.md`.

### Critérios de aceite

- Existe documento de baseline com SHA das duas branches.
- Build do back-end foi executado.
- Build do front-end foi executado.
- Falhas conhecidas estão listadas.
- Nenhuma correção P0 começa sem baseline registrada.

### Testes

```bash
./gradlew clean test
npm ci
npm run lint
npm run test
npm run build
```

---

## LGPD-S00-02 — Consolidar contrato OpenAPI LGPD

**Prioridade:** P0  
**Tipo:** Contrato API  
**Backend:** Sim  
**Frontend:** Sim

### Descrição

Gerar ou atualizar o contrato OpenAPI dos endpoints LGPD, incluindo solicitações, exportação, anonimização, inventário, incidentes e termos.

### Endpoints mínimos

```text
GET    /lgpd/requests
POST   /lgpd/requests
GET    /lgpd/requests/{requestId}
GET    /lgpd/requests/{requestId}/history
GET    /lgpd/employees/{employeeId}/export
POST   /lgpd/employees/{employeeId}/anonymize

GET    /lgpd/admin/requests
GET    /lgpd/admin/requests/{requestId}
PATCH  /lgpd/admin/requests/{requestId}/assign
POST   /lgpd/admin/requests/{requestId}/notes
POST   /lgpd/admin/requests/{requestId}/complete
POST   /lgpd/admin/requests/{requestId}/reject

GET    /api/lgpd/inventory
GET    /api/lgpd/inventory/active
GET    /api/lgpd/inventory/{processCode}
POST   /api/lgpd/inventory
PATCH  /api/lgpd/inventory/{inventoryId}

GET    /terms/status
GET    /terms/biometric/current
POST   /terms/accept-biometric
DELETE /terms/revoke-biometric
```

### Critérios de aceite

- Front-end não usa rota inexistente.
- Back-end documenta payloads de entrada e saída.
- Contrato distingue rotas `/lgpd/*` e `/api/lgpd/*`, ou padroniza ambas.
- Testes de contrato falham se rota usada no front não existir no back.

---

# Sprint 1 — Biometria e consentimento do titular

## Objetivo

Eliminar o risco de cadastro/uso de biometria sem consentimento válido do titular.

---

## LGPD-S01-01 — Bloquear cadastro biométrico por gestor sem consentimento

**Prioridade:** P0  
**Tipo:** Correção legal/técnica  
**Backend:** Sim  
**Frontend:** Sim

### Problema

O gestor pode criar ou atualizar colaborador enviando `faceImageBase64`, fazendo upload/indexação biométrica sem comprovação de consentimento ativo do titular.

### Regra de negócio

O gestor pode cadastrar dados administrativos do colaborador, mas não pode cadastrar biometria facial em nome do titular sem fluxo formal de aceite.

### Backend — tarefas

- Remover o processamento automático de `faceImageBase64` em `createEmployee`.
- Remover o processamento automático de `faceImageBase64` em `updateEmployee`.
- Rejeitar payload com `faceImageBase64` nesses endpoints, ou ignorar com erro de validação explícito.
- Criar exceção padronizada:

```json
{
  "code": "BIOMETRIC_ENROLLMENT_REQUIRES_DATA_SUBJECT_ACTION",
  "message": "A biometria deve ser cadastrada pelo próprio titular após aceite do termo."
}
```

- Criar endpoint próprio para cadastro biométrico pelo titular autenticado:

```text
POST /employee/me/biometric-enrollment
```

- O endpoint deve exigir:
  - usuário autenticado;
  - consentimento biométrico ativo;
  - liveness quando ambiente for produção;
  - rate limit;
  - auditoria;
  - substituição segura da face anterior.

### Frontend — tarefas

- Remover upload/captura facial da tela de criação/edição de colaborador feita por gestor.
- Exibir mensagem: “A biometria deve ser cadastrada pelo próprio colaborador no primeiro uso do recurso biométrico.”
- Criar tela/modal “Cadastrar minha biometria”.
- Proteger o cadastro com `BiometricConsentGuard`.

### Critérios de aceite

- Manager não consegue cadastrar face de colaborador em `createEmployee`.
- Manager não consegue alterar face de colaborador em `updateEmployee`.
- Colaborador consegue cadastrar a própria face após aceitar termo.
- Revogação remove a face e exige novo aceite/cadastro para uso biométrico.
- Toda tentativa bloqueada gera log sem armazenar a imagem.

### Testes obrigatórios

- Unit test: `createEmployee` com `faceImageBase64` retorna erro.
- Unit test: `updateEmployee` com `faceImageBase64` retorna erro.
- Integration test: endpoint próprio de biometria exige consentimento.
- E2E: gestor cria colaborador sem campo biométrico.
- E2E: colaborador aceita termo e cadastra biometria.

---

## LGPD-S01-02 — Formalizar fluxo de consentimento biométrico

**Prioridade:** P0  
**Tipo:** Produto / Legal Tech  
**Backend:** Sim  
**Frontend:** Sim

### Descrição

Garantir que o aceite biométrico seja específico, destacado, versionado, auditável e revogável.

### Tarefas

- Confirmar que `LegalText` possui:
  - versão;
  - hash;
  - conteúdo;
  - tipo;
  - data de ativação;
  - status ativo.
- Garantir que `LegalConsent` registre:
  - versão;
  - IP;
  - user-agent;
  - data;
  - hash da evidência;
  - documento de evidência;
  - finalidade;
  - base legal.
- Exibir claramente no front:
  - finalidade;
  - quais dados são coletados;
  - que a biometria é sensível;
  - como revogar;
  - consequência da recusa/revogação.
- Revisar texto para remover ambiguidade entre:
  - consentimento;
  - obrigação legal;
  - execução de contrato;
  - controle de jornada.

### Critérios de aceite

- O termo não usa texto genérico.
- O aceite é específico para biometria.
- A recusa não bloqueia a plataforma inteira.
- A revogação não bloqueia login por senha.
- O termo exibido no front corresponde ao hash enviado ao back.

---

## LGPD-S01-03 — Ativar liveness obrigatório em produção

**Prioridade:** P1  
**Tipo:** Segurança biométrica  
**Backend:** Sim  
**DevOps:** Sim

### Tarefas

- Definir `app.biometric.liveness-required=true` no profile `prod`.
- Adicionar `ProductionConfigValidator` impedindo boot em produção com liveness desligado.
- Criar teste de contexto de produção.
- Registrar métrica de falha de liveness.
- Criar alerta operacional para picos de falha.

### Critérios de aceite

- Aplicação não sobe em `prod` se liveness estiver `false`.
- Login facial e ponto biométrico exigem liveness.
- Ambiente local pode manter configuração flexível.

---

# Sprint 2 — Isolamento multi-tenant e autorização LGPD

## Objetivo

Eliminar risco de exposição de dados entre empresas diferentes.

---

## LGPD-S02-01 — Corrigir listagem administrativa LGPD para Manager

**Prioridade:** P0  
**Tipo:** Segurança / Autorização  
**Backend:** Sim  
**Frontend:** Sim

### Problema

Manager não pode listar solicitações LGPD de outras empresas, com ou sem filtro `companyId`.

### Backend — regra

```text
CTO:
  pode listar todas as empresas
  pode filtrar por companyId

MANAGER:
  sempre limitado à empresa do próprio manager
  ignora companyId externo ou rejeita se companyId != empresa do manager

PARTNER:
  não acessa endpoints admin
```

### Tarefas

- Alterar `LgpdService.listAdminRequests`.
- Usar `DomainAuthorizationService.authorizeCompanyAccess`.
- Se role `MANAGER`:
  - obter `companyId` do colaborador autenticado;
  - aplicar filtro obrigatório;
  - rejeitar companyId divergente.
- Adicionar logs de tentativa de acesso indevido.
- Criar teste de segurança multi-tenant.

### Critérios de aceite

- Manager da empresa A não lista solicitações da empresa B.
- Manager da empresa A não acessa detalhe de solicitação da empresa B.
- Manager da empresa A não atribui/conclui/rejeita solicitação da empresa B.
- CTO mantém acesso global.
- Testes automatizados cobrem os cenários.

---

## LGPD-S02-02 — Endurecer autorização em detalhes e ações administrativas

**Prioridade:** P0  
**Tipo:** Segurança  
**Backend:** Sim

### Tarefas

Validar tenant em:

```text
GET   /lgpd/admin/requests/{requestId}
PATCH /lgpd/admin/requests/{requestId}/assign
POST  /lgpd/admin/requests/{requestId}/notes
POST  /lgpd/admin/requests/{requestId}/complete
POST  /lgpd/admin/requests/{requestId}/reject
```

### Critérios de aceite

- Toda ação administrativa passa por autorização de empresa.
- Falha retorna 403 ou 404 sem vazar existência do recurso.
- Auditoria registra tentativa negada.

---

# Sprint 3 — Exportação de dados do titular

## Objetivo

Garantir que a exportação seja completa, correta, segura e compreensível.

---

## LGPD-S03-01 — Corrigir exportação de audit logs

**Prioridade:** P0  
**Tipo:** Bug  
**Backend:** Sim

### Problema

A exportação deve buscar logs por `userId`, não por `employeeId`.

### Tarefas

- Alterar `LgpdService.exportEmployeeData`.
- Obter usuário por `employeeId`.
- Buscar logs por `user.userId()`.
- Se não houver usuário, retornar lista vazia e registrar observação no manifesto.
- Criar teste unitário.

### Critérios de aceite

- Exportação de colaborador com usuário inclui audit logs corretos.
- Exportação de colaborador sem usuário não falha.
- Nenhuma busca usa `employeeId` como `userId`.

---

## LGPD-S03-02 — Criar manifesto da exportação

**Prioridade:** P1  
**Tipo:** Transparência  
**Backend:** Sim  
**Frontend:** Sim

### Descrição

Toda exportação deve conter metadados explicando o que foi exportado.

### Campos mínimos

```json
{
  "exportId": "uuid",
  "exportedAt": "instant",
  "requestedByUserId": "uuid",
  "targetEmployeeId": "uuid",
  "includePreciseGeolocation": false,
  "sections": [
    "employee",
    "user",
    "company",
    "documents",
    "timeRecords",
    "messages",
    "auditLogs",
    "legalConsents"
  ],
  "warnings": [
    "Este arquivo contém dados pessoais e pode conter dados sensíveis."
  ]
}
```

### Critérios de aceite

- Exportação inclui manifesto.
- Front exibe aviso antes do download.
- Arquivo tem nome padronizado.
- Exportação gera auditoria.

---

## LGPD-S03-03 — Criar escopo de exportação por perfil

**Prioridade:** P1  
**Tipo:** Segurança  
**Backend:** Sim

### Regras

| Perfil | Pode exportar |
|---|---|
| PARTNER | próprios dados |
| MANAGER | colaboradores da própria empresa, com justificativa |
| CTO | qualquer colaborador, com auditoria alta |

### Critérios de aceite

- Manager não exporta dados de outra empresa.
- Exportação de terceiros exige motivo.
- Exportação do próprio titular continua simples.

---

# Sprint 4 — Anonimização e eliminação por domínio

## Objetivo

Transformar a anonimização em processo completo, auditável e seguro por domínio de dados.

---

## LGPD-S04-01 — Integrar `EmployeeAnonymizationService` ao `AnonymizationPlanExecutor`

**Prioridade:** P0  
**Tipo:** Arquitetura / Domínio  
**Backend:** Sim

### Problema

Existem processadores de anonimização, mas a operação de anonimização precisa garantir execução centralizada, consistente e logada por domínio.

### Tarefas

- Criar `AnonymizationPlan` a partir do pedido LGPD.
- Definir flags:
  - `preserveLaborData`;
  - `preserveFiscalData`;
  - `deleteBiometricArtifacts`;
  - `anonymizeDocuments`;
  - `anonymizeMessages`;
  - `anonymizeAuditLogs`.
- Executar `AnonymizationPlanExecutor`.
- Consolidar resultado em `AnonymizationExecutionLog`.
- Retornar resumo da execução para admin.

### Critérios de aceite

- Anonimização roda por plano.
- Cada domínio gera log próprio.
- Falha parcial não fica invisível.
- Admin consegue ver resultado.
- Dados trabalhistas/fiscais são preservados conforme regra.

---

## LGPD-S04-02 — Implementar modo DRY_RUN para anonimização

**Prioridade:** P0  
**Tipo:** Segurança operacional  
**Backend:** Sim  
**Frontend:** Sim

### Tarefas

- Criar endpoint:

```text
POST /lgpd/employees/{employeeId}/anonymize/dry-run
```

- Retornar:
  - quantidade de documentos;
  - registros de ponto;
  - mensagens;
  - logs;
  - artefatos biométricos;
  - dados preservados por obrigação legal;
  - riscos.
- Exibir prévia no front antes de confirmar.

### Critérios de aceite

- Admin vê impacto antes de anonimizar.
- Dry-run não altera dados.
- Apply só ocorre após confirmação explícita.

---

## LGPD-S04-03 — Estratégia por domínio de anonimização

**Prioridade:** P0  
**Tipo:** Domínio / Segurança  
**Backend:** Sim

### Regras por domínio

| Domínio | Estratégia |
|---|---|
| Employee | anonimizar nome, CPF, PIS, e-mail, telefone, endereço |
| User | desativar e anonimizar username se necessário |
| Biometria | excluir S3 e Rekognition |
| Documents | anonimizar metadados ou excluir arquivo quando permitido |
| TimeRecord | preservar se trabalhista/fiscal, pseudonimizar identificadores quando possível |
| Messages | anonimizar remetente/destinatário e conteúdo pessoal |
| AuditLog | sanitizar detalhes pessoais, preservar evento mínimo |
| LGPD Request | preservar evidência mínima de atendimento |

### Critérios de aceite

- Cada domínio tem teste próprio.
- Nenhum domínio é ignorado silenciosamente.
- Dados preservados possuem motivo técnico/legal registrado.

---

# Sprint 5 — Retenção e descarte real

## Objetivo

Garantir que a retenção execute ações reais, controladas, reversíveis operacionalmente via backup e auditáveis.

---

## LGPD-S05-01 — Validar processadores de retenção por domínio

**Prioridade:** P0  
**Tipo:** Retenção  
**Backend:** Sim

### Domínios mínimos

```text
TOKEN
PASSWORD_RESET_TOKEN
MESSAGE
DOCUMENT
AUDIT_LOG
LGPD_REQUEST
BIOMETRIC_ARTIFACT
```

### Tarefas

- Validar processador existente para cada `RetentionResourceType`.
- Adicionar processadores ausentes.
- Garantir que `resourceType` inválido falha explicitamente.
- Criar fixtures de banco para cada domínio.
- Registrar `RetentionExecutionLog`.

### Critérios de aceite

- Cada política executa o processador correto.
- `DRY_RUN` não altera dados.
- `APPLY` altera dados conforme política.
- Logs mostram scanned/affected/skipped/errors.

---

## LGPD-S05-02 — Definir políticas padrão de retenção

**Prioridade:** P0  
**Tipo:** Produto / Legal Tech  
**Backend:** Sim  
**Docs:** Sim

### Políticas sugeridas

| Código | Recurso | Ação | Retenção | Observação |
|---|---|---|---:|---|
| `RET_PASSWORD_TOKEN` | password reset token | delete | 1 dia | token expirado |
| `RET_BLACKLIST_TOKEN` | blacklisted token | delete | após expiração | segurança |
| `RET_MESSAGES` | messages | anonymize/delete | 180 dias | conforme política da empresa |
| `RET_DOCUMENTS_COMMON` | documents | delete/anonymize | configurável | exceto trabalhista/fiscal |
| `RET_AUDIT_LOGS` | audit logs | anonymize | 365 dias | preservar evento mínimo |
| `RET_BIOMETRIC_REVOKED` | biometric artifacts | delete | imediato | após revogação |
| `RET_LGPD_REQUESTS` | lgpd requests | preserve minimal | 5 anos sugerido | evidência de atendimento |

### Critérios de aceite

- Políticas ficam versionadas em migration ou seed controlado.
- Produção inicia com `DRY_RUN`.
- Mudança para `APPLY` exige aprovação explícita.

---

## LGPD-S05-03 — Criar dashboard de retenção

**Prioridade:** P1  
**Tipo:** Observabilidade  
**Backend:** Sim  
**Frontend:** Opcional

### Métricas

- execuções por política;
- registros escaneados;
- registros afetados;
- falhas;
- tempo de execução;
- última execução;
- modo `DRY_RUN`/`APPLY`.

### Critérios de aceite

- Admin técnico consegue ver se retenção está funcionando.
- Falhas geram alerta.

---

# Sprint 6 — Inventário de tratamento e RIPD

## Objetivo

Criar documentação técnica-operacional que sustente finalidade, base legal, retenção e risco.

---

## LGPD-S06-01 — Validar contrato do inventário LGPD

**Prioridade:** P1  
**Tipo:** Contrato API  
**Backend:** Sim  
**Frontend:** Sim

### Problema

O back-end expõe inventário em `/api/lgpd/inventory`, enquanto o front define paths relativos como `inventory`, `inventory/active` e `inventory/{processCode}`. É necessário garantir que o `apiBaseUrl` normalize corretamente o prefixo `/api`.

### Tarefas

- Validar se front chama URL correta em produção.
- Criar teste de contrato para:
  - listagem;
  - listagem ativa;
  - busca por processCode;
  - criação;
  - atualização.
- Alinhar update:
  - front usa `processCode`;
  - back usa `inventoryId` no `PATCH`.
- Definir padrão único:
  - `PATCH /api/lgpd/inventory/{inventoryId}` ou
  - `PUT /api/lgpd/inventory/{processCode}`.

### Critérios de aceite

- Front não chama endpoint inexistente.
- Criação e edição funcionam em ambiente integrado.
- Contrato fica documentado.

---

## LGPD-S06-02 — Completar campos obrigatórios do inventário

**Prioridade:** P1  
**Tipo:** Governança de dados  
**Backend:** Sim  
**Frontend:** Sim

### Campos mínimos por processo

```text
processCode
processName
description
dataSubjects
personalDataCategories
sensitiveDataCategories
processingPurpose
legalBasis
retentionPolicyCode
sharingWithThirdParties
operators
internationalTransfer
securityMeasures
riskLevel
ripdRequired
active
version
createdAt
updatedAt
```

### Critérios de aceite

- Todo processo crítico do Kronos está inventariado.
- Biometria e geolocalização aparecem como alto risco.
- Dados de jornada e documentos aparecem com retenção própria.
- Existe versão do inventário.

---

## LGPD-S06-03 — Criar RIPD biometria + geolocalização + jornada

**Prioridade:** P1  
**Tipo:** Documentação / Governança  
**Docs:** Sim  
**Backend:** Opcional  
**Frontend:** Opcional

### Documento alvo

```text
docs/legal/RIPD-biometria-geolocalizacao-jornada.md
```

### Conteúdo mínimo

- contexto do tratamento;
- descrição dos dados pessoais;
- dados sensíveis;
- titulares;
- finalidade;
- base legal;
- fluxo de coleta;
- armazenamento;
- operadores terceiros;
- riscos;
- probabilidade;
- impacto;
- medidas de mitigação;
- retenção;
- descarte;
- aprovação interna;
- revisão periódica.

### Critérios de aceite

- RIPD cobre biometria, geolocalização e jornada.
- Documento possui versão e data.
- Documento possui responsável por aprovação.
- Riscos têm medidas associadas.

---

# Sprint 7 — Solicitações LGPD e atendimento operacional

## Objetivo

Completar o ciclo operacional das solicitações dos titulares.

---

## LGPD-S07-01 — Melhorar workflow de solicitações LGPD

**Prioridade:** P1  
**Tipo:** Produto  
**Backend:** Sim  
**Frontend:** Sim

### Status esperados

```text
OPEN
IN_ANALYSIS
WAITING_CONTROLLER
WAITING_LEGAL_REVIEW
WAITING_DATA_SUBJECT
COMPLETED
REJECTED
PARTIALLY_COMPLETED
CANCELLED
```

### Tarefas

- Adicionar `WAITING_DATA_SUBJECT` se necessário.
- Permitir pedido de complemento ao titular.
- Registrar notas públicas e internas separadamente.
- Exibir ao titular apenas notas públicas.
- Exigir razão de rejeição.

### Critérios de aceite

- Titular vê andamento claro.
- Admin vê histórico completo.
- Rejeição sem motivo é bloqueada.
- Conclusão sem nota pública é bloqueada.

---

## LGPD-S07-02 — Notificação de mudança de status

**Prioridade:** P1  
**Tipo:** Comunicação  
**Backend:** Sim  
**Frontend:** Opcional

### Tarefas

- Enviar e-mail ou aviso interno quando:
  - solicitação é criada;
  - responsável é atribuído;
  - status muda;
  - pedido é concluído;
  - pedido é rejeitado;
  - SLA está próximo de vencer.
- Registrar notificação enviada.

### Critérios de aceite

- Titular recebe informação de mudança relevante.
- Falha de envio não quebra a transação principal.
- Existe retry ou log de falha.

---

# Sprint 8 — Incidentes de segurança

## Objetivo

Transformar o cadastro de incidente em workflow de resposta.

---

## LGPD-S08-01 — Implementar avaliação de risco de incidente

**Prioridade:** P1  
**Tipo:** Segurança / LGPD  
**Backend:** Sim  
**Frontend:** Sim

### Campos adicionais

```text
incidentConfirmed
personalDataInvolved
sensitiveDataInvolved
affectedSubjectsEstimate
dataCategories
incidentCause
confidentialityImpact
integrityImpact
availabilityImpact
riskToSubjects
communicationRequired
anpdCommunicationDeadline
subjectsCommunicationDeadline
containmentActions
correctiveActions
evidenceLinks
```

### Critérios de aceite

- Incidente confirmado calcula prazo interno.
- Sistema registra decisão de comunicar ou não comunicar.
- Se comunicação for necessária, prazo fica visível.
- Encerramento exige medidas corretivas.

---

## LGPD-S08-02 — Criar relatório de incidente

**Prioridade:** P1  
**Tipo:** Evidência  
**Backend:** Sim  
**Frontend:** Sim

### Saída

- PDF ou JSON exportável com:
  - dados do incidente;
  - avaliação de risco;
  - titulares afetados;
  - dados afetados;
  - medidas tomadas;
  - comunicação ANPD/titulares;
  - evidências.

### Critérios de aceite

- Relatório pode ser gerado por CTO.
- Relatório não expõe segredo técnico desnecessário.
- Relatório fica vinculado ao incidente.

---

# Sprint 9 — Segurança, sessão e hardening

## Objetivo

Fortalecer controles técnicos de proteção de dados.

---

## LGPD-S09-01 — Revisar cookies, CSRF e SameSite por ambiente

**Prioridade:** P1  
**Tipo:** Segurança  
**Backend:** Sim  
**DevOps:** Sim

### Tarefas

- Garantir `HttpOnly=true` no access token.
- Garantir `Secure=true` em produção.
- Definir `SameSite` por cenário:
  - mesmo domínio: `Lax`;
  - cross-site necessário: `None` + `Secure`.
- Tornar CSRF cookie configurável.
- Validar CORS por origem explícita.

### Critérios de aceite

- Produção não aceita origem curinga.
- Cookies são seguros.
- CSRF funciona com front real.
- Reset password não sofre redirecionamento indevido por sessão expirada.

---

## LGPD-S09-02 — Sanitizar logs e erros

**Prioridade:** P1  
**Tipo:** Segurança  
**Backend:** Sim

### Dados proibidos em logs

```text
CPF completo
PIS completo
token JWT
password
reset token
faceImageBase64
latitude/longitude precisa
document payload
conteúdo de documento
```

### Critérios de aceite

- Testes garantem mascaramento.
- Exceptions não retornam stack trace em produção.
- Logs de auditoria preservam evento, não payload sensível.

---

# Sprint 10 — Transparência e experiência do titular

## Objetivo

Melhorar a clareza para o titular sobre tratamento de dados, exportação, revogação e direitos.

---

## LGPD-S10-01 — Melhorar Privacy Center

**Prioridade:** P1  
**Tipo:** Frontend / Produto  
**Frontend:** Sim

### Seções

- Meus dados.
- Exportar dados.
- Solicitações LGPD.
- Consentimento biométrico.
- Revogação.
- Política de privacidade.
- Contato do encarregado/DPO.
- Histórico de termos aceitos.

### Critérios de aceite

- Titular encontra todos os direitos em uma tela.
- Exportação exibe aviso de sensibilidade.
- Revogação explica consequência sem coerção.
- Política de privacidade é acessível.

---

## LGPD-S10-02 — Histórico de consentimentos no front

**Prioridade:** P1  
**Tipo:** Transparência  
**Backend:** Sim  
**Frontend:** Sim

### Tarefas

- Criar endpoint:

```text
GET /terms/consents/history
```

- Exibir:
  - tipo;
  - versão;
  - data de aceite;
  - data de revogação;
  - status;
  - documento de evidência quando permitido.

### Critérios de aceite

- Titular visualiza histórico de consentimentos.
- Documento de evidência não expõe dados indevidos a terceiros.

---

# Sprint 11 — Testes automatizados de conformidade

## Objetivo

Criar uma suíte que impeça regressões de LGPD.

---

## LGPD-S11-01 — Testes de biometria

**Prioridade:** P0  
**Tipo:** Testes  
**Backend:** Sim  
**Frontend:** Sim

### Cenários

- Manager não cadastra biometria.
- Titular aceita termo e cadastra biometria.
- Titular revoga biometria.
- Ponto biométrico sem consentimento falha.
- Ponto biométrico com consentimento passa.
- Liveness obrigatório em produção.

---

## LGPD-S11-02 — Testes multi-tenant

**Prioridade:** P0  
**Tipo:** Testes  
**Backend:** Sim

### Cenários

- Manager A não lista solicitações da empresa B.
- Manager A não acessa detalhes da empresa B.
- Manager A não exporta colaborador da empresa B.
- Manager A não anonimiza colaborador da empresa B.
- CTO acessa conforme permissão.

---

## LGPD-S11-03 — Testes de retenção e anonimização

**Prioridade:** P0  
**Tipo:** Testes  
**Backend:** Sim

### Cenários

- Retenção `DRY_RUN` não altera banco.
- Retenção `APPLY` altera apenas registros elegíveis.
- Dados trabalhistas/fiscais são preservados.
- Anonimização remove biometria.
- Anonimização preserva evidência mínima.
- Logs de execução são criados.

---

# Sprint 12 — Readiness de produção e auditoria

## Objetivo

Consolidar evidências e decidir se a branch pode ir para produção.

---

## LGPD-S12-01 — Criar checklist final de produção LGPD

**Prioridade:** P0  
**Tipo:** Release  
**Backend:** Sim  
**Frontend:** Sim  
**DevOps:** Sim  
**Docs:** Sim

### Checklist

- [ ] Build back-end aprovado.
- [ ] Build front-end aprovado.
- [ ] Testes P0 aprovados.
- [ ] Testes multi-tenant aprovados.
- [ ] Liveness obrigatório em produção.
- [ ] CORS revisado.
- [ ] Cookies revisados.
- [ ] CSRF validado.
- [ ] Retenção em `DRY_RUN` executada e validada.
- [ ] Plano de ativação de `APPLY` aprovado.
- [ ] RIPD criado.
- [ ] Inventário criado.
- [ ] Política de privacidade publicada.
- [ ] Canal de encarregado/DPO definido.
- [ ] Fluxo de incidente documentado.
- [ ] Rollback documentado.

---

## LGPD-S12-02 — Pacote de evidências

**Prioridade:** P1  
**Tipo:** Auditoria  
**Docs:** Sim

### Gerar pasta

```text
docs/legal/evidence/
```

### Conteúdo

```text
01-baseline.md
02-api-contract.md
03-test-results.md
04-retention-dry-run.md
05-anonymization-dry-run.md
06-ripd.md
07-inventory-export.md
08-security-incident-flow.md
09-cookie-csrf-review.md
10-release-approval.md
```

### Critérios de aceite

- Evidências estão versionadas.
- Cada evidência possui data e responsável.
- Release só é aprovado com evidências P0 concluídas.

---

# 6. Ordem recomendada de execução

## Prioridade absoluta

1. `LGPD-S01-01` — bloquear biometria por gestor.
2. `LGPD-S02-01` — corrigir multi-tenant em admin LGPD.
3. `LGPD-S03-01` — corrigir audit logs da exportação.
4. `LGPD-S04-01` — integrar anonimização por plano.
5. `LGPD-S05-01` — validar retenção real.
6. `LGPD-S11-01` e `LGPD-S11-02` — testes de regressão P0.

## Sequência ideal

```text
Sprint 0 -> Sprint 1 -> Sprint 2 -> Sprint 3 -> Sprint 4 -> Sprint 5 -> Sprint 11 -> Sprint 12
```

As sprints 6, 7, 8, 9 e 10 podem rodar em paralelo após correção dos P0.

---

# 7. Definition of Done geral

Uma tarefa deste backlog só pode ser considerada concluída se atender a todos os itens abaixo:

- Código implementado.
- Testes unitários criados/atualizados.
- Testes de integração criados/atualizados quando houver API.
- Testes E2E criados/atualizados quando houver fluxo de usuário.
- Logs sensíveis revisados.
- Autorização multi-tenant validada.
- Documentação atualizada.
- Critérios de aceite cumpridos.
- Evidência registrada em `docs/legal/evidence`, quando aplicável.
- Build local aprovado.
- CI aprovado.

---

# 8. Riscos remanescentes

| Risco | Mitigação |
|---|---|
| Base legal da biometria incorreta | Revisão jurídica/DPO antes de produção |
| Retenção apagar dados trabalhistas/fiscais | Começar em `DRY_RUN`, validar com jurídico/contábil |
| Exportação expor dados de terceiros | Escopo por perfil e sanitização |
| Manager acessar dados de outra empresa | Testes multi-tenant obrigatórios |
| Incidente sem comunicação no prazo | Workflow com prazo e alerta |
| Front e back divergirem nos contratos | Testes de contrato/OpenAPI |
| Logs conterem dados sensíveis | Testes de sanitização |

---

# 9. Critério de liberação para produção

A branch `feature/lgpd-compliance` só deve ser promovida para produção quando:

```text
P0 = 100% concluído
P1 crítico = concluído ou formalmente aceito como risco
Testes LGPD = verdes
RIPD = criado
Inventário = criado e validado
Retenção = validada em DRY_RUN
Plano APPLY = aprovado
Rollback = documentado
```

---

# 10. Conclusão técnica

A branch `feature/lgpd-compliance` está em um estágio avançado de adequação, mas ainda precisa de correções críticas para reduzir risco regulatório e operacional.

O maior bloqueador técnico é a biometria cadastrável por gestor sem consentimento formal do titular. O segundo maior bloqueador é a validação rigorosa de isolamento multi-tenant no módulo administrativo LGPD. Em seguida, devem ser fechadas exportação correta, anonimização por domínio e retenção com evidências.

Este backlog deve ser tratado como plano de hardening antes de produção.
