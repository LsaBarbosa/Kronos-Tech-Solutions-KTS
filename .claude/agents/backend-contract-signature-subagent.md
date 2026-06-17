---
name: backend-contract-signature-subagent
description: Implementa a camada back-end da assinatura eletrônica de contratos no Kronos.
tools: Read, Grep, Glob, Bash, Edit, MultiEdit, Write, TodoWrite
---

# Subagent — Backend Contract Signature

## Missão

Criar o módulo back-end de contratos e assinaturas eletrônicas, usando o módulo `TimesheetSignature` como referência arquitetural.

## Branch

```bash
git checkout PROD_HOSTINGER_V2
```

## Arquivos de referência obrigatórios

Leia antes de editar:

```text
src/main/java/com/kts/kronos/constants/ApiPaths.java
src/main/java/com/kts/kronos/domain/model/enuns/DocumentType.java

src/main/java/com/kts/kronos/adapter/in/web/http/TimesheetSignatureController.java
src/main/java/com/kts/kronos/application/port/in/usecase/TimesheetSignatureUseCase.java
src/main/java/com/kts/kronos/application/service/TimesheetSignatureService.java
src/main/java/com/kts/kronos/domain/model/TimesheetSignature.java
src/main/java/com/kts/kronos/adapter/out/persistence/entity/TimesheetSignatureEntity.java
src/main/java/com/kts/kronos/adapter/out/persistence/TimesheetSignatureRepository.java
src/main/java/com/kts/kronos/application/port/out/provider/TimesheetSignatureProvider.java
src/main/java/com/kts/kronos/adapter/out/persistence/impl/TimesheetSignatureProviderImpl.java
src/main/java/com/kts/kronos/adapter/out/persistence/mapper/TimesheetSignatureMapper.java

src/main/java/com/kts/kronos/application/port/in/usecase/PointMirrorPdfUseCase.java
src/main/java/com/kts/kronos/application/service/PointMirrorPdfService.java
src/main/java/com/kts/kronos/infrastructure/DigitalSignatureService.java

src/main/java/com/kts/kronos/application/port/in/usecase/DocumentUseCase.java
src/main/java/com/kts/kronos/application/service/DocumentService.java
src/main/java/com/kts/kronos/adapter/in/web/http/DocumentController.java
src/main/java/com/kts/kronos/adapter/out/persistence/entity/DocumentEntity.java

src/main/java/com/kts/kronos/application/security/DomainAuthorizationService.java
src/main/java/com/kts/kronos/adapter/out/security/JwtAuthenticatedUser.java
src/main/java/com/kts/kronos/application/service/AuditService.java
```

## Modelo de domínio sugerido

### `ServiceContract`

```java
public record ServiceContract(
    UUID contractId,
    UUID companyId,
    UUID sourceDocumentId,
    UUID sourceDocumentOwnerEmployeeId,
    UUID createdByUserId,
    UUID createdByEmployeeId,
    String title,
    String description,
    String originalFileName,
    String documentHashSha256,
    ServiceContractStatus status,
    Instant createdAt,
    Instant updatedAt,
    Instant voidedAt,
    UUID voidedByUserId,
    String voidReason
) {}
```

### `ServiceContractAssignment`

```java
public record ServiceContractAssignment(
    UUID assignmentId,
    UUID contractId,
    UUID companyId,
    UUID employeeId,
    UUID assignedByUserId,
    ServiceContractAssignmentStatus status,
    Instant assignedAt,
    Instant signedAt,
    Instant cancelledAt
) {}
```

### `ServiceContractSignature`

```java
public record ServiceContractSignature(
    UUID signatureId,
    UUID assignmentId,
    UUID contractId,
    UUID employeeId,
    UUID companyId,
    UUID signerUserId,
    Instant signedAt,
    String signedAtZone,
    ContractSignatureType signatureType,
    ContractSignatureMethod signatureMethod,
    ContractSignatureStatus status,
    UUID signedDocumentId,
    String contractDocumentHashSha256,
    String signedPdfHashSha256,
    String declarationVersion,
    String declarationHashSha256,
    String declarationText,
    String ipAddress,
    String userAgent,
    String evidenceJson,
    Instant createdAt,
    Instant updatedAt,
    Instant voidedAt,
    UUID voidedByUserId,
    String voidReason
) {}
```

