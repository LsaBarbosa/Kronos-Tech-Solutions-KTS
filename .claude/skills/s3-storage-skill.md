# Skill: S3 Upload/Download Debug — Kronos

## Arquitetura de storage (hexagonal)

```
adapter/in/web/http/DocumentController.java       ← recebe multipart/form-data, delega ao UseCase
application/service/DocumentService.java          ← orquestra: scan → upload S3 → persiste metadados
application/port/out/provider/
  BucketStorageProvider.java                      ← porta de saída para storage
  FileScanningProvider.java                       ← porta de saída para antivírus
adapter/out/storage/
  S3BucketStorageProviderImpl.java                ← ATIVO (kronos.storage.provider=s3) multi-bucket
adapter/out/persistence/impl/
  BucketStorageProviderImpl.java                  ← LOCAL (kronos.storage.provider=local) disco
  S3StorageProviderImpl.java                      ← LEGADO single-bucket + Object Lock
  FileScanningProviderImpl.java                   ← ClamAV TCP 127.0.0.1:3310
config/AwsClientConfig.java                       ← Bean S3Client: região + StaticCredentials
adapter/out/storage/S3DocumentBucketProperties.java ← @ConfigurationProperties(prefix="aws.s3")
```

## Mapeamento DocumentType → bucket

| DocumentType | Propriedade yml | Env var |
|---|---|---|
| PAYSLIP | `aws.s3.bucket-payslip` | `AWS_S3_BUCKET_PAYSLIP` |
| TIME_OFF | `aws.s3.bucket-time-off` | `AWS_S3_BUCKET_TIME_OFF` |
| DOCUMENTS | `aws.s3.bucket-documents` | `AWS_S3_BUCKET_DOCUMENTS` |
| EMPLOYEE_DOCUMENTS | `aws.s3.bucket-employee-documents` | `AWS_S3_BUCKET_EMPLOYEE_DOCUMENTS` |
| POINT_RECORD_RECEIPT | `aws.s3.bucket-point-record-receipt` | `AWS_S3_BUCKET_POINT_RECORD_RECEIPT` |
| BIOMETRIC_CONSENT_TERM | `aws.s3.bucket-biometric-consent-term` | `AWS_S3_BUCKET_BIOMETRIC_CONSENT_TERM` |
| SERVICE_CONTRACT_TERMS | `aws.s3.bucket-service-contract-terms` | `AWS_S3_BUCKET_SERVICE_CONTRACT_TERMS` |
| POINT_MIRROR_SIGNATURE | `aws.s3.bucket-point-mirror-signature` | `AWS_S3_BUCKET_POINT_MIRROR_SIGNATURE` |

Formato da chave S3: `company/{companyId}/employee/{employeeId}/{TYPE}/{year}/{month}/{uuid}-{filename}`

## SDK AWS configurado

- **Versão**: BOM `software.amazon.awssdk:bom:2.25.10`
- **Cliente**: `S3Client` via `AwsClientConfig.s3Client()` (bean compartilhado)
- **Região**: `${aws.region}` → `AWS_REGION`
- **Credenciais**: `StaticCredentialsProvider` se `AWS_ACCESS_KEY_ID` + `AWS_SECRET_ACCESS_KEY` presentes; fallback para DefaultCredentialsChain

## ⚠️ INCIDENTE ATIVO (2026-07-01): IAM quarentenado

O IAM user `kronos_render` (ARN: `arn:aws:iam::943598056698:user/kronos_render`) está com a policy
`AWSCompromisedKeyQuarantineV3` aplicada automaticamente pela AWS. Esta policy é aplicada quando
a AWS detecta credenciais comprometidas em repositórios públicos (ver AUDITORIA_SEGURANCA_20260629
item CRIT-001: chave `AKIA5XMWTWT5HGX5KEV3` commitada no histórico git).

**Efeito**: TODAS as operações S3 retornam HTTP 403 com `explicit deny in an identity-based policy`.

**Solução**: Acessar o console AWS → IAM → criar novo usuário com políticas corretas (S3 CRUD nos
buckets Kronos) → gerar novo par de chaves → atualizar `AWS_ACCESS_KEY_ID` e `AWS_SECRET_ACCESS_KEY`
em `/etc/kronos/kronos.env` → reiniciar backend.

## Checklist: erro "Node cannot be found in the current page" (SDK AWS v2)

Esse erro ocorre quando o SDK AWS v2 não consegue parsear a resposta XML da AWS:
- [ ] Região incorreta (request vai para endpoint errado → resposta inesperada)
- [ ] Bucket em região diferente da configurada (`AWS_REGION`)
- [ ] Credenciais quarentenadas ou inválidas (resposta de erro mal formada para o parser)
- [ ] Endpoint customizado configurado incorretamente
- [ ] Versão do SDK incompatível com resposta da AWS (raro)

**Diagnóstico**: Verificar `AWS_REGION` vs `aws s3api get-bucket-location --bucket <BUCKET>`.

## Checklist: mensagem de exceção vazia em catch de S3Exception

Ocorre quando `e.getMessage()` retorna null e o código concatena diretamente:

```java
// ❌ BUGADO — em DocumentService.java linha 138
throw new BadRequestException(ERROR_GET_FILE);
// ERROR_GET_FILE = "Falha ao buscar o arquivo no storage: " (sem concatenar a mensagem)

// ❌ TAMBÉM BUGADO — caso existisse concatenação
throw new BadRequestException(ERROR_GET_FILE + e.getMessage()); // getMessage() pode ser null
```

`S3Exception` AWS SDK v2 pode ter `getMessage()` não-null mas conter apenas o HTTP status sem
detalhe quando a policy bloqueia antes do parsing completo da resposta.

## Padrão correto de tratamento S3 (Java)

```java
// Em S3BucketStorageProviderImpl — catch de SdkException genérico
} catch (SdkException e) {
    String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    log.error("event=s3_error operation={} detail={} awsRequestId={}",
        operation, detail,
        (e instanceof S3Exception s3e) ? s3e.requestId() : "N/A", e);
    throw new RuntimeException("Erro de comunicação com o S3: " + detail, e);
}

// Em DocumentService — catch de RuntimeException no download
} catch (RuntimeException e) {
    String detail = e.getMessage() != null ? e.getMessage() : e.toString();
    log.error("event=document_download result=failure reason=storage_error message={}", detail, e);
    throw new BadRequestException(ERROR_GET_FILE + detail);
    // ou melhor: lançar 502/503 quando for erro de infraestrutura, não 400
}
```

## Verificar se chave existe no S3 sem fazer download

```bash
# head-object não baixa o arquivo
aws s3api head-object --bucket <BUCKET> --key <KEY> 2>&1
# Retorna 200 (metadados) se existe, 404 se não existe, 403 se sem permissão
```

## Testar conectividade S3

```bash
# Listar buckets (requer s3:ListAllMyBuckets)
aws s3 ls 2>&1

# Listar objetos em bucket específico (requer s3:ListBucket)
aws s3 ls s3://<AWS_S3_BUCKET_PAYSLIP>/ 2>&1 | head -10

# Verificar identidade IAM
aws sts get-caller-identity 2>&1

# Verificar região do bucket
aws s3api get-bucket-location --bucket <AWS_S3_BUCKET_PAYSLIP> 2>&1
```
