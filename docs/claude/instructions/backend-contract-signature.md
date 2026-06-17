# Back-end — Assinatura de Contratos

## Branch

`PROD_HOSTINGER_V2`

## Referência

Antes de implementar, leia o fluxo de assinatura do espelho de ponto e o fluxo de documentos.

Arquivos principais:

```text
ApiPaths.java
DocumentType.java
TimesheetSignatureController.java
TimesheetSignatureService.java
DocumentService.java
DomainAuthorizationService.java
AuditService.java
```

## Entrega

Criar módulo de contrato com:

```text
ServiceContract
ServiceContractAssignment
ServiceContractSignature
```

Criar controller, use case, service, providers, repositories, mappers, DTOs, migration Flyway e testes.

## Regras

- O contrato original deve ser PDF.
- O documento original deve usar `SERVICE_CONTRACT_TERMS`.
- O gestor opera apenas dados do próprio tenant.
- O colaborador vê apenas contratos atribuídos a ele.
- Contrato já concluído não aparece como pendente.
- Toda operação sensível deve ser auditável.
