# Sprint 4 — Análise de Impacto e Planejamento

## Visão Geral

**Sprint:** 4 - Painel Administrativo LGPD  
**Objetivo:** Criar interface para CTO/MANAGER tratar solicitações LGPD com listagem, detalhes, filtros e atualização de status.

---

## Features e Escopo

### LGPD-401: Rotas e Menu Administrativo LGPD
- **Tipo:** Front-end
- **Prioridade:** P0
- **Trabalho:** UI/UX com menu condicional por role

### LGPD-402: Listagem Administrativa de Solicitações  
- **Tipo:** Back-end + Front-end
- **Prioridade:** P0
- **Trabalho:** Paginação, filtros avançados, autorização por tenant

### LGPD-403: Detalhe da Solicitação
- **Tipo:** Back-end + Front-end
- **Prioridade:** P0
- **Trabalho:** Endpoint enriched, histórico, form de atualização

---

## Matriz de Impacto

| Componente | Sistema | Impacto | Risco |
|-----------|---------|--------|-------|
| LgpdController | Back-end | **MÉDIO** - novo endpoint de detalhe | BAIXO - padrão estabelecido |
| LgpdService | Back-end | **ALTO** - novo método listByCompany com paginação | MÉDIO - tenant isolation |
| LgpdRequest (entity) | Back-end/DB | **BAIXO** - nenhuma alteração | BAIXO - modelo estável |
| AuthorizationFilter | Back-end | **MÉDIO** - validação de roles | MÉDIO - acesso administrativo |
| App.tsx | Front-end | **MÉDIO** - novo menu condicional | BAIXO - padrão estabelecido |
| AdminLgpdRequests | Front-end | **ALTO** - novo componente de tabela | MÉDIO - paginação/filtros |
| AdminLgpdRequestDetails | Front-end | **ALTO** - novo componente de detalhe | MÉDIO - timeline/form |
| api.ts | Front-end | **BAIXO** - novos endpoints | BAIXO - padrão estabelecido |

---

## Alterações Necessárias por Arquivo

### Back-end

#### Controllers
- **LgpdController.java**
  - [ ] Adicionar `listByCompany(@RequestParam companyId, @RequestParam status, ..., Pageable)` 
  - [ ] Adicionar `getRequestDetails(@PathVariable requestId)`
  - [ ] Filtros: companyId, employeeId, type, status, createdFrom, createdTo, dueBefore, overdue, page, size, sort

#### Services
- **LgpdService.java**
  - [ ] Novo método: `Page<LgpdRequestResponse> listByCompanyWithFilters(...)`
  - [ ] Novo método: `LgpdRequestDetailsResponse getRequestDetails(UUID requestId)`
  - [ ] Implementar tenant isolation: CTO vê todas, MANAGER vê sua empresa, EMPLOYEE vê próprias

#### Responses/DTOs
- **LgpdRequestDetailsResponse.java** (novo)
  ```java
  public record LgpdRequestDetailsResponse(
      LgpdRequestResponse request,
      EmployeeSummaryResponse employee,
      CompanySummaryResponse company,
      UserSummaryResponse assignedTo,
      List<LgpdRequestHistoryResponse> history,
      List<LgpdRequestEvidenceResponse> evidences
  ) {}
  ```

#### Testes
- **LgpdControllerTest.java**
  - [ ] testListByCompanyAsAdmin (CTO, MANAGER, EMPLOYEE)
  - [ ] testGetRequestDetailsWithAuthorization
  - [ ] testFiltersAreApplied
  - [ ] testTenantIsolation

- **LgpdServiceTest.java**
  - [ ] testListWithPagination
  - [ ] testListWithFilters
  - [ ] testTenantIsolationInList

### Front-end

#### Componentes Novos
- **src/pages/admin/AdminLgpdRequests.tsx** (novo)
  - [ ] Tabela com colunas: ID, Colaborador, Empresa, Tipo, Status, Data abertura, Prazo, Responsável, Última atualização
  - [ ] Filtros: Status, Tipo, Vencidas, Empresa (CTO), Colaborador, Período
  - [ ] Paginação
  - [ ] Ações: Ver detalhes, Alterar status, Atribuir responsável, Adicionar nota

- **src/pages/admin/AdminLgpdRequestDetails.tsx** (novo)
  - [ ] Seção de dados da solicitação
  - [ ] Timeline visual do histórico
  - [ ] Form de atualização: novo status, nota interna, nota pública, responsável
  - [ ] Exibição de evidências

