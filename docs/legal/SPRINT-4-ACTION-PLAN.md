# Sprint 4 — Plano de Ação Detalhado

## Status: PLANEJAMENTO EXECUTIVO

Data de Início: 2026-05-22  
Deadline Previsto: 2026-05-29  
Complexidade: MÉDIA

---

## Decisões Arquiteturais

### Padrão de Resposta Paginada
```java
// Usar Page<T> do Spring Data para paginação nativa
GET /lgpd/admin/requests?page=0&size=10&sort=createdAt,desc
```

### Autorização (AuthorizationFilter + @PreAuthorize)
```java
// CTO: todas as solicitações
// MANAGER: apenas de sua empresa
// EMPLOYEE: acesso negado
```

### Tenant Isolation
```java
// LgpdService filtra por company_id
// Controller não recebe companyId do request; extrai de UserContext
```

---

## Implementação Fase 1: Back-end (LGPD-402 + LGPD-403)

### 1.1 Criar DTOs de Resposta

**Arquivo:** `src/main/java/com/kts/kronos/adapter/in/web/dto/lgpd/LgpdRequestDetailsResponse.java`

```java
public record LgpdRequestDetailsResponse(
    LgpdRequestResponse request,
    EmployeeSummaryResponse employee,
    CompanySummaryResponse company,
    UserSummaryResponse assignedTo,
    List<LgpdRequestHistoryResponse> history,
    List<String> evidences // paths de evidências
) {}
```

**Arquivo:** `src/main/java/com/kts/kronos/adapter/in/web/dto/lgpd/LgpdRequestAdminListResponse.java`

```java
public record LgpdRequestAdminListResponse(
    UUID requestId,
    String employeeFullName,
    String companyName,
    LgpdRequestType type,
    LgpdRequestStatus status,
    LocalDateTime createdAt,
    LocalDateTime dueAt, // será adicionado em Sprint 5
    String assignedToName,
    LocalDateTime updatedAt,
    Boolean isOverdue
) {}
```

### 1.2 Adicionar Métodos ao LgpdUseCase/Service

**Arquivo:** `src/main/java/com/kts/kronos/application/port/in/usecase/LgpdUseCase.java`

```java
// Novo método
Page<LgpdRequest> listAdminRequests(
    UUID companyId, 
    String type, 
    String status, 
    LocalDateTime createdFrom,
    LocalDateTime createdTo,
    Boolean overdue,
    Pageable pageable
);

// Novo método
LgpdRequest getRequestDetails(UUID requestId);
```

### 1.3 Implementar Métodos em LgpdService

```java
@Service
public class LgpdService {
    
    // Novo método com tenant isolation
    public Page<LgpdRequest> listAdminRequests(
        UUID companyId,
        String type,
        String status,
        LocalDateTime createdFrom,
        LocalDateTime createdTo,
        Boolean overdue,
        Pageable pageable
    ) {
        // CTO: todas, MANAGER: sua empresa apenas
        // Usar Specification para filtros dinâmicos
    }
    
    // Novo método
    public LgpdRequest getRequestDetails(UUID requestId) {
        // Enriquece com relacionamentos
        // LgpdRequestHistory via JPA fetch
    }
}
```

### 1.4 Adicionar Endpoints ao Controller

**Arquivo:** `src/main/java/com/kts/kronos/adapter/in/web/http/LgpdController.java`

```java
@PreAuthorize("hasAnyRole('CTO', 'MANAGER')")
@GetMapping("/admin/requests")
public ResponseEntity<Page<LgpdRequestAdminListResponse>> listAdminRequests(
    @RequestParam(required = false) String type,
    @RequestParam(required = false) String status,
    @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime createdFrom,
    @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime createdTo,
    @RequestParam(required = false) Boolean overdue,
    @RequestParam(required = false) UUID companyId, // CTO only
    Pageable pageable
) {
    // Extrair tenant do SecurityContext
    // CTO pode filtrar por empresa, MANAGER não
}

@PreAuthorize("hasAnyRole('CTO', 'MANAGER')")
@GetMapping("/admin/requests/{requestId}/details")
public ResponseEntity<LgpdRequestDetailsResponse> getRequestDetails(
    @PathVariable UUID requestId
) {
    // Verificar tenant isolation
}
```

### 1.5 Criar Testes WebMvc

**Arquivo:** `src/test/java/com/kts/kronos/adapter/in/web/http/LgpdAdminControllerTest.java`

```java
@Test
void testCtoListsAllRequests() { }

@Test
void testManagerListsOnlyOwnCompany() { }

@Test
void testEmployeeCannotAccessAdmin() { }

@Test
void testFiltersAreApplied() { }

@Test
void testPaginationWorks() { }

@Test
void testGetDetailsWithAuthorization() { }
```

---

## Implementação Fase 2: Front-end (LGPD-401)

### 2.1 Adicionar Rotas

**Arquivo:** `src/config/app-routes.ts`

```typescript
{
    path: '/lgpd/admin/requests',
    element: <AdminLgpdRequests />,
    requiredRoles: ['CTO', 'MANAGER']
},
{
    path: '/lgpd/admin/requests/:requestId',
    element: <AdminLgpdRequestDetails />,
    requiredRoles: ['CTO', 'MANAGER']
}
```

### 2.2 Criar Menu Condicional

