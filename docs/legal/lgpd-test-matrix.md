# Matriz de Testes LGPD - Kronos

## Overview

Esta matriz de testes documenta os cenários de teste essenciais para validar a conformidade LGPD da plataforma Kronos. Cada cenário inclui pré-condições, passos, resultados esperados e evidências de validação.

**Data:** 2026-05-24  
**Versão:** 1.0  
**Status:** Ativo

---

## 1. Aceite de Consentimento Biométrico

| Atributo | Descrição |
|----------|-----------|
| **ID** | LGPD-TEST-001 |
| **Cenário** | Usuário aceita termo de consentimento biométrico |
| **Prioridade** | Alta |

### Pré-condições
- Usuário autenticado na plataforma
- Termo biométrico atual disponível no backend
- Usuário não possui consentimento ativo anteriormente

### Passos
1. Acessar Centro de Privacidade
2. Navegar para seção de Consentimento Biométrico
3. Clicar em "Aceitar e Cadastrar Biometria"
4. Revisar texto do termo completo
5. Confirmar aceite
6. Capturar imagem facial (se requerido)

### Resultado Esperado
- Aceite registrado no sistema
- Status muda para "Consentimento Ativo"
- `LegalConsent` criado no banco de dados
- Auditoria registrada com `BIOMETRIC_CONSENT_ACCEPTED`

### Evidência Esperada
- ✓ Registro em `tb_legal_consent` com `acceptedAt` preenchido
- ✓ Log de auditoria contendo `BIOMETRIC_CONSENT_ACCEPTED`
- ✓ IP e User-Agent registrados
- ✓ Hash do termo salvo para validação futura

### Responsável
- QA / Developer

---

## 2. Revogação de Consentimento Biométrico

| Atributo | Descrição |
|----------|-----------|
| **ID** | LGPD-TEST-002 |
| **Cenário** | Usuário revoga consentimento biométrico ativo |
| **Prioridade** | Alta |

### Pré-condições
- Usuário possui consentimento biométrico ativo
- Biometria foi previamente cadastrada
- Usuário autenticado

### Passos
1. Acessar Centro de Privacidade
2. Clicar em "Revogar Consentimento"
3. Confirmar revogação na dialog
4. Sistema atualiza sessão

### Resultado Esperado
- Consentimento marcado como revogado
- `revokedAt` preenchido no registro
- Artefatos biométricos removidos
- Status muda para "Consentimento Pendente"
- Sessão atualizada
- Auditoria registrada com `BIOMETRIC_CONSENT_REVOKED`

### Evidência Esperada
- ✓ `tb_legal_consent.revoked_at` preenchido
- ✓ Registros biométricos deletados
- ✓ Log de auditoria com `BIOMETRIC_CONSENT_REVOKED`
- ✓ IP da revogação registrado
- ✓ `tb_legal_consent.revoked_reason` documentado

### Responsável
- QA / Developer

---

## 3. Consulta de Status de Consentimento

| Atributo | Descrição |
|----------|-----------|
| **ID** | LGPD-TEST-003 |
| **Cenário** | Verificar status atual do consentimento biométrico |
| **Prioridade** | Alta |

### Pré-condições
- Usuário autenticado
- Endpoint `/terms/status` operacional

### Passos
1. Chamar GET `/terms/status`
2. Verificar resposta

### Resultado Esperado
- HTTP 200 OK
- Response: `{ "accepted": boolean }`
- Campo `accepted` reflete estado real

### Evidência Esperada
- ✓ `accepted: true` quando há consentimento ativo válido
- ✓ `accepted: false` quando revogado ou pendente
- ✓ Resposta consistente em múltiplas chamadas
- ✓ Sem exposição de dados sensíveis na resposta

### Responsável
- QA / Developer

---

## 4. Exportação de Dados Próprios (Mesmo Usuário)

| Atributo | Descrição |
|----------|-----------|
| **ID** | LGPD-TEST-004 |
| **Cenário** | Usuário exporta seus próprios dados |
| **Prioridade** | Alta |

### Pré-condições
- Usuário autenticado
- Dados pessoais no sistema
- Endpoint `/lgpd/employees/{employeeId}/export` operacional

### Passos
1. Acessar Centro de Privacidade
2. Clicar em "Exportar Meus Dados"
3. Revisar modal de confirmação
4. Confirmar exportação
5. Download inicia automaticamente

