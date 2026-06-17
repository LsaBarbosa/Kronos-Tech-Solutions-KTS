---
name: kronos-contract-signature
description: Implementar assinatura eletrônica de contratos no Kronos usando o fluxo do espelho de ponto como referência.
---

# Skill — Kronos Contract Signature

## Uso

Use esta skill para implementar o módulo de assinatura eletrônica de contratos no Kronos.

## Repositórios alvo

- Back-end: `LsaBarbosa/Kronos-Tech-Solutions-KTS`, branch `PROD_HOSTINGER_V2`.
- Front-end: `LsaBarbosa/Kronos-Tech-Solution-User-Plataform`, branch `PROD_HOSTINGER_v2`.
- Documentação: `LsaBarbosa/kronos-business`, branch `main`.

## Referência obrigatória

Antes de implementar, leia o fluxo atual de assinatura do espelho de ponto:

```text
src/main/java/com/kts/kronos/adapter/in/web/http/TimesheetSignatureController.java
src/main/java/com/kts/kronos/application/port/in/usecase/TimesheetSignatureUseCase.java
src/main/java/com/kts/kronos/application/service/TimesheetSignatureService.java
src/main/java/com/kts/kronos/domain/model/TimesheetSignature.java
src/main/java/com/kts/kronos/adapter/out/persistence/entity/TimesheetSignatureEntity.java
src/main/java/com/kts/kronos/adapter/out/persistence/TimesheetSignatureRepository.java
src/main/java/com/kts/kronos/application/port/out/provider/TimesheetSignatureProvider.java
src/main/java/com/kts/kronos/adapter/out/persistence/impl/TimesheetSignatureProviderImpl.java
src/main/java/com/kts/kronos/adapter/out/persistence/mapper/TimesheetSignatureMapper.java
```

Leia também:

```text
src/main/java/com/kts/kronos/constants/ApiPaths.java
src/main/java/com/kts/kronos/domain/model/enuns/DocumentType.java
src/main/java/com/kts/kronos/application/service/DocumentService.java
src/main/java/com/kts/kronos/application/security/DomainAuthorizationService.java
src/main/java/com/kts/kronos/adapter/out/security/JwtAuthenticatedUser.java
src/main/java/com/kts/kronos/application/service/AuditService.java
src/main/java/com/kts/kronos/infrastructure/DigitalSignatureService.java
```

## Resultado técnico esperado

Criar domínio próprio para contratos:

```text
ServiceContract
ServiceContractAssignment
ServiceContractSignature
```

Criar endpoints administrativos para envio e acompanhamento, endpoints do colaborador para pendências, preview e assinatura, além de download do documento final.

## Regras centrais

- O contrato original deve ser PDF.
- O arquivo original deve ser salvo no bucket como `SERVICE_CONTRACT_TERMS`.
- Cada contrato pode ser atribuído a um ou mais colaboradores.
- Cada colaborador assina sua própria atribuição.
- Contrato já concluído pelo colaborador não volta à lista de pendências.
- Toda operação deve respeitar tenant.
- Toda alteração estrutural deve usar Flyway.
- Controllers devem apenas orquestrar entrada, saída e chamada do use case.

## Critério de pronto

- Back-end compila e possui testes relevantes.
- Front-end compila e possui fluxo visual completo.
- Documentação é atualizada.
- O fluxo manual cobre: envio, atribuição, pendência, preview, assinatura, ocultação da pendência e consulta administrativa.
