# Sprint 2 — Isolamento Multi-Tenant e Autorização LGPD
## Matriz de Impacto

**Data:** 22 de maio de 2026

---

## Objetivo da Sprint

Eliminar risco de exposição de dados entre empresas diferentes. Manager não pode listar/acessar/modificar solicitações LGPD de outras empresas.

---

## Features

### LGPD-S02-01: Corrigir listagem administrativa LGPD para Manager
**Prioridade:** P0  
**Tipo:** Segurança / Autorização

**Problema Identificado:**
- Método `listAdminRequests` não valida se Manager pode acessar o companyId solicitado
- Manager pode omitir companyId ou enviar um forçado e ver solicitações de outras empresas

**Solução:**
- Usar `authorizeCompanyAccess(companyId)` para validar tenant
- Se Manager: filtro obrigatório pela empresa do usuário autenticado
- CTO: mantém acesso global

**Impacto de Segurança:** ⚠️ CRÍTICO (P0)

---

### LGPD-S02-02: Endurecer autorização em detalhes e ações administrativas
**Prioridade:** P0  
**Tipo:** Segurança / Autorização

**Endpoints Afetados:**
- `GET /lgpd/admin/requests/{requestId}` (getRequestDetails)
- `PATCH /lgpd/admin/requests/{requestId}/assign` (assignRequest)
- `POST /lgpd/admin/requests/{requestId}/notes` (addNote)
- `POST /lgpd/admin/requests/{requestId}/complete` (completeRequest)
- `POST /lgpd/admin/requests/{requestId}/reject` (rejectRequest)

**Problema Identificado:**
- Todos chamam `findAuthorizedRequest()` que valida apenas employee access
- Não validam se Manager pode acessar aquele LgpdRequest pelo lado da empresa

**Solução:**
- Criar método `findAuthorizedAdminRequest()` ou melhorar `findAuthorizedRequest()`
- Adicionar validação de company após buscar a solicitação
- Retornar 403 Forbidden se acesso negado
- Logar tentativa de acesso indevido

**Impacto de Segurança:** ⚠️ CRÍTICO (P0)

---

## Arquitetura Existente

### DomainAuthorizationService
- ✅ Tem método `authorizeCompanyAccess(companyId)` 
- Valida se Manager pode acessar aquele companyId
- Lança `ForbiddenException` se não autorizado

### LgpdService
- Métodos a corrigir:
  - `listAdminRequests()` - sem validação de company
  - `getRequestDetails()` - usa `findAuthorizedRequest()` (employee-only)
  - `assignRequest()` - usa `findAuthorizedRequest()` (employee-only)
  - `addNote()` - usa `findAuthorizedRequest()` (employee-only)
  - `completeRequest()` - usa `findAuthorizedRequest()` (employee-only)
  - `rejectRequest()` - usa `findAuthorizedRequest()` (employee-only)

---

## Matriz de Alterações

| Arquivo | Método | Alteração | Complexidade | Risco |
|---------|--------|-----------|--------------|-------|
| LgpdService.java | listAdminRequests | Adicionar validação de company via authorizeCompanyAccess | Baixa | Baixo |
| LgpdService.java | getRequestDetails | Adicionar validação de company após findAuthorizedRequest | Média | Médio |
| LgpdService.java | assignRequest | Adicionar validação de company após findAuthorizedRequest | Média | Médio |
| LgpdService.java | addNote | Adicionar validação de company após findAuthorizedRequest | Média | Médio |
| LgpdService.java | completeRequest | Adicionar validação de company após findAuthorizedRequest | Média | Médio |
| LgpdService.java | rejectRequest | Adicionar validação de company após findAuthorizedRequest | Média | Médio |
| LgpdService.java | findAuthorizedRequest (novo) | Criar método helper para admin requests | Baixa | Baixo |
| LgpdServiceTest.java | - | Adicionar testes de segurança multi-tenant | Alta | Baixo |

---

## Validação de Segurança

### Antes da alteração (Vulnerável):
```
Manager A (empresa X) → GET /lgpd/admin/requests?companyId=Y
→ Resultado: Vê solicitações da empresa Y (SEGURANÇA BREACH!)
```

### Depois da alteração (Seguro):
```
Manager A (empresa X) → GET /lgpd/admin/requests?companyId=Y
→ authorizeCompanyAccess(Y) valida: X != Y
→ Resultado: 403 Forbidden (Seguro!)
```

---

## Testes Obrigatórios

### Testes de Segurança Multi-Tenant
- Manager A não lista solicitações da empresa B
- Manager A não acessa detalhe da empresa B (404 ou 403)
- Manager A não atribui solicitação da empresa B
- Manager A não adiciona nota a solicitação da empresa B
- Manager A não completa solicitação da empresa B
- Manager A não rejeita solicitação da empresa B
- CTO acessa todas as empresas
- CTO lista solicitações de qualquer empresa
- Logs registram tentativas negadas

---

## Cronograma

| Fase | Duração | Atividade |
|------|---------|-----------|
| Implementação | 2h | Alterações em LgpdService |
| Testes | 1.5h | Testes unitários e integração |
| Code Review | 0.5h | Revisão de segurança |
| Docs | 0.5h | Atualização de documentação |
| **Total** | **4.5h** | - |

---

## Riscos Identificados

| Risco | Probabilidade | Impacto | Mitigação |
|-------|---------------|---------|-----------|
| Quebra de compatibilidade com front-end | Baixa | Médio | Testes integrados |
| CTO perde acesso global | Muito Baixa | Alto | Testes de permissão CTO |
| Manager vê erro genérico sem context | Média | Baixo | Melhorar mensagem de erro |
| Regressão em outros métodos de LGPD | Baixa | Médio | Suite de testes completa |

---

## Checklist de Implementação

- [ ] Implementar validação em listAdminRequests
- [ ] Implementar validação em getRequestDetails
- [ ] Implementar validação em assignRequest
- [ ] Implementar validação em addNote
- [ ] Implementar validação em completeRequest
- [ ] Implementar validação em rejectRequest
- [ ] Criar testes de segurança multi-tenant
- [ ] Executar build
- [ ] Executar testes
- [ ] Atualizar documentação
- [ ] Gerar relatório final

---

## Conclusão

Sprint 2 é **crítica (P0)** para eliminar risco de exposição multi-tenant. As alterações são **localizadas** em um único serviço (LgpdService), com **baixo risco** de regressão se testes forem abrangentes.

Estimativa de conclusão: **4-5 horas** incluindo testes e documentação.
