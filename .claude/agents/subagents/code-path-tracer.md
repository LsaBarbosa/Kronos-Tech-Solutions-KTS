# Subagent: Code Path Tracer

## Propósito
Rastrear fluxo completo de `POST /documents` e `GET /documents/{id}` do Controller ao adapter S3.

## Fluxo POST /documents (upload)

```
1. DocumentController.upload()
   src/main/java/com/kts/kronos/adapter/in/web/http/DocumentController.java
   → @PostMapping("/documents"), @PreAuthorize(ANY_EMPLOYEE)
   → useCase.uploadDocument(type, employeeId, file)

2. DocumentService.uploadDocumentInternal()
   src/main/java/com/kts/kronos/application/service/DocumentService.java
   → validateAndPrepareUpload():
       a. FileSizeValidator
       b. FileExtensionValidator (ALLOWED_EXTENSIONS = {pdf, jpg, jpeg, png})
       c. MagicBytesValidator
       d. fileScanningProvider.scanOrThrow(name, mime, bytes)  ← ClamAV TCP 3310
   → domainAuthorizationService.authorizeEmployeeAccess(employeeId)
   → bucketStorageProvider.uploadFile(type, storageKey, data, mime)  ← S3
   → documentRepository.save(document)
   → auditService.register(DOCUMENT_UPLOAD)

3. S3BucketStorageProviderImpl.uploadFile()
   src/main/java/com/kts/kronos/adapter/out/storage/S3BucketStorageProviderImpl.java
   → S3DocumentBucketProperties.bucketFor(type)  ← resolve bucket
   → S3Client.putObject(PutObjectRequest + ServerSideEncryption.AES256)
   → catch (SdkException e) → RuntimeException("Erro ao enviar arquivo para o S3.", e)

4. FileScanningProviderImpl.scanOrThrow()
   src/main/java/com/kts/kronos/adapter/out/persistence/impl/FileScanningProviderImpl.java
   → Socket.connect(localhost, 3310, timeout)
   → catch (IOException e) → RuntimeException(FILE_SCAN_FAILED, e)  ← ⚠️ PONTO DE FALHA 1
```

## Fluxo GET /documents/{id} (download)

```
1. DocumentController.download()
   → @GetMapping("/documents/{id}"), @PreAuthorize(ANY_EMPLOYEE)
   → useCase.downloadDocument(employeeId, documentId)

2. DocumentService.downloadDocument()
   DocumentService.java linha 79-140
   → domainAuthorizationService.authorizeDocumentAccess(documentId, employeeId)
   → bucketStorageProvider.downloadFile(doc.type(), doc.storagePath())
   → catch (ResourceNotFoundException) → ResourceNotFoundException(DOCUMENT_NOT_FOUND)
   → catch (RuntimeException e) linha 131-139:
       log.error(...)
       throw new BadRequestException(ERROR_GET_FILE)  ← ⚠️ PONTO DE FALHA 2
       // ERROR_GET_FILE = "Falha ao buscar o arquivo no storage: " (sem e.getMessage())

3. S3BucketStorageProviderImpl.downloadFile()
   S3BucketStorageProviderImpl.java linha 88+
   → s3Client.getObject(GetObjectRequest)
   → catch (NoSuchKeyException) → ResourceNotFoundException (404)
   → catch (S3Exception) status 404 → ResourceNotFoundException
   → catch (SdkException) → RuntimeException("Erro ao enviar arquivo para o S3.", e)
                             ← ⚠️ PONTO DE FALHA 3 (403 IAM quarantena cai aqui)
```

## Pontos de falha identificados

| ID | Arquivo | Linha | Descrição | Status |
|---|---|---|---|---|
| PF-1 | `FileScanningProviderImpl.java` | 79 | ClamAV TCP não disponível → IOException → RuntimeException | CORRIGIDO (ClamAV instalado) |
| PF-2 | `DocumentService.java` | 138 | `BadRequestException(ERROR_GET_FILE)` sem concatenar mensagem da exceção | PENDENTE correção código |
| PF-3 | `S3BucketStorageProviderImpl.java` | ~148 | IAM quarentenado → S3Exception 403 → cai no catch SdkException genérico → RuntimeException | PENDENTE rotação de credenciais |

## Grep úteis

```bash
BASE=/home/deploy/apps/Kronos-Tech-Solutions-KTS/src/main/java/com/kts/kronos

# Encontrar linha 138 do DocumentService
sed -n '130,145p' $BASE/application/service/DocumentService.java

# Ver tratamento de exceção S3 no download
grep -n "catch\|throw\|SdkException\|S3Exception\|NoSuchKey" \
  $BASE/adapter/out/storage/S3BucketStorageProviderImpl.java | head -20

# Verificar constante ERROR_GET_FILE
grep -n "ERROR_GET_FILE" $BASE/constants/Messages.java
grep -rn "ERROR_GET_FILE" $BASE/ --include="*.java"
```