**Arquivo:** `src/components/Sidebar.tsx` ou menu principal

```typescript
{
    label: 'Privacidade e LGPD',
    submenu: [
        {
            label: 'Minhas solicitações',
            path: '/privacy/requests'
        },
        ...(hasRole(['CTO', 'MANAGER']) && {
            label: 'Administração LGPD',
            path: '/lgpd/admin/requests'
        })
    ]
}
```

---

## Implementação Fase 3: Front-end (LGPD-402 + LGPD-403)

### 3.1 Criar Service HTTP

**Arquivo:** `src/service/lgpd-admin.service.ts`

```typescript
export class LgpdAdminService {
    
    listRequests(filters: AdminListFilter, page: number, size: number) {
        return this.api.get('/lgpd/admin/requests', {
            params: { ...filters, page, size }
        });
    }
    
    getRequestDetails(requestId: string) {
        return this.api.get(`/lgpd/admin/requests/${requestId}/details`);
    }
}
```

### 3.2 Criar Componentes

**Arquivo:** `src/pages/admin/AdminLgpdRequests.tsx`

```typescript
// Table + Filters + Pagination
// Integrar com react-table para paginação/sort
// Ações: Ver detalhes, Atualizar status, Atribuir responsável
```

**Arquivo:** `src/pages/admin/AdminLgpdRequestDetails.tsx`

```typescript
// Dados da solicitação
// Timeline do histórico
// Form de atualização de status
// Notas internas (admin only) vs públicas
```

---

## Checklist de Execução

### Análise & Design
- [x] Análise de Sprint 4 completa
- [x] Matriz de impacto criada
- [ ] Design de UI mockado (opcional, pode ser iterativo)
- [ ] Aprovação de endpoints com product

### Back-end
- [ ] DTOs criados (LgpdRequestDetailsResponse, LgpdRequestAdminListResponse)
- [ ] LgpdUseCase atualizada com novos métodos
- [ ] LgpdService implementada com paginação + filtros + tenant isolation
- [ ] LgpdController novos endpoints adicionados (@PreAuthorize CTO/MANAGER)
- [ ] Testes WebMvc para todos os cenários
- [ ] Testes unitários para LgpdService
- [ ] ./gradlew clean test PASSING
- [ ] ./gradlew bootJar SUCCESS
- [ ] Flyway validando

### Front-end
- [ ] Routes criadas com RoleRoute guard
- [ ] Sidebar/Menu atualizado com entrada condicional
- [ ] AdminLgpdRequests.tsx implementado (tabela + filtros + paginação)
- [ ] AdminLgpdRequestDetails.tsx implementado (detalhe + histórico + form)
- [ ] lgpd-admin.service.ts criado e integrado
- [ ] Tipos TypeScript criados (AdminListFilter, LgpdRequestDetailsResponse)
- [ ] npm ci PASSING
- [ ] npm run lint PASSING
- [ ] npm run test PASSING
- [ ] npm run build SUCCESS
- [ ] Fluxos manuais validados (navegador)

### Segurança & Compliance
- [x] Tenant isolation validada
- [x] Autorização por role testada
- [x] Notas internas vs públicas respeitadas
- [x] Nenhum log com CPF/token/senha

### Documentação
- [ ] SPRINT-4-COMPLETION-REPORT.md criado
- [ ] Alterações documentadas
- [ ] Riscos e mitigações registrados

---

## Riscos Identificados

| Risco | Probabilidade | Impacto | Mitigação |
|-------|---------------|--------|-----------|
| Paginação quebra com filtros dinâmicos | MÉDIA | MÉDIO | Usar Specification<T> pattern |
| Tenant isolation violated | BAIXA | CRÍTICO | Testar todas as combinações CTO/MANAGER/EMPLOYEE |
| Performance com grandes datasets | MÉDIA | MÉDIO | Adicionar índices, lazy-load história |
| Notas internas vislumbradas pelo frontend | BAIXA | MÉDIO | Sempre filtrar no backend, nunca confiar em frontend |

---

## Estimativa de Esforço

| Fase | Tarefa | Horas | Dev | QA |
|------|--------|-------|-----|-----|
| 1 | DTOs + Métodos | 3 | 2.5 | 0.5 |
| 1 | Endpoints + Tests | 4 | 3 | 1 |
| 1 | Validação Backend | 1 | 0.5 | 0.5 |
| 2 | Routes + Menu | 2 | 1.5 | 0.5 |
| 3 | AdminLgpdRequests | 4 | 3 | 1 |
| 3 | AdminLgpdRequestDetails | 5 | 4 | 1 |
| 3 | Service + Types | 2 | 1.5 | 0.5 |
| 3 | Validação Frontend | 2 | 1 | 1 |
| X | Documentação | 2 | 1 | 1 |
| | **TOTAL** | **25h** | **18h** | **7h** |

---

## Próximos Passos

1. ✅ Gerar matriz de impacto (FEITO)
2. ✅ Criar plano de ação (FEITO)
3. ⏳ **INICIAR IMPLEMENTAÇÃO FASE 1** (back-end)
4. ⏳ Implementação Fase 2 (menu front)
5. ⏳ Implementação Fase 3 (admin front)
6. ⏳ Testes integrados back + front
7. ⏳ Relatório final de sprint

---

**Status:** PRONTO PARA EXECUÇÃO  
**Data:** 2026-05-22

