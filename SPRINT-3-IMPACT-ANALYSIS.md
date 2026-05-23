# Sprint 3 — Exportação de Dados do Titular
## Matriz de Impacto

**Data:** 22 de maio de 2026

---

## Objetivo da Sprint

Garantir que a exportação de dados LGPD seja completa, correta, segura e compreensível.

---

## Features

### LGPD-S03-01: Corrigir exportação de audit logs
**Prioridade:** P0  
**Tipo:** Bug  

**Problema Identificado:**
- Método `LgpdService.exportEmployeeData()` busca audit logs usando `employeeId` como `userId`
- `auditService.findByUserId(targetEmployee.employeeId())` está incorreto
- Deve usar `user.userId()` ao invés de `employee.employeeId()`

**Solução:**
- Obter usuário por `employeeId`
- Se usuário existe: usar `user.userId()` para buscar logs
- Se usuário não existe: retornar lista vazia
- Registrar observação no manifesto se não houver usuário

**Impacto de Segurança:** ⚠️ CRÍTICO (P0)
- Caso não resolvido: exportação está vazia ou com dados errados
- Resolve: exportação inclui logs corretos do usuário

---

### LGPD-S03-02: Criar manifesto da exportação
**Prioridade:** P1  
**Tipo:** Transparência  

**Problema Identificado:**
- Exportação não inclui metadados explicativos
- Titular não sabe exatamente o que foi exportado ou por quem
- Arquivo não tem identificador único

**Solução:**
- Adicionar record `ExportManifest` com campos mínimos:
  - `exportId` (UUID único)
  - `exportedAt` (timestamp)
  - `requestedByUserId` (quem pediu)
  - `targetEmployeeId` (dados de quem)
  - `includePreciseGeolocation` (se incluiu coordenadas)
  - `sections` (quais seções foram exportadas)
  - `warnings` (avisos sobre dados sensíveis)
- Incluir manifesto no início da resposta JSON
- Front exibe aviso antes do download

**Impacto:** 📋 TRANSPARÊNCIA
- Melhora conformidade com Art. 20 LGPD (direito de acesso)
- Prova técnica do que foi enviado

---

### LGPD-S03-03: Criar escopo de exportação por perfil
**Prioridade:** P1  
**Tipo:** Segurança  

**Problema Identificado:**
- Não há validação de quem pode exportar dados de quem
- `domainAuthorizationService.authorizeEmployeeAccess()` já valida employer access
- Mas não há razão/justificativa registrada para exportação de terceiros

**Regras de Negócio:**
```
PARTNER: pode exportar apenas próprios dados (já validado)
MANAGER: pode exportar dados de colaboradores da própria empresa (já validado)
         exige razão/justificativa se não for dele mesmo
CTO: pode exportar dados de qualquer colaborador (já validado)
     exige auditoria alta
```

**Solução:**
- Adicionar parâmetro opcional `exportReason` no endpoint
- Se exportando dados de outro usuário: exigir razão
- Registrar razão no audit log
- Separar exportação própria (simples) de terceiros (exige razão)

**Impacto:** 🔐 SEGURANÇA
- Evita exportação abusiva
- Prova técnica de justificativa legal

---

## Arquitetura Existente

### LgpdService
- ✅ Método `exportEmployeeData()` existente
- ❌ Bug: busca audit logs com `employeeId` em vez de `userId`
- ✅ Usa `domainAuthorizationService.authorizeEmployeeAccess()` (valida empresa/permissão)

### LgpdEmployeeExportResponse
- ✅ Record com todos os dados exportados
- ❌ Sem manifesto com metadados
- Tem 9 seções: employee, user, company, documents, timeRecords, messages, auditLogs, legalConsents, biometricStatus

### Autorização
- ✅ `authorizeEmployeeAccess()` já valida tenant e role
- ✅ CTO: acesso global
- ✅ Manager: limitado à empresa
- ✅ Employee: apenas dados próprios

---

## Matriz de Alterações

| Arquivo | Método/Campo | Alteração | Complexidade | Risco |
|---------|------------|----------|--------------|-------|
| LgpdService.java | exportEmployeeData | Corrigir busca de audit logs: user.userId() ao invés de employeeId | Baixa | Baixo |
| LgpdService.java | exportEmployeeData | Adicionar parâmetro exportReason (opcional) | Média | Médio |
| LgpdService.java | exportEmployeeData | Validar razão para exportação de terceiros | Média | Médio |
| LgpdEmployeeExportResponse.java | manifesto (novo) | Adicionar record ExportManifest com metadados | Média | Baixo |
| LgpdEmployeeExportResponse.java | from() | Integrar manifesto na resposta | Baixa | Baixo |
| LgpdServiceTest.java | testes | Novos testes para audit logs corretos | Alta | Baixo |
| LgpdServiceTest.java | testes | Testes de manifesto | Alta | Baixo |
| LgpdServiceTest.java | testes | Testes de scope por perfil | Alta | Baixo |

