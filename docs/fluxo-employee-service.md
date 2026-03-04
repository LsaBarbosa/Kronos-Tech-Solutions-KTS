# Fluxos do `EmployeeService` (método a método)

Este documento detalha o fluxo de **cada método de serviço** da classe `EmployeeService`.

## 1) `createEmployee(CreateEmployeeRequest req)`

```mermaid
flowchart TD
    A[Início] --> B{Role do usuário logado}
    B -->|CTO| C{companyId informado?}
    C -->|Não| C1[BadRequestException]
    C -->|Sim| D[Usa req.companyId]

    B -->|MANAGER| E[Busca manager no employeeProvider]
    E --> E1{Encontrou?}
    E1 -->|Não| E2[ResourceNotFoundException]
    E1 -->|Sim| D1[Usa companyId do manager]

    B -->|Outro role| F[ForbiddenException]

    D --> G[Busca colaborador por CPF]
    D1 --> G
    G --> H{CPF já existe?}
    H -->|Sim + possui User| H1[BadRequestException CPF já cadastrado]
    H -->|Sim + órfão| H2[updateOrphanEmployee]
    H -->|Não| I[viaCep.lookup + defaults de salário/horários]

    I --> J[Cria Employee novo]
    J --> K[employeeProvider.save]
    K --> L{faceImageBase64 enviada?}
    L -->|Não| M[Retorna Employee salvo]
    L -->|Sim| N[handleFaceRegistration]
    N --> O[Atualiza faceS3ObjectKey e salva]
    O --> M
```

## 2) `listEmployees(Boolean active)`

```mermaid
flowchart TD
    A[Início] --> B[getCompanyIdFromLoggedUser]
    B --> C{active é null?}
    C -->|Sim| D[findByCompanyId]
    C -->|Não| E[findByCompanyIdAndActive]
    D --> F[Retorna lista]
    E --> F
```

## 3) `getEmployee(UUID employeeId)`

```mermaid
flowchart TD
    A[Início] --> B[getCompanyIdFromLoggedUser]
    B --> C[Busca employee por ID]
    C --> D{Encontrou?}
    D -->|Não| E[ResourceNotFoundException]
    D -->|Sim| F{employee.companyId == manager.companyId?}
    F -->|Não| G[ResourceNotFoundException - não vazar existência em outra empresa]
    F -->|Sim| H[Retorna employee]
```

## 4) `updateEmployee(UUID id, UpdateEmployeeManagerRequest req)`

```mermaid
flowchart TD
    A[Início] --> B[getEmployee(id)]
    B --> C[Reconstrói Employee com merge req x atual]
    C --> D{req.address != null?}
    D -->|Sim| E[viaCep.lookup + withNumber]
    D -->|Não| F[Segue]
    E --> F
    F --> G{req.faceImageBase64 preenchida?}
    G -->|Sim| H[handleFaceRegistration]
    G -->|Não| I[Mantém chave S3 atual]
    H --> J[Atualiza faceS3ObjectKey]
    I --> J
    J --> K[employeeProvider.save]
```

## 5) `deleteEmployee(UUID id)`

```mermaid
flowchart TD
    A[Início] --> B[getEmployee(id)]
    B --> C[employeeProvider.deleteById]
    C --> D[Fim]
```

## 6) `getOwnProfile()`

```mermaid
flowchart TD
    A[Início] --> B[Obtém employeeId do token]
    B --> C[getEmployee(employeeId)]
    C --> D[userProvider.findByEmployeeId]
    D --> E{Encontrou user?}
    E -->|Não| F[ResourceNotFoundException]
    E -->|Sim| G[Retorna EmployeeProfile(employee + role)]
```

## 7) `updateOwnProfile(UpdateEmployeePartnerRequest req)`

```mermaid
flowchart TD
    A[Início] --> B[employeeId do token]
    B --> C[getEmployee(employeeId)]
    C --> D{req.address != null?}
    D -->|Sim| E[viaCep.lookup + withNumber]
    D -->|Não| F[Usa endereço atual]
    E --> G[withEmail/withPhone/withAddress]
    F --> G
    G --> H[employeeProvider.save]
```

## 8) `markMessagesAsSeen()`

```mermaid
flowchart TD
    A[Início] --> B[employeeId do token]
    B --> C[findById(employeeId)]
    C --> D{Encontrou?}
    D -->|Não| E[ResourceNotFoundException]
    D -->|Sim| F[withLastSeenMessageTimestamp(now)]
    F --> G[employeeProvider.save]
```

## 9) `cpfExists(String cpf)`

```mermaid
flowchart TD
    A[Início] --> B[employeeProvider.cpfExists(cpf)]
    B --> C[Retorna boolean]
```

## 10) `toggleActivate(UUID employeeId)`

```mermaid
flowchart TD
    A[Início] --> B[getEmployee(employeeId)]
    B --> C[newStatus = !employee.active]
    C --> D[withActive(newStatus)]
    D --> E[employeeProvider.save]
```

---

## Fluxos auxiliares importantes

### A) `handleFaceRegistration(...)`

```mermaid
flowchart TD
    A[Decode Base64] --> B[uploadFaceImage no S3]
    B --> C[indexFace no Rekognition]
    C --> D{faceId retornou?}
    D -->|Não| E[Deleta imagem nova + BadRequestException]
    D -->|Sim| F{existe imagem antiga?}
    F -->|Sim| G[Deleta imagem antiga]
    F -->|Não| H[Retorna nova S3 key]
    G --> H

    A -.Base64 inválido.-> I[BadRequestException + rollback de imagem nova]
    B -.falha runtime.-> J[RuntimeException + rollback de imagem nova]
    C -.falha runtime.-> J
```

### B) `getCompanyIdFromLoggedUser()`

```mermaid
flowchart TD
    A[employeeId do token] --> B[employeeProvider.findById]
    B --> C{Encontrou?}
    C -->|Não| D[ResourceNotFoundException]
    C -->|Sim| E[Retorna companyId]
```

### C) `updateOrphanEmployee(...)`

```mermaid
flowchart TD
    A[viaCep.lookup + defaults] --> B[Reconstrói Employee com ID original]
    B --> C[employeeProvider.save]
    C --> D{faceImageBase64 enviada?}
    D -->|Não| E[Retorna employee salvo]
    D -->|Sim| F[handleFaceRegistration]
    F --> G[Atualiza faceS3ObjectKey + save]
    G --> E
```
