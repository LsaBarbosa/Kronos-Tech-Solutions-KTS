# SPEC — Correção da Exportação LGPD vinculada a Solicitação Formal

## Objetivo

Corrigir o fluxo de exportação LGPD para impedir exportações administrativas amplas sem solicitação formal, análise, aprovação e trilha decisória.

Separar:

1. Exportação própria do titular:
   - GET /lgpd/me/export
   - usa employeeId do JWT
   - não aceita employeeId por path/query
   - não inclui geolocalização precisa
   - retorna dados minimizados
   - audita LGPD_OWN_DATA_EXPORTED

2. Exportação administrativa:
   - POST /lgpd/admin/requests/{requestId}/export
   - exige requestId
   - resolve employeeId pela LgpdRequest
   - exige status APPROVED_FOR_EXPORT
   - exige legalBasis, operationalReason e reviewerNotes
   - permite apenas requestType ACCESS, PORTABILITY, SHARING_INFORMATION, CONFIRM_PROCESSING
   - bloqueia CORRECTION, ANONYMIZATION, BLOCKING, DELETION, CONSENT_REVOCATION
   - includePreciseGeolocation false por padrão
   - includePreciseGeolocation=true só para CTO e com justificativa explícita em reviewerNotes
   - audita sucesso e bloqueio

## Backend atual

Endpoint atual inseguro:

GET /lgpd/employees/{employeeId}/export

Problema:
- @PreAuthorize(ANY_EMPLOYEE)
- permite employeeId direto
- permite includePreciseGeolocation
- para terceiro exige apenas exportReason textual
- não exige requestId
- não valida solicitação LGPD aprovada

## Implementação obrigatória

### 1. LgpdRequestStatus

Adicionar:

APPROVED_FOR_EXPORT

Atualizar transições:
- WAITING_LEGAL_REVIEW pode ir para APPROVED_FOR_EXPORT
- APPROVED_FOR_EXPORT pode ir para COMPLETED, PARTIALLY_COMPLETED, REJECTED, CANCELLED

### 2. LgpdUseCase

Criar métodos:

LgpdEmployeeExportResponse exportOwnEmployeeData(String ipAddress, String userAgent);

LgpdEmployeeExportResponse exportEmployeeDataForApprovedRequest(
    UUID requestId,
    boolean includePreciseGeolocation,
    String legalBasis,
    String operationalReason,
    String reviewerNotes,
    String ipAddress,
    String userAgent
);

### 3. DTO

Criar DTO:

LgpdAdminExportRequest

Campos:
- boolean includePreciseGeolocation
- String legalBasis
- String operationalReason
- String reviewerNotes

Aplicar Bean Validation:
- legalBasis obrigatório
- operationalReason obrigatório
- reviewerNotes obrigatório

### 4. Controller

Criar:

GET /lgpd/me/export

POST /lgpd/admin/requests/{requestId}/export

Manter endpoint antigo de forma segura:
GET /lgpd/employees/{employeeId}/export

Regra do endpoint antigo:
- se employeeId != employeeId do JWT, retornar 403:
  "Exportação administrativa exige solicitação LGPD aprovada."
- se for o próprio employeeId, delegar para exportOwnEmployeeData

### 5. Service

Refatorar LgpdService:

- extrair buildExport(...)
- criar validateAdministrativeExportRequest(...)
- criar validatePreciseGeolocationForAdminExport(...)
- criar exportOwnEmployeeData(...)
- criar exportEmployeeDataForApprovedRequest(...)

Regras administrativas:
- request precisa existir
- request precisa estar autorizado por domínio
- request.status == APPROVED_FOR_EXPORT
- request.requestType permitido
- assignedToUserId recomendado/obrigatório
- legalBasis não vazio
- operationalReason não vazio
- reviewerNotes não vazio
- geolocalização precisa só com CTO + reviewerNotes explícito

### 6. Auditoria

Adicionar AuditAction, se necessário:

LGPD_OWN_DATA_EXPORTED
LGPD_ADMIN_DATA_EXPORT_ATTEMPTED
LGPD_ADMIN_DATA_EXPORTED
LGPD_ADMIN_DATA_EXPORT_BLOCKED
LGPD_EXPORT_APPROVED

Auditar:
- exportação própria
- exportação administrativa bem-sucedida
- tentativa administrativa bloqueada

### 7. Manifest

Atualizar manifest de LgpdEmployeeExportResponse para conter, se possível:

- requestId
- exportScope
- administrativeExport
- preciseGeolocationIncluded
- approvedByUserId
- approvedAt

Valores:
- OWN_DATA_MINIMIZED
- ADMIN_APPROVED_FULL
- ADMIN_APPROVED_REDACTED

### 8. Documentação

Atualizar:

docs/legal/data-subject-rights.md

Incluir:
- exportação própria minimizada
- exportação administrativa vinculada a requestId
- exigência de APPROVED_FOR_EXPORT
- revisão antes da entrega
- bloqueio de exportação administrativa por employeeId direto

### 9. Testes obrigatórios

Criar/atualizar testes para:

1. titular exporta próprios dados com sucesso
2. exportação própria não inclui geolocalização precisa
3. manager tentando exportar terceiro pelo endpoint antigo recebe 403
4. admin exporta request APPROVED_FOR_EXPORT com sucesso
5. admin não exporta request OPEN
6. admin não exporta request IN_ANALYSIS
7. admin não exporta request REJECTED
8. admin não exporta request CANCELLED
9. admin não exporta request ANONYMIZATION
10. admin não exporta request DELETION
11. admin não exporta sem legalBasis
12. admin não exporta sem operationalReason
13. admin não exporta sem reviewerNotes
14. includePreciseGeolocation=true sem autorização formal gera 403
15. sucesso gera audit log
16. bloqueio gera audit log

## Critério final

A pendência estará resolvida quando não existir exportação administrativa ampla por employeeId direto e toda exportação de terceiro exigir requestId aprovado.