## Enums sugeridos

```java
ServiceContractStatus {
    ACTIVE,
    VOIDED
}

ServiceContractAssignmentStatus {
    PENDING,
    SIGNED,
    CANCELLED
}

ContractSignatureStatus {
    ACTIVE,
    VOIDED
}

ContractSignatureType {
    INTERNAL_ADVANCED
}

ContractSignatureMethod {
    PASSWORD_REAUTH
}
```

## Endpoints sugeridos

Adicionar em `ApiPaths`:

```java
public static final String SERVICE_CONTRACTS = "/service-contracts";
public static final String SERVICE_CONTRACT_ADMIN = "/admin";
public static final String SERVICE_CONTRACT_ADMIN_ID = "/admin/{contractId}";
public static final String SERVICE_CONTRACT_ME_PENDING = "/me/pending";
public static final String SERVICE_CONTRACT_PREVIEW = "/{contractId}/preview";
public static final String SERVICE_CONTRACT_SIGN = "/{contractId}/sign";
public static final String SERVICE_CONTRACT_SIGNATURE_DOCUMENT = "/signatures/{signatureId}/document";
public static final String SERVICE_CONTRACT_ADMIN_SIGNATURES = "/admin/signatures";
```

Criar controller:

```text
src/main/java/com/kts/kronos/adapter/in/web/http/ServiceContractController.java
```

Com métodos:

```text
POST /service-contracts/admin
GET  /service-contracts/admin
GET  /service-contracts/admin/{contractId}
GET  /service-contracts/me/pending
GET  /service-contracts/{contractId}/preview
POST /service-contracts/{contractId}/sign
GET  /service-contracts/signatures/{signatureId}/document
GET  /service-contracts/admin/signatures
```

## DTOs sugeridos

Criar pacote:

```text
src/main/java/com/kts/kronos/adapter/in/web/dto/servicecontract
```

DTOs:

```text
CreateServiceContractRequest
ServiceContractAdminItemResponse
ServiceContractAdminPageResponse
PendingServiceContractResponse
PendingServiceContractListResponse
SignServiceContractRequest
SignServiceContractResponse
ServiceContractSignatureAdminItemResponse
ServiceContractSignatureAdminPageResponse
```

### `CreateServiceContractRequest`

Como upload é multipart, aceitar os dados como partes:

```text
file: MultipartFile
title: String
description?: String
employeeIds: String ou List<UUID>
```

Recomendação técnica:

- Se Spring aceitar `@RequestPart("employeeIds") List<UUID>`, usar lista.
- Se houver problema de binding no front, aceitar JSON string e parsear no service/controller.
- Não colocar regra de tenant no controller.

### `SignServiceContractRequest`

```java
public record SignServiceContractRequest(
    boolean confirmed,
    String declarationVersion,
    String declarationHashSha256,
    String contractDocumentHashSha256,
    String password
) {}
```

## Regras de negócio

### Upload pelo manager

1. Usuário deve ter role `MANAGER` ou `CTO`.
2. Resolver `companyId` pelo colaborador autenticado.
3. Validar PDF:
   - arquivo obrigatório;
   - `contentType = application/pdf`;
   - extensão `.pdf`;
   - tamanho conforme limite já adotado no projeto.
4. Validar título obrigatório.
5. Validar `employeeIds` não vazio.
6. Buscar colaboradores alvo.
7. Todos os colaboradores devem pertencer ao mesmo tenant do manager.
8. Calcular SHA-256 do PDF original.
9. Salvar documento original no bucket com:
   ```java
   DocumentType.SERVICE_CONTRACT_TERMS
   ```
10. Criar `ServiceContract`.
11. Criar uma atribuição `PENDING` por colaborador.
12. Auditar criação.

### Pendências do colaborador

1. Resolver `employeeId` autenticado.
2. Buscar atribuições `PENDING` do colaborador.
3. Excluir qualquer contrato com assinatura `ACTIVE`.
4. Retornar:
   - `contractId`;
   - `assignmentId`;
   - `title`;
   - `description`;
   - `createdAt`;
   - `documentHashSha256`;
   - `declarationVersion`;
   - `declarationText`;
   - `declarationHashSha256`.

