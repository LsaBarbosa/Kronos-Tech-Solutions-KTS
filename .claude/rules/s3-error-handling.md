# Rules: Tratamento de Erros S3 — Kronos

## Regra 1: Nunca engolir mensagem de exceção S3

`S3Exception.getMessage()` pode retornar null (especialmente sob políticas de deny explícito).
Sempre usar `.toString()` ou verificar null:

```java
// ❌ BUGADO (atual em DocumentService.java linha 138)
throw new BadRequestException(ERROR_GET_FILE);
// Resultado: "Falha ao buscar o arquivo no storage: " — sem nenhuma informação útil

// ❌ TAMBÉM BUGADO
throw new BadRequestException(ERROR_GET_FILE + e.getMessage());
// Resultado: "Falha ao buscar o arquivo no storage: null" se getMessage() == null

// ✅ CORRETO
String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName() + ": " + e;
log.error("event=document_download result=failure reason=storage_error awsRequestId={}",
    (e.getCause() instanceof S3Exception s3e) ? s3e.requestId() : "N/A", e);
throw new BadRequestException(ERROR_GET_FILE + detail);
```

## Regra 2: Upload e registro no banco devem ser transacionais

O banco nunca deve persistir um `Document` se o upload S3 não ocorreu com sucesso.
Ordem correta:

```java
// ✅ S3 primeiro, banco depois (atual no DocumentService — VERIFICAR)
String storageKey = bucketStorageProvider.uploadFile(type, key, data, mime);
// Se uploadFile() lançar exceção, o save() abaixo NÃO é chamado
documentRepository.save(buildDocument(storageKey));
```

Se o banco persistir antes do S3 e o S3 falhar, o registro fica órfão e o download
retornará 400 com mensagem vazia (bug composto da Regra 1).

## Regra 3: Ao falhar upload, NÃO persistir no banco

Garantido pela Regra 2 (S3 primeiro). Se a ordem for invertida, adicionar compensação:

```java
var document = documentRepository.save(buildDocument(tentativeKey));
try {
    bucketStorageProvider.uploadFile(type, tentativeKey, data, mime);
} catch (Exception e) {
    documentRepository.delete(document);  // compensação
    throw e;
}
```

## Regra 4: Distinguir "arquivo não existe no S3" de "erro de IO"

```java
// Em S3BucketStorageProviderImpl.downloadFile():
} catch (NoSuchKeyException e) {
    throw new ResourceNotFoundException("Arquivo não encontrado no storage.");
    // → HTTP 404
} catch (S3Exception e) {
    if (e.statusCode() == 404) {
        throw new ResourceNotFoundException("Arquivo não encontrado no storage.");
        // → HTTP 404
    }
    if (e.statusCode() == 403) {
        // Pode ser quarentena ou permissão IAM ausente
        log.error("event=s3_access_denied awsRequestId={} detail={}", e.requestId(), e.getMessage());
        throw new RuntimeException("Acesso negado ao storage: " + e.getMessage(), e);
        // → HTTP 502 (erro de infraestrutura, não de input do usuário)
    }
    throw new RuntimeException("Erro de storage: " + e, e);
}
```

## Regra 5: Logar o S3 request ID em caso de erro

O `requestId` da AWS é essencial para acionar suporte e rastrear no CloudTrail:

```java
// ✅ Sempre logar requestId e extendedRequestId quando disponível
if (e instanceof S3Exception s3e) {
    log.error("event=s3_error operation={} status={} awsRequestId={} detail={}",
        operation, s3e.statusCode(), s3e.requestId(), s3e.getMessage(), e);
}
```

## Regra 6: HTTP 400 vs 502 para erros de storage

- **400 BadRequest**: erro causado por input do USUÁRIO (arquivo inválido, malicioso)
- **404 NotFound**: arquivo não existe no S3
- **502 BadGateway**: falha de infraestrutura (S3 inacessível, IAM, região errada)
- **500 InternalServerError**: bug inesperado

O código atual lança `BadRequestException` para erros de storage — **incorreto**.
Erros de infraestrutura devem retornar 502 ou 503, não 400.
