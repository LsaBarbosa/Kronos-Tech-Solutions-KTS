# Subagent: Code Tracer

## Papel
Rastreia caminhos de execução no código-fonte Kronos para identificar onde exceções são lançadas
e como propagam até a resposta HTTP.

## Localização do código

```
src/main/java/com/kts/kronos/
├── adapter/in/web/http/DocumentController.java          # Entry point REST
├── adapter/in/web/exceptions/RestExceptionHandler.java  # Captura global de exceções
├── adapter/out/persistence/impl/FileScanningProviderImpl.java  # PONTO DE FALHA ATUAL
├── adapter/out/storage/S3BucketStorageProviderImpl.java  # Adapter S3 ativo
├── adapter/out/storage/S3DocumentBucketProperties.java   # Mapeamento type→bucket
├── adapter/out/persistence/impl/S3StorageProviderImpl.java  # Legado (Object Lock)
├── application/service/DocumentService.java             # Orchestrador do upload
├── config/AwsClientConfig.java                          # Bean S3Client + RekognitionClient
└── application/port/out/provider/FileScanningProvider.java  # Porta do antivírus
```

## Propagação de exceções em DocumentService.uploadDocumentInternal()

```
RuntimeException (FileScanningProviderImpl: ConnectException)
  → catch (RuntimeException e) em DocumentService
  → log.error("event=document_upload result=failure ... exception_type=RuntimeException")
  → throw e
  → RestExceptionHandler captura
  → HTTP 500 + "Falha ao validar a segurança do arquivo enviado."
```

## Tipos de exceção e HTTP resultante

| Exceção | Handler | HTTP |
|---|---|---|
| `BadRequestException` (arquivo malicioso) | RestExceptionHandler | 400 |
| `ForbiddenException` | RestExceptionHandler | 403 |
| `ResourceNotFoundException` | RestExceptionHandler | 404 |
| `IOException` (leitura do arquivo) | DocumentService → BadRequestException | 400 |
| `RuntimeException` (ClamAV/S3) | RestExceptionHandler | 500 |

## Grep úteis

```bash
# Encontrar onde FILE_SCAN_FAILED é definido
grep -r "FILE_SCAN_FAILED" src/

# Ver constante Messages
grep -r "FILE_SCAN_FAILED\|MALICIOUS_FILE" src/main/java/com/kts/kronos/constants/

# Ver todas as exceções lançadas por FileScanningProviderImpl
grep -n "throw" src/main/java/.../FileScanningProviderImpl.java

# Ver como DocumentService trata RuntimeException
grep -n -A3 "catch.*RuntimeException" src/main/java/.../DocumentService.java
```
