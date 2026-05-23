# Sprint 4 — Anonimização e Eliminação por Domínio
## Matriz de Impacto

**Data:** 22 de maio de 2026

---

## Objetivo da Sprint

Transformar a anonimização em processo completo, auditável e seguro por domínio de dados.

---

## Features

### LGPD-S04-01: Integrar EmployeeAnonymizationService ao AnonymizationPlanExecutor
**Prioridade:** P0  
**Tipo:** Arquitetura / Domínio  

**Problema Identificado:**
- Processadores de anonimização existem, mas execução não é centralizada
- Não há consolidação de resultados por domínio
- Admin não consegue ver resultado da operação

**Solução:**
- Criar `AnonymizationPlan` com 6 flags de controle:
  - `preserveLaborData` (dados trabalhistas)
  - `preserveFiscalData` (dados fiscais)
  - `deleteBiometricArtifacts` (deletar biometria)
  - `anonymizeDocuments` (anonimizar documentos)
  - `anonymizeMessages` (anonimizar mensagens)
  - `anonymizeAuditLogs` (anonimizar logs)
- Executar via `AnonymizationPlanExecutor`
- Consolidar em `AnonymizationExecutionLog`
- Retornar resumo para admin

**Impacto:** ⚠️ CRÍTICO (P0)

---

### LGPD-S04-02: Implementar modo DRY_RUN para anonimização
**Prioridade:** P0  
**Tipo:** Segurança operacional  

**Problema Identificado:**
- Admin não consegue ver impacto antes de anonimizar
- Risco de surpresas (quanto será afetado?)
- Sem reversão após aplicação

**Solução:**
- Novo endpoint: `POST /lgpd/employees/{employeeId}/anonymize/dry-run`
- Retorna preview com:
  - Quantidade de documentos a afetados
  - Registros de ponto
  - Mensagens
  - Logs de auditoria
  - Artefatos biométricos
  - Dados preservados por lei
  - Riscos identificados
- Sem alterar dados
- Apply requer confirmação explícita

**Impacto:** 🔐 SEGURANÇA OPERACIONAL (P0)

---

### LGPD-S04-03: Estratégia por domínio de anonimização
**Prioridade:** P0  
**Tipo:** Domínio / Segurança  

**Regras por Domínio:**

| Domínio | Estratégia | Preservar |
|---------|-----------|-----------|
| Employee | Anonimizar nome, CPF, PIS, email, telefone, endereço | ID apenas |
| User | Desativar e anonimizar username | ID apenas |
| Biometria | Excluir S3 e Rekognition | Nada |
| Documents | Anonimizar metadados ou excluir | Trabalhista/Fiscal |
| TimeRecord | Preservar se trabalhista/fiscal, pseudonimizar IDs | Labor/Fiscal records |
| Messages | Anonimizar remetente/destinatário e conteúdo | Thread ID apenas |
| AuditLog | Sanitizar detalhes pessoais, preservar evento | Evento mínimo |
| LGPD Request | Preservar evidência mínima de atendimento | Evidência |

**Impacto:** ⚠️ CRÍTICO (P0)

---

## Arquitetura Existente

### EmployeeAnonymizationService
- ✅ Existe e é chamado por `LgpdService.anonymizeEmployee()`
- ❌ Sem integração centralizada
- ❌ Sem logs de execução por domínio
- ❌ Sem modo DRY_RUN

### Anonimização por Domínio
- ✅ Processadores individuais existem (parcialmente)
- ❌ Sem coordenação centralizada
- ❌ Sem AnonymizationPlan
- ❌ Sem AnonymizationExecutionLog

### Controllers
- ✅ `POST /lgpd/employees/{employeeId}/anonymize` existe
- ❌ Sem suporte a DRY_RUN
- ❌ Sem resposta detalhada

---

## Matriz de Alterações

| Arquivo | Método/Campo | Alteração | Complexidade | Risco |
|---------|------------|----------|--------------|-------|
| AnonymizationPlan.java (novo) | N/A | Novo record com 6 flags | Baixa | Baixo |
| AnonymizationExecutionLog.java (novo) | N/A | Novo record para logging | Média | Baixo |
| AnonymizationPlanExecutor.java (novo) | execute() | Orquestrador central | Alta | Médio |
| EmployeeAnonymizationService.java | anonymize() | Refatorar para usar plan | Alta | Médio |
| LgpdService.java | anonymizeEmployee() | Suportar DRY_RUN e reason | Média | Médio |
| LgpdController.java | anonymizeEmployee() | Novo endpoint dry-run | Média | Baixo |
| Processadores (8 domínios) | execute() | Padronizar com plan | Alta | Alto |
| LgpdServiceTest.java | N/A | Testes de anonimização | Alta | Baixo |
| LgpdControllerWebMvcTest.java | N/A | Testes de endpoint | Alta | Baixo |

---

## Processadores por Domínio

1. **EmployeeAnonymizationProcessor**
   - Anonimizar: fullName, cpf, pis, email, phone, address
   - Preservar: employeeId, companyId, createdAt

2. **UserAnonymizationProcessor**
   - Desativar: user.active = false
   - Anonimizar: username
   - Preservar: userId, employeeId

3. **BiometricAnonymizationProcessor**
   - Deletar: faceS3ObjectKey from Employee
   - Deletar: Rekognition face ID
   - Preservar: Nada

