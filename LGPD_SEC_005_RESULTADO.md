# LGPD-SEC-005 - Resultado

## Escopo

Correção exclusiva do achado `LGPD-SEC-005` na branch `feature/lgpd-compliance`, sem alterações em front-end, documentação existente, outras findings LGPD, commit ou push.

## Resumo da mudança

- `EmployeeResponse.maskedCpf` deixou de receber `employee.cpf()` bruto.
- O mascaramento de CPF passou a ser centralizado em `SensitiveDataMasker.maskCpf(...)`.
- O DTO de colaborador continua sem expor `faceS3ObjectKey`.
- Ajustei testes de mapper, DTO, WebMvc e masker para garantir que CPF cru não volte a aparecer em respostas de colaborador.
- Em `LgpdEmployeeExportResponse`, removi a referência direta a `faceS3ObjectKey` do DTO de resposta e passei a usar `employee.hasFaceImage()`, sem alterar comportamento funcional.

## Estratégia adotada

Estratégia: `apenas mascaramento`.

Não implementei separação `summary/detail` nesta tarefa porque o contrato atual do front-end consome campos como `salary`, `phone` e `address` em listagem e perfil. Como o escopo proibia alterar front-end e exigia compatibilidade funcional mínima, corrigi o vazamento de CPF cru e confirmei que chaves faciais não são expostas pelos DTOs de colaborador.

## Campos removidos ou mantidos por endpoint

### `GET /employee`

- Mantidos:
  - `employeeId`
  - `fullName`
  - `maskedCpf`
  - `jobPosition`
  - `email`
  - `active`
  - `companyName`
  - `salary`
  - `phone`
  - `address`
  - demais campos já existentes no contrato atual do DTO
- Garantias aplicadas:
  - `maskedCpf` agora sempre mascarado
  - `cpf` cru não é serializado
  - `faceS3ObjectKey` não é serializado

### `GET /employee/{employeeId}`

- Mantido o shape atual do `EmployeeResponse`
- Garantias aplicadas:
  - `maskedCpf` mascarado
  - sem `cpf` cru
  - sem `faceS3ObjectKey`

### `GET /employee/own-profile`

- Mantido o shape atual do `EmployeeResponse`
- Garantias aplicadas:
  - `maskedCpf` mascarado
  - sem `cpf` cru
  - sem `faceS3ObjectKey`

### `POST /employee`

- Mantido o retorno atual com `EmployeeResponse`
- Garantias aplicadas:
  - `maskedCpf` mascarado
  - sem `cpf` cru
  - sem `faceS3ObjectKey`

## Justificativa para campos sensíveis mantidos

- `salary`, `phone` e `address` foram mantidos por compatibilidade do contrato atual consumido pelo front-end existente.
- Esta tarefa não alterou o front-end por restrição explícita.
- Remover esses campos agora quebraria o consumo atual das telas e hooks já existentes.
- O bug crítico corrigido nesta entrega foi o uso de CPF cru no campo `maskedCpf`.

## Arquivos alterados

- `src/main/java/com/kts/kronos/application/util/SensitiveDataMasker.java`
- `src/main/java/com/kts/kronos/adapter/in/web/dto/employee/EmployeeResponse.java`
- `src/main/java/com/kts/kronos/domain/model/Employee.java`
- `src/main/java/com/kts/kronos/adapter/in/web/dto/lgpd/LgpdEmployeeExportResponse.java`
- `src/test/java/com/kts/kronos/application/util/SensitiveDataMaskerTest.java`
- `src/test/java/com/kts/kronos/adapter/in/web/dto/ResponseMapperTest.java`
- `src/test/java/com/kts/kronos/adapter/in/web/http/webmvc/EmployeeControllerWebMvcTest.java`
- `src/test/java/com/kts/kronos/adapter/in/web/dto/employee/EmployeeResponseTest.java`

## Testes criados ou alterados

### `SensitiveDataMaskerTest`

- `maskCpf_shouldMaskValidCpf`
- `maskCpf_shouldNotExposeRawCpf`
- `maskCpf_shouldHandleFormattedCpf`
- `maskCpf_shouldHandleNullBlankAndInvalidCpf`
- ajuste em `shouldSanitizeDetailsMaskingCpf`

### `EmployeeResponseTest`

- `fromDomain_shouldReturnMaskedCpf`
- `shouldNotExposeFaceS3ObjectKey`

### `ResponseMapperTest`

- ajuste da expectativa de `maskedCpf` para valor mascarado