### Resultado Esperado
- HTTP 200 OK
- Arquivo PDF/ZIP retornado
- Manifesto de exportação exibido
- Auditoria registrada com `LGPD_DATA_EXPORTED`
- Dados sanitizados no arquivo

### Evidência Esperada
- ✓ `tb_audit_log` contém `LGPD_DATA_EXPORTED`
- ✓ Arquivo contém dados corretos e completos
- ✓ CPF/PII mascarados nos logs (ex: `***.456.789-**`)
- ✓ Export manifest salvo com ID único
- ✓ IP e User-Agent registrados
- ✓ Timestamp de exportação em `ExportManifest`

### Responsável
- QA / Developer

---

## 5. Tentativa de Exportar Dados de Outro Usuário (Sem Autorização)

| Atributo | Descrição |
|----------|-----------|
| **ID** | LGPD-TEST-005 |
| **Cenário** | Usuário tenta exportar dados de outro colaborador sem justificativa |
| **Prioridade** | Alta |

### Pré-condições
- Usuário Partner/Employee autenticado
- Outro colaborador existe no sistema
- Usuário NÃO possui role CTO/MANAGER

### Passos
1. Tentar chamar GET `/lgpd/employees/{outroId}/export`
2. Observar resposta

### Resultado Esperado
- HTTP 403 Forbidden
- Erro: "Unauthorized to export data for this employee"
- Auditoria registrada com tentativa

### Evidência Esperada
- ✓ `tb_audit_log` contém tentativa com `LGPD_DATA_EXPORTED`
- ✓ Campo `details` indica autorização negada
- ✓ IP da tentativa registrado
- ✓ User-Agent registrado
- ✓ Dados não foram exportados

### Responsável
- QA / Security

---

## 6. Criação de Solicitação LGPD

| Atributo | Descrição |
|----------|-----------|
| **ID** | LGPD-TEST-006 |
| **Cenário** | Usuário cria nova solicitação LGPD |
| **Prioridade** | Alta |

### Pré-condições
- Usuário autenticado
- Endpoint `/lgpd/requests` operacional
- Tipos de solicitação disponíveis

### Passos
1. Acessar Centro de Privacidade
2. Criar nova solicitação LGPD
3. Selecionar tipo (ACCESS, DELETION, RECTIFICATION)
4. Fornecer descrição
5. Submeter

### Resultado Esperado
- HTTP 201 Created
- `LgpdRequest` criado no banco
- Status inicial: `OPEN`
- ID da solicitação retornado
- Auditoria registrada com `LGPD_REQUEST_CREATED`

### Evidência Esperada
- ✓ `tb_lgpd_request` contém novo registro
- ✓ `status = 'OPEN'`
- ✓ `created_at` preenchido
- ✓ `tb_audit_log` contém `LGPD_REQUEST_CREATED`
- ✓ IP e User-Agent registrados

### Responsável
- QA / Developer

---

## 7. Visualizar Histórico de Solicitação LGPD

| Atributo | Descrição |
|----------|-----------|
| **ID** | LGPD-TEST-007 |
| **Cenário** | Consultar histórico de transições de solicitação |
| **Prioridade** | Média |

### Pré-condições
- Solicitação LGPD criada
- Transições no histórico
- Endpoint `/lgpd/requests/{requestId}/history` operacional

### Passos
1. Chamar GET `/lgpd/requests/{requestId}/history`
2. Revisar resposta

### Resultado Esperado
- HTTP 200 OK
- Lista ordenada cronologicamente
- Cada entrada contém status e timestamp

### Evidência Esperada
- ✓ Todas as transições de status aparecem
- ✓ Ordem cronológica preservada
- ✓ Timestamps precisos
- ✓ Sem dados pessoais expostos

### Responsável
- QA / Developer

---

## 8. Atribuição Administrativa de Solicitação LGPD

| Atributo | Descrição |
|----------|-----------|
| **ID** | LGPD-TEST-008 |
| **Cenário** | Manager/CTO atribui solicitação para outro gestor |
| **Prioridade** | Média |

### Pré-condições
- Solicitação LGPD em status OPEN
- Usuário possui role CTO/MANAGER
- Alvo de atribuição é válido

### Passos
1. Chamar PATCH `/lgpd/admin/requests/{requestId}/assign`
2. Fornecer `assignedToUserId`

### Resultado Esperado
- HTTP 200 OK
- `assignedToUserId` atualizado
- Auditoria registrada com `LGPD_REQUEST_ASSIGNED`
- Notificação enviada para responsável