---

## Validação de Segurança

### Antes da alteração (Vulnerável):
```
Colaborador A solicita exportação de dados
→ exportEmployeeData(employeeA.id, includeGeo)
→ auditService.findByUserId(employeeA.id) ← ERRO: passa employeeId
→ Busca retorna logs vazios ou incorretos
→ Exportação incompleta (falta auditoria)
```

### Depois da alteração (Seguro):
```
Colaborador A solicita exportação de dados
→ exportEmployeeData(employeeA.id, includeGeo)
→ userA = userProvider.findByEmployeeId(employeeA.id)
→ auditLogs = auditService.findByUserId(userA.userId()) ✓
→ Exportação inclui manifesto com metadados
→ Manager exporta colaborador B: exige razão
→ Razão registrada em auditoria
```

---

## Testes Obrigatórios

### LGPD-S03-01: Audit logs corretos
- ✅ Exportação de colaborador com usuário retorna audit logs corretos
- ✅ Exportação de colaborador sem usuário não falha (retorna lista vazia)
- ✅ Logs retornados têm userId, não employeeId
- ✅ Detalhes de logs são sanitizados (sem dados sensíveis)

### LGPD-S03-02: Manifesto
- ✅ Exportação inclui manifesto com exportId único
- ✅ Manifesto inclui timestamp exportedAt
- ✅ Manifesto inclui requestedByUserId (quem pediu)
- ✅ Manifesto inclui targetEmployeeId (de quem)
- ✅ Manifesto lista todas as 9 seções
- ✅ Manifesto inclui warnings sobre dados sensíveis

### LGPD-S03-03: Escopo por perfil
- ✅ Employee pode exportar próprios dados sem razão
- ✅ Employee não pode exportar dados de outro
- ✅ Manager pode exportar colaborador da empresa com razão
- ✅ Manager não pode exportar colaborador de outra empresa
- ✅ CTO pode exportar qualquer colaborador com razão
- ✅ Razão é registrada em auditoria

---

## Cronograma

| Fase | Duração | Atividade |
|------|---------|-----------|
| Implementação | 1.5h | Alterações em LgpdService e DTO |
| Testes | 1h | Testes unitários e integração |
| Front-end | 1h | Exibição de manifesto, campo de razão |
| Build | 0.5h | Compilação e verificação |
| Docs | 0.5h | Atualização de documentação |
| **Total** | **4.5h** | - |

---

## Riscos Identificados

| Risco | Probabilidade | Impacto | Mitigação |
|-------|---------------|---------|-----------|
| Export sem manifesto em cache | Baixa | Médio | Invalidar cache após deploy |
| Razão exportação vazia vs opcional | Média | Baixo | Documentar claramente na API |
| Front não envia razão | Média | Médio | Teste E2E de exportação terceiros |
| Audit log query lenta | Baixa | Médio | Usar índice em userId |
| Aumento de auditoria afeta performance | Baixa | Baixo | Monitorar latência |

---

## Checklist de Implementação

- [ ] Corrigir busca de audit logs em exportEmployeeData
- [ ] Adicionar record ExportManifest
- [ ] Integrar manifesto na resposta
- [ ] Adicionar parâmetro exportReason
- [ ] Validar razão para exportação de terceiros
- [ ] Registrar razão em auditoria
- [ ] Testes unitários: audit logs
- [ ] Testes unitários: manifesto
- [ ] Testes unitários: scope por perfil
- [ ] Testes integração: exportação com manifesto
- [ ] Front-end: exibir manifesto
- [ ] Front-end: campo de razão
- [ ] Front-end: validação de campo obrigatório
- [ ] Build back-end
- [ ] Build front-end
- [ ] Documentação atualizada

---

## Conclusão

Sprint 3 corrige bug crítico de exportação (P0) e adiciona transparência/segurança (P1). As alterações são **localizadas** em LgpdService e DTOs, com **baixo risco** de regressão se testes forem abrangentes.

Estimativa de conclusão: **4-5 horas** incluindo testes, front-end e documentação.