#### Componentes Existentes
- **src/App.tsx**
  - [ ] Importar AdminLgpdRequests
  - [ ] Importar AdminLgpdRequestDetails
  - [ ] Registrar rotas /lgpd/admin/requests e /lgpd/admin/requests/:requestId

- **src/components/Sidebar.tsx** (ou menu principal)
  - [ ] Adicionar menu "Privacidade e LGPD" com submenu "Administração LGPD"
  - [ ] Condicionar visibilidade a RoleRoute (CTO, MANAGER)

- **src/config/app-routes.ts**
  - [ ] Adicionar rota admin para /lgpd/admin/requests
  - [ ] Adicionar rota admin para /lgpd/admin/requests/:requestId

#### Services
- **src/service/lgpd-admin.service.ts** (novo)
  - [ ] `listRequests(filters, page, size, sort)`
  - [ ] `getRequestDetails(requestId)`
  - [ ] `updateRequestStatus(requestId, newStatus, notes)`
  - [ ] `assignResponsible(requestId, userId)`
  - [ ] `addNote(requestId, note, isPublic)`

#### Types
- **src/types/lgpd.ts**
  - [ ] `LgpdAdminFilter`
  - [ ] `LgpdRequestDetailsResponse`
  - [ ] `LgpdRequestHistoryResponse`
  - [ ] `LgpdRequestEvidenceResponse`

#### API
- **src/api-routes.ts** (ou equivalente)
  - [ ] `GET /lgpd/requests (com params: companyId, status, type, page, size)`
  - [ ] `GET /lgpd/requests/{requestId}/details`

#### Testes
- **src/pages/admin/__tests__/AdminLgpdRequests.test.tsx** (novo)
  - [ ] testRenderTable
  - [ ] testFiltersWork
  - [ ] testPaginationWorks
  - [ ] testActionsAvailable

- **src/pages/admin/__tests__/AdminLgpdRequestDetails.test.tsx** (novo)
  - [ ] testRenderDetails
  - [ ] testTimelineVisible
  - [ ] testFormSubmit
  - [ ] testHistoryInCronologicalOrder

---

## Critérios de Aceite

### LGPD-401
- [x] CTO enxerga menu administrativo
- [x] MANAGER enxerga menu administrativo
- [x] EMPLOYEE não enxerga menu administrativo
- [x] Acesso direto por URL respeita RoleRoute

### LGPD-402
- [x] CTO lista todas as solicitações
- [x] MANAGER lista apenas solicitações da própria empresa
- [x] EMPLOYEE não acessa endpoint administrativo
- [x] Tabela possui paginação
- [x] Filtros funcionam

### LGPD-403
- [x] Detalhe exibe dados completos
- [x] Histórico é exibido em ordem cronológica
- [x] Acesso respeita tenant
- [x] Notas internas não aparecem para titular
- [x] Notas públicas aparecem para titular

---

## Sequência de Implementação

1. **Back-end (LGPD-402 + LGPD-403)**
   - Criar/ajustar LgpdController com novos endpoints
   - Implementar LgpdService com paginação e filtros
   - Criar DTOs para responses enriquecidas
   - Testes WebMvc e unitários

2. **Front-end (LGPD-401)**
   - Adicionar rotas em app-routes.ts
   - Criar menu condicional em Sidebar

3. **Front-end (LGPD-402)**
   - Criar AdminLgpdRequests.tsx com tabela e filtros
   - Criar lgpd-admin.service.ts
   - Integrar com API

4. **Front-end (LGPD-403)**
   - Criar AdminLgpdRequestDetails.tsx
   - Integrar histórico e form de atualização
   - Testes de paginação e filtros

---

## Considerações de Segurança

- ✅ Tenant isolation obrigatória: CTO > todas, MANAGER > sua empresa, EMPLOYEE > nada
- ✅ Validar roles (CTO, MANAGER) em autorização
- ✅ Não logar CPF completo, token, senha, base64 de face
- ✅ Notas internas visíveis apenas para staff
- ✅ Histórico respeitando visibilidade (publicOnly vs. adminFull)

---

## Artefatos Esperados

- 1 novo controller method com 2 endpoints
- 1 novo service method com 2 sub-métodos
- 2 novos DTOs (LgpdRequestDetailsResponse, filtros)
- 2 novos componentes React (AdminLgpdRequests, AdminLgpdRequestDetails)
- 1 novo service HTTP (lgpd-admin.service.ts)
- 1 novo tipo (LgpdAdminFilter, etc)
- ~15 testes (back) + ~8 testes (front)
- 0 novas migrations (reutiliza LgpdRequest existente)