### Evidência Esperada
- ✓ `tb_lgpd_request.assigned_to_user_id` atualizado
- ✓ `tb_audit_log` contém `LGPD_REQUEST_ASSIGNED`
- ✓ Details: `oldAssignedToUserId`, `newAssignedToUserId`, `actorUserId`
- ✓ Notificação enfileirada

### Responsável
- QA / Developer

---

## 9. Conclusão/Rejeição/Cancelamento de Solicitação

| Atributo | Descrição |
|----------|-----------|
| **ID** | LGPD-TEST-009 |
| **Cenário** | Finalizar solicitação (sucesso, rejeição ou cancelamento) |
| **Prioridade** | Alta |

### Pré-condições
- Solicitação em status OPEN/IN_ANALYSIS
- Usuário possui permissão apropriada

### Passos - Conclusão
1. Chamar POST `/lgpd/admin/requests/{requestId}/complete`
2. Fornecer notas de resolução

### Resultado Esperado - Conclusão
- Status muda para COMPLETED
- Auditoria registrada com `LGPD_REQUEST_COMPLETED`

### Passos - Rejeição
1. Chamar POST `/lgpd/admin/requests/{requestId}/reject`
2. Fornecer motivo

### Resultado Esperado - Rejeição
- Status muda para REJECTED
- Auditoria registrada com `LGPD_REQUEST_REJECTED`

### Passos - Cancelamento
1. Chamar POST `/lgpd/admin/requests/{requestId}/cancel` (CTO apenas)
2. Fornecer razão

### Resultado Esperado - Cancelamento
- Status muda para CANCELLED
- Auditoria registrada com `LGPD_REQUEST_CANCELLED`

### Evidência Esperada (Todos)
- ✓ `tb_audit_log` contém ação apropriada
- ✓ `status` atualizado corretamente
- ✓ Timestamps preenchidos
- ✓ Motivo/notas sanitizados (não brutos nos logs)

### Responsável
- QA / Developer

---

## 10. Anonimização Dry-Run

| Atributo | Descrição |
|----------|-----------|
| **ID** | LGPD-TEST-010 |
| **Cenário** | Executar dry-run de anonimização sem alterar dados |
| **Prioridade** | Média |

### Pré-condições
- Colaborador com dados no sistema
- Endpoint `/lgpd/employees/{employeeId}/anonymize/dry-run` operacional

### Passos
1. Chamar POST `/lgpd/employees/{employeeId}/anonymize/dry-run`
2. Revisar resultado

### Resultado Esperado
- HTTP 200 OK
- Retorna `AnonymizationDryRunResponse`
- Contém contagem de registros a afetar
- Nenhum dado alterado

### Evidência Esperada
- ✓ Resposta contém: `resourceType`, `totalScanned`, `totalEligible`, `action`
- ✓ Nenhum dado deletado/anonimizado no banco
- ✓ Sem auditoria de APPLY (apenas DRY_RUN)
- ✓ Sem envio de notificações

### Responsável
- QA / Developer

---

## 11. Retenção Dry-Run

| Atributo | Descrição |
|----------|-----------|
| **ID** | LGPD-TEST-011 |
| **Cenário** | Executar dry-run de retenção sem alterar dados |
| **Prioridade** | Média |

### Pré-condições
- Endpoint `/lgpd/admin/retention/dry-run` operacional
- Usuário possui role CTO

### Passos
1. Chamar GET `/lgpd/admin/retention/dry-run`
2. Revisar resultado

### Resultado Esperado
- HTTP 200 OK
- Lista de `RetentionDryRunResult`
- Cada item contém domínio e contagem
- Nenhum dado alterado

### Evidência Esperada
- ✓ Resposta contém: `policyCode`, `resourceType`, `totalScanned`, `totalEligible`
- ✓ Nenhum registro deletado/minimizado
- ✓ Logs contêm: `event=lgpd_retention_dry_run`
- ✓ Sem chamada a `LgpdRetentionApplyService`

### Responsável
- QA / Developer

---

## 12. Sanitização de Dados Sensíveis em Audit Log

| Atributo | Descrição |
|----------|-----------|
| **ID** | LGPD-TEST-012 |
| **Cenário** | Validar que audit logs não contêm dados pessoais crus |
| **Prioridade** | Alta |

