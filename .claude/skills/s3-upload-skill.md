# Skill: S3 Upload Debug

## Propósito
Diagnosticar e corrigir erros no fluxo de upload de documentos para AWS S3.

## Caminho crítico de upload (PAYSLIP)

```
POST /documents?type=PAYSLIP
  → DocumentController.upload()
  → DocumentService.uploadDocumentInternal()
    1. validateAndPrepareUpload()
       → fileScanningProvider.scanOrThrow()        ← PONTO DE FALHA ATUAL (ClamAV desconectado)
       → MagicBytesValidator, FileExtensionValidator, FileSizeValidator
    2. bucketStorageProvider.uploadFile(type, key, data, mime)
       → S3BucketStorageProviderImpl (@ConditionalOnProperty(s3))
       → S3DocumentBucketProperties.bucketFor(PAYSLIP) → AWS_S3_BUCKET_PAYSLIP
       → PutObjectRequest + ServerSideEncryption.AES256
    3. documentRepository.save(document)
    4. auditService.register(DOCUMENT_UPLOAD)
```

## Adapter ativo vs legado

| Adapter | Bean | Condição | Bucket |
|---|---|---|---|
| `S3BucketStorageProviderImpl` | `bucketStorageProvider` | `kronos.storage.provider=s3` | Multi-bucket por DocumentType |
| `S3StorageProviderImpl` | legado | incondicionalmente presente | Single bucket `AWS_S3_BUCKET_NAME_DOC` com Object Lock |

## Variáveis de ambiente relevantes

```bash
# Antivírus (causa do 500 atual)
UPLOAD_ANTIVIRUS_ENABLED        # true → tenta TCP localhost:3310 (ClamAV)

# S3
AWS_S3_BUCKET_PAYSLIP           # bucket de holerites
AWS_S3_BUCKET_NAME_DOC          # bucket legado (atenção: yml espera NAME_DOCS com S)
AWS_REGION                      # região — critica para S3Client.builder()
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
```

## Atenção: inconsistência de variável legada

- `application.yml`: `bucket-name-doc: ${AWS_S3_BUCKET_NAME_DOCS:kronos-docs-legal-prod}`
- `/etc/kronos/kronos.env`: `AWS_S3_BUCKET_NAME_DOC=...` (sem "S" final)
- Isso faz `S3StorageProviderImpl` usar o valor default (`kronos-docs-legal-prod`) em vez do env real.
- `S3BucketStorageProviderImpl` NÃO usa `bucket-name-doc` — impacto zero no fluxo ativo.

## Diagnóstico rápido

```bash
# 1. Verificar se antivírus está causando o erro
grep "document_upload_scan" /var/log/kronos/backend.log | tail -5

# 2. Verificar ClamAV
systemctl status clamav-daemon
ss -tlnp | grep 3310

# 3. Testar S3 (requer AWS CLI instalada)
aws s3 ls s3://<AWS_S3_BUCKET_PAYSLIP> 2>&1 | head -5
```