4. **DocumentAnonymizationProcessor**
   - Deletar: Se não é trabalhista/fiscal
   - Anonimizar: fileName se travalhista/fiscal preservado
   - Preservar: Documentos trabalhista/fiscal com metadata

5. **TimeRecordAnonymizationProcessor**
   - Preservar: Se trabalhista/fiscal
   - Pseudonimizar: Identificadores de localização
   - Preservar: Records para folha de pagamento

6. **MessageAnonymizationProcessor**
   - Anonimizar: senderEmployeeId, recipientEmployeeId, conteúdo
   - Preservar: messageId, threadId, timestamps

7. **AuditLogAnonymizationProcessor**
   - Sanitizar: userId, IP, details com dados pessoais
   - Preservar: action, timestamp, evento mínimo

8. **LgpdRequestAnonymizationProcessor**
   - Preservar: requestId, status, createdAt, type
   - Anonimizar: requestedByUserId se necessário
   - Preservar: evidência de atendimento

---

## Validação de Segurança

### Antes da alteração (Vulnerável):
```
Admin clica em "anonimizar" 
→ Não sabe o impacto
→ 50K registros deletados sem aviso
→ Sem logs de what happened
→ Sem reversão possível
```

### Depois da alteração (Seguro):
```
Admin clica em "dry-run"
→ Vê: 2K documentos, 15K timerecords, 500 messages
→ Vê: Dados preservados por lei (1K timerecords)
→ Confirma operação
→ Clica em "apply"
→ AnonymizationPlan executa cada domínio
→ AnonymizationExecutionLog registra resultado
→ Admin vê: "Sucesso - 2K docs deletados, 500 msgs anonimizadas"
```

---

## Testes Obrigatórios

### LGPD-S04-01: AnonymizationPlan
- ✅ Plan criado com flags corretos
- ✅ Plan executado por domínio
- ✅ ExecutionLog registra cada operação
- ✅ Falha parcial não fica invisível
- ✅ Admin consegue ver resultado

### LGPD-S04-02: DRY_RUN
- ✅ DRY_RUN não altera dados
- ✅ DRY_RUN retorna contagem exata
- ✅ DRY_RUN mostra dados preservados
- ✅ Apply requer confirmação explícita
- ✅ Apply se executa apenas após DRY_RUN

### LGPD-S04-03: Por Domínio
- ✅ Employee: nome, CPF, PIS, email, phone, address anonimizados
- ✅ User: desativado e username anonimizado
- ✅ Biometria: deletada de S3 e Rekognition
- ✅ Documents: deletados ou anonimizados (preservar trabalhista)
- ✅ TimeRecord: preservados se trabalhista, pseudonimizados
- ✅ Messages: anonimizadas com preservação de thread
- ✅ AuditLog: sanitizado com evento mínimo
- ✅ LGPD Request: evidência preservada

---

## Cronograma

| Fase | Duração | Atividade |
|------|---------|-----------|
| Design | 1h | Arquitetura de AnonymizationPlan e Executor |
| Implementação | 3h | Processadores por domínio (8) |
| DRY_RUN | 1h | Endpoint e validação |
| Testes | 2h | Testes unitários e integração |
| Build | 0.5h | Compilação e verificação |
| Docs | 0.5h | Atualização de documentação |
| **Total** | **8h** | - |

---

## Riscos Identificados

| Risco | Probabilidade | Impacto | Mitigação |
|-------|---------------|---------|-----------|
| Processador falha parcial afeta outros | Média | Alto | ExecutionLog com todos os resultados |
| S3 delete falha mas BD deletado | Média | Alto | Transação com rollback |
| Dados trabalhista não preservados | Baixa | Alto | Testes específicos por domínio |
| DRY_RUN lento (big data) | Baixa | Médio | Cache de contagens |
| Admin esquece DRY_RUN | Baixa | Médio | UI com obrigatoriedade |

---

## Checklist de Implementação

- [ ] Criar AnonymizationPlan record
- [ ] Criar AnonymizationExecutionLog record
- [ ] Criar AnonymizationPlanExecutor
- [ ] Implementar EmployeeAnonymizationProcessor
- [ ] Implementar UserAnonymizationProcessor
- [ ] Implementar BiometricAnonymizationProcessor
- [ ] Implementar DocumentAnonymizationProcessor
- [ ] Implementar TimeRecordAnonymizationProcessor
- [ ] Implementar MessageAnonymizationProcessor
- [ ] Implementar AuditLogAnonymizationProcessor
- [ ] Implementar LgpdRequestAnonymizationProcessor
- [ ] Refatorar EmployeeAnonymizationService para usar plan
- [ ] Criar endpoint DRY_RUN
- [ ] Testes para cada processador
- [ ] Testes integração DRY_RUN
- [ ] Testes de segurança (transação, rollback)
- [ ] Build back-end
- [ ] Documentação atualizada

---

## Conclusão

Sprint 4 é **crítica (P0)** para anonimização segura e auditável por domínio. As alterações são **significativas** (8 novos processadores + orquestrador), com **médio risco** se testes forem abrangentes.

Estimativa de conclusão: **8 horas** incluindo testes, integração e documentação.

**Impacto:**
- ✅ Anonimização completa por domínio
- ✅ Modo DRY_RUN para segurança operacional
- ✅ Auditoria centralizada com ExecutionLog
- ✅ Reversibilidade via transações
