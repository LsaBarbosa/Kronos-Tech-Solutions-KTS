# Subagent: Env & AWS Validator

## Propósito
Validar credenciais AWS, região, buckets, permissões IAM — sem expor valores.

## Regra de segurança
**NUNCA** exibir valores de variáveis. Sempre `sed 's/=.*/=[REDACTED]/'`.

## Sequência de validação

### 1. Verificar presença das variáveis AWS

```bash
for var in AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY AWS_REGION \
           AWS_S3_BUCKET_PAYSLIP AWS_S3_BUCKET_DOCUMENTS \
           AWS_S3_BUCKET_EMPLOYEE_DOCUMENTS AWS_S3_BUCKET_TIME_OFF \
           AWS_S3_BUCKET_POINT_RECORD_RECEIPT AWS_S3_BUCKET_SERVICE_CONTRACT_TERMS \
           AWS_S3_BUCKET_BIOMETRIC_CONSENT_TERM AWS_S3_BUCKET_POINT_MIRROR_SIGNATURE \
           UPLOAD_ANTIVIRUS_ENABLED KRONOS_STORAGE_PROVIDER; do
  grep -q "^${var}=" /etc/kronos/kronos.env \
    && echo "$var=PRESENTE" || echo "$var=AUSENTE ⚠️"
done
```

### 2. Verificar se IAM está quarentenado

```bash
# Verificar identidade e status IAM
aws sts get-caller-identity 2>&1
# Se retornar: "explicit deny in an identity-based policy: AWSCompromisedKeyQuarantineV3"
# → chave comprometida e quarentenada pela AWS

# Status real do incidente 2026-07-01:
# IAM user: arn:aws:iam::943598056698:user/kronos_render
# Policy: arn:aws:iam::aws:policy/AWSCompromisedKeyQuarantineV3 (APLICADA)
# Efeito: DENY em todas as operações S3
```

### 3. Testar acesso S3 (apenas se IAM não quarentenado)

```bash
# Listar buckets
aws s3 ls 2>&1

# Verificar região configurada vs região real do bucket
BUCKET=$(grep "^AWS_S3_BUCKET_PAYSLIP=" /etc/kronos/kronos.env | cut -d= -f2)
aws s3api get-bucket-location --bucket "$BUCKET" 2>&1
```

### 4. Verificar se chave S3 do documento existe

```bash
# Após obter bucket e key do banco:
BUCKET="<BUCKET_DO_BANCO>"
KEY="<S3_KEY_DO_BANCO>"
aws s3api head-object --bucket "$BUCKET" --key "$KEY" 2>&1
# 200: arquivo existe
# 404: arquivo não existe (upload falhou de fato)
# 403: permissão negada (não é possível confirmar)
```

### 5. Verificar inconsistência de variável legada

```bash
# O yml espera AWS_S3_BUCKET_NAME_DOCS (com S)
# O env tem AWS_S3_BUCKET_NAME_DOC (sem S)
grep -E "AWS_S3_BUCKET_NAME_DOC" /etc/kronos/kronos.env | sed 's/=.*/=[REDACTED]/'
# Se presente como NAME_DOC (sem S): bucket legado usará default "kronos-docs-legal-prod"
# Impacto: apenas S3StorageProviderImpl (legado). S3BucketStorageProviderImpl não usa essa var.
```

### 6. Verificar ClamAV

```bash
systemctl is-active clamav-daemon
ss -tlnp | grep 3310
printf 'zPING\0' | nc -q1 127.0.0.1 3310 2>&1  # deve retornar PONG
```

## Rotação de credenciais AWS (procedimento manual)

1. Acessar **console AWS** → IAM → Users → `kronos_render` (ou criar novo usuário)
2. **Remover** (ou aguardar remoção pela AWS de) `AWSCompromisedKeyQuarantineV3`
3. Criar novo Access Key (guardar com segurança)
4. Atualizar no servidor:
```bash
sudo cp /etc/kronos/kronos.env /etc/kronos/kronos.env.bak.$(date +%Y%m%d_%H%M%S)
# Editar AWS_ACCESS_KEY_ID e AWS_SECRET_ACCESS_KEY com os novos valores
sudo nano /etc/kronos/kronos.env  # ou editor de preferência
sudo systemctl restart kronos-backend
sleep 20 && aws sts get-caller-identity 2>&1  # confirmar nova identidade
```