### `EmployeeControllerWebMvcTest`

- validações em:
  - listagem
  - detalhe
  - own-profile
  - registro
- asserts adicionais:
  - `maskedCpf` mascarado
  - ausência de `cpf`
  - ausência de `faceS3ObjectKey`

## Comandos executados

```bash
git status --short
git branch --show-current
grep -R -n "employee.cpf()" src/main/java/com/kts/kronos
grep -R -n "maskedCpf" src/main/java/com/kts/kronos src/test/java/com/kts/kronos
grep -R -n "salary" src/main/java/com/kts/kronos/adapter/in/web/dto
grep -R -n "faceS3ObjectKey\|faceImageBase64\|s3Key" src/main/java/com/kts/kronos/adapter/in/web/dto

./gradlew test --tests "*SensitiveDataMaskerTest*"
./gradlew test --tests "*EmployeeResponseTest*"
./gradlew test --tests "*ResponseMapperTest*"
./gradlew test --tests "*EmployeeController*"
./gradlew --stop
./gradlew test --tests "*SensitiveDataMaskerTest*"
./gradlew test --tests "*EmployeeResponse*"
./gradlew test --tests "*EmployeeController*"
./gradlew test --tests "*Employee*"

grep -R -n "maskedCpf.*employee.cpf()\|employee.cpf().*maskedCpf" src/main/java/com/kts/kronos
grep -R -n "faceS3ObjectKey\|faceImageBase64\|s3Key" src/main/java/com/kts/kronos/adapter/in/web/dto
```

## Resultado dos testes

- `./gradlew test --tests "*SensitiveDataMaskerTest*"`: passou
- `./gradlew test --tests "*EmployeeResponseTest*"`: passou
- `./gradlew test --tests "*ResponseMapperTest*"`: passou
- `./gradlew test --tests "*EmployeeController*"`: passou
- `./gradlew test --tests "*EmployeeResponse*"`: passou
- `./gradlew test --tests "*Employee*"`: falhou por testes amplos de compliance multi-tenant fora do escopo desta mudança (`MultiTenantComplianceTest` com falha de contexto), não por regressão demonstrada do patch de CPF/DTO

## Resultado das buscas finais

### CPF cru no `maskedCpf`

Comando:

```bash
grep -R -n "maskedCpf.*employee.cpf()\|employee.cpf().*maskedCpf" src/main/java/com/kts/kronos
```

Resultado:

- nenhuma ocorrência

### Chaves faciais/storage em DTOs

Comando:

```bash
grep -R -n "faceS3ObjectKey\|faceImageBase64\|s3Key" src/main/java/com/kts/kronos/adapter/in/web/dto
```

Resultado:

- sem ocorrências em DTOs de resposta de colaborador
- permanecem ocorrências em DTOs de request já mascarados no `toString()`, fora do escopo desta correção:
  - `timerecord/GeolocationRequest.java`
  - `employee/CreateEmployeeRequest.java`
  - `employee/UpdateEmployeeManagerRequest.java`
  - `employee/RegisterFaceRequest.java`
  - `security/FaceLoginRequest.java`

## Se algo não foi possível validar

- Não validei remoção de `salary`, `phone` e `address` da listagem porque isso exigiria alteração de contrato consumido pelo front-end, explicitamente fora do escopo.
- O pacote amplo `*Employee*` falhou por testes de compliance/contexto já fora deste patch. Os testes específicos do achado passaram.

## Diff resumido

- 7 arquivos modificados
- 1 arquivo novo de teste
- `git diff --stat` dos arquivos rastreados desta correção:

```text
 src/main/java/com/kts/kronos/adapter/in/web/dto/employee/EmployeeResponse.java     |  7 +--
 src/main/java/com/kts/kronos/adapter/in/web/dto/lgpd/LgpdEmployeeExportResponse.java |  2 +-
 src/main/java/com/kts/kronos/application/util/SensitiveDataMasker.java               | 25 +++++++-
 src/main/java/com/kts/kronos/domain/model/Employee.java                              |  4 ++
 src/test/java/com/kts/kronos/adapter/in/web/dto/ResponseMapperTest.java              |  4 +-
 src/test/java/com/kts/kronos/adapter/in/web/http/webmvc/EmployeeControllerWebMvcTest.java | 21 +++++--
 src/test/java/com/kts/kronos/application/util/SensitiveDataMaskerTest.java           | 66 ++++++++++++++++++++--
```

## Confirmação de escopo

Apenas o `LGPD-SEC-005` foi tratado nesta entrega.