### Pré-condições
- Diversas operações executadas
- `tb_audit_log` contém registros com `details`

### Passos
1. Querying `tb_audit_log` com busca por CPF/email
2. Validar nenhum match cru

### Resultado Esperado
- Nenhum CPF completo em `details`
- Nenhum email completo em `details`
- Nenhuma base64 longa em `details`
- Nenhum JWT token em `details`

### Evidência Esperada
- ✓ CPF aparece como `***.456.789-**` (mascarado)
- ✓ Email aparece como `us***@d***` (mascarado)
- ✓ Tokens aparecem como `[JWT_TOKEN_REDACTED]`
- ✓ Base64 longo aparece como `[BASE64_REDACTED]`

### Responsável
- QA / Security

---

## 13. Resolução Correta de IP Confiável

| Atributo | Descrição |
|----------|-----------|
| **ID** | LGPD-TEST-013 |
| **Cenário** | Validar que IP é corretamente resolvido com proxy confiável |
| **Prioridade** | Alta |

### Pré-condições
- `kronos.security.client-ip.trust-forwarded-headers=true`
- `kronos.security.client-ip.trusted-proxy-cidrs` configurado
- Request com `X-Forwarded-For` header

### Passos
1. Request a `/lgpd/processing-catalog` com:
   - `Remote-Addr: 127.0.0.1` (trusted)
   - `X-Forwarded-For: 192.0.2.1`
2. Validar IP registrado em auditoria

### Resultado Esperado
- IP registrado: `192.0.2.1` (do header, confiável)
- `ipTrusted: true`

### Passos - Não Confiável
1. Request com:
   - `Remote-Addr: 198.51.100.7` (não trusted)
   - `X-Forwarded-For: 192.0.2.1`
2. Validar IP registrado

### Resultado Esperado - Não Confiável
- IP registrado: `198.51.100.7` (remoteAddr, não header)
- `ipTrusted: false`

### Evidência Esperada
- ✓ `tb_audit_log.ip_trusted` = true/false apropriado
- ✓ `tb_audit_log.details` contém `ipSource` (X_FORWARDED_FOR ou REMOTE_ADDR)
- ✓ Comportamento consistente em todas operações

### Responsável
- QA / Security

---

## Matriz de Execução

| ID | Cenário | Backend | Frontend | Periodicidade | Status |
|---|---------|---------|----------|---------------|--------|
| LGPD-TEST-001 | Aceite Biométrico | ✓ | ✓ | Manual + CI | Pendente |
| LGPD-TEST-002 | Revogação | ✓ | ✓ | Manual + CI | Pendente |
| LGPD-TEST-003 | Consulta Status | ✓ | - | CI | Pendente |
| LGPD-TEST-004 | Exportar Próprios | ✓ | ✓ | Manual + CI | Pendente |
| LGPD-TEST-005 | Exportar Sem Auth | ✓ | - | CI | Pendente |
| LGPD-TEST-006 | Criar Solicitação | ✓ | ✓ | Manual + CI | Pendente |
| LGPD-TEST-007 | Histórico | ✓ | - | CI | Pendente |
| LGPD-TEST-008 | Atribuição Admin | ✓ | - | CI | Pendente |
| LGPD-TEST-009 | Conclusão/Rejeição | ✓ | - | CI | Pendente |
| LGPD-TEST-010 | Anonimização DryRun | ✓ | - | CI | Pendente |
| LGPD-TEST-011 | Retenção DryRun | ✓ | - | CI | Pendente |
| LGPD-TEST-012 | Sanitização Logs | ✓ | - | Manual | Pendente |
| LGPD-TEST-013 | Resolução IP | ✓ | - | CI | Pendente |

---

## Checklist Pré-Deploy

- [ ] Todos os testes LGPD executados com sucesso
- [ ] Nenhum dado pessoal exposto em logs
- [ ] IP resolution funciona com proxies
- [ ] Auditoria registra todas operações
- [ ] Consentimento biométrico funciona completo
- [ ] Exportação retorna dados corretos
- [ ] Retenção dry-run não altera dados
- [ ] Emails de notificação enviados corretamente
- [ ] Documentação atualizada
- [ ] Testes de segurança passam

---

**Próximas Ações:**
1. Implementar testes de integração (LGPD-S08-T02, T03)
2. Implementar testes E2E (LGPD-S08-T04)
3. Executar matriz completa em staging
4. Documental problemas encontrados
5. Deploy em produção após validação

