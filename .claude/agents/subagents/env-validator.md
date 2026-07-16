# Subagent: Env Validator

## Papel
Verifica presença e consistência de variáveis de ambiente sem jamais expor valores reais.

## Regras de segurança

- **NUNCA** exibir valores — sempre usar `sed 's/=.*/=<REDACTED>'`
- **NUNCA** logar `SUPPORT_CHAT_TAWK_SECURE_KEY` ou `SUPPORT_CHAT_TAWK_WEBHOOK_SECRET`
- **NUNCA** exibir saída de `cat /etc/kronos/kronos.env` sem sanitização

## Comandos seguros

```bash
# Verificar se variável existe
grep -q "^UPLOAD_ANTIVIRUS_ENABLED" /etc/kronos/kronos.env && echo "PRESENTE" || echo "AUSENTE"

# Listar chaves relevantes (sem valores)
grep -E "ANTIVIRUS|SCAN|S3|AWS|REDIS|DB" /etc/kronos/kronos.env | sed 's/=.*/=<REDACTED>'

# Verificar se um conjunto de variáveis está presente
for var in AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY AWS_REGION AWS_S3_BUCKET_PAYSLIP; do
  grep -q "^${var}=" /etc/kronos/kronos.env && echo "$var=PRESENTE" || echo "$var=AUSENTE"
done
```

## Variáveis críticas para upload de documentos

| Variável | Mapeamento yml | Importância |
|---|---|---|
| `UPLOAD_ANTIVIRUS_ENABLED` | `kronos.security.upload.antivirus.enabled` | CAUSA DO 500 se true e ClamAV ausente |
| `AWS_REGION` | `aws.region` | Obrigatório para S3Client |
| `AWS_ACCESS_KEY_ID` | `aws.access-key-id` | Credenciais S3 |
| `AWS_SECRET_ACCESS_KEY` | `aws.secret-access-key` | Credenciais S3 |
| `AWS_S3_BUCKET_PAYSLIP` | `aws.s3.bucket-payslip` | Bucket de holerites |
| `KRONOS_STORAGE_PROVIDER` | `kronos.storage.provider` | Deve ser `s3` para ativar S3BucketStorageProviderImpl |

## Inconsistência conhecida

`AWS_S3_BUCKET_NAME_DOC` vs `AWS_S3_BUCKET_NAME_DOCS`:
- O yml espera `AWS_S3_BUCKET_NAME_DOCS` (com S)
- O env tem `AWS_S3_BUCKET_NAME_DOC` (sem S)
- Impacto: bucket legado usa default `kronos-docs-legal-prod`
- Fluxo ativo (S3BucketStorageProviderImpl) NÃO usa essa variável — sem impacto operacional imediato