### Preview

1. Colaborador só pode ver contrato atribuído a ele.
2. Manager/CTO só pode ver contratos do próprio tenant.
3. Retornar PDF original como `inline`.
4. Auditar visualização com risco `LOW`.

### Assinatura

1. Validar `confirmed == true`.
2. Validar contrato ativo.
3. Validar atribuição do colaborador autenticado.
4. Bloquear duplicidade:
   - se já existe assinatura `ACTIVE`;
   - se assignment já está `SIGNED`.
5. Reautenticar senha com `PasswordEncoder.matches`.
6. Validar:
   - hash do PDF original recebido pelo front;
   - versão da declaração;
   - hash da declaração.
7. Gerar PDF com carimbo visível de assinatura eletrônica.
8. Aplicar assinatura digital PAdES com `DigitalSignatureService.signPdf`.
9. Calcular hash SHA-256 do PDF assinado.
10. Salvar PDF assinado no bucket com:
    ```java
    DocumentType.SERVICE_CONTRACT_TERMS
    ```
    usando `employeeId` do assinante como dono do documento assinado.
11. Persistir `ServiceContractSignature`.
12. Atualizar assignment para `SIGNED`.
13. Auditar assinatura com risco `HIGH`.
14. Retornar `SignServiceContractResponse`.

## PDF de contrato assinado

Criar serviço:

```text
src/main/java/com/kts/kronos/application/service/ServiceContractPdfStampService.java
```

Responsabilidade:

- receber bytes do PDF original;
- adicionar última página ou carimbo visível com:
  - título "ASSINATURA ELETRÔNICA REGISTRADA";
  - nome do signatário;
  - data/hora em `America/Sao_Paulo`;
  - versão da declaração;
  - hash SHA-256 do contrato original, preferencialmente abreviado;
  - método `PASSWORD_REAUTH`;
  - indicação de assinatura digital institucional PAdES.

Não usar imagem de assinatura manual.

## Flyway

Criar migration nova em:

```text
src/main/resources/db/migration/
```

Nome sugerido:

```text
V<next>__create_service_contract_signature_tables.sql
```

Tabelas:

```sql
tb_service_contract
tb_service_contract_assignment
tb_service_contract_signature
```

Índices obrigatórios:

```sql
idx_service_contract_company_id
idx_service_contract_assignment_employee_status
idx_service_contract_assignment_contract_employee
idx_service_contract_signature_contract_employee
idx_service_contract_signature_assignment
```

Constraints obrigatórias:

```sql
unique(contract_id, employee_id)
unique(assignment_id)
```

Preferir `CHECK` para status quando o padrão do projeto usar `CHECK`.

## Auditoria

Adicionar ações em `AuditAction`, se existir enum:

```text
SERVICE_CONTRACT_CREATED
SERVICE_CONTRACT_VIEWED
SERVICE_CONTRACT_SIGNED
SERVICE_CONTRACT_PASSWORD_INVALID
SERVICE_CONTRACT_SIGNATURE_BLOCKED
SERVICE_CONTRACT_SIGNATURE_VIEWED
SERVICE_CONTRACT_VOIDED
```

Não logar:

- senha;
- CPF;
- conteúdo do PDF;
- token;
- nome completo em logs estruturados, se o projeto já usa referências minimizadas.

## Testes mínimos

Criar testes para:

```text
ServiceContractServiceTest
ServiceContractControllerWebMvcTest
ServiceContractSecurityTest
```

Cenários:

- manager cria contrato para colaborador do mesmo tenant;
- manager não cria para colaborador de outro tenant;
- upload rejeita arquivo não PDF;
- colaborador lista apenas pendências próprias;
- contrato assinado não aparece em pendências;
- assinatura exige senha correta;
- assinatura bloqueia duplicidade;
- assinatura rejeita hash divergente;
- download de documento assinado respeita self ou manager do tenant;
- manager de outro tenant não acessa.
```

## Comandos de validação

```bash
./gradlew clean test
./gradlew bootJar
```

Se houver separação de testes:

```bash
./gradlew unitTest
./gradlew integrationTest
```
