# Rules: Tratamento de Erros — Upload de Documentos

## Regra 1: Serviços externos não devem derrubar o fluxo principal com 500

`FileScanningProviderImpl` deve suportar modo `fail-open` configurável:
- `kronos.security.upload.antivirus.fail-open=true` → log de WARN mas continua o upload
- `kronos.security.upload.antivirus.fail-open=false` (default) → bloqueia upload com 500

**Racional:** ClamAV indisponível não deve impedir upload em produção sem opção de degradação graciosa.

## Regra 2: ConnectException em serviço de suporte = WARN, não ERROR fatal

Quando um serviço de suporte (antivírus, cache) não está disponível:
- Logar em WARN com `reason=service_unavailable`
- Não lançar RuntimeException sem modo `fail-open`
- Distinguir "arquivo malicioso detectado" (BadRequest → 400) de "serviço indisponível" (se fail-open, continua; se não, 500)

## Regra 3: Nomenclatura de variáveis de ambiente

Novas variáveis de bucket S3 devem seguir o padrão: `AWS_S3_BUCKET_{TIPO}` e o yml deve espelhar exatamente: `${AWS_S3_BUCKET_{TIPO}:default-value}`.

Inconsistência identificada: `AWS_S3_BUCKET_NAME_DOC` vs `AWS_S3_BUCKET_NAME_DOCS` — ao corrigir, manter compatibilidade pois a variável antiga pode estar em uso.

## Regra 4: Dependência de infraestrutura → health check

Todo serviço externo que pode causar 500 deve ter health check em `/actuator/health`:
- ClamAV: indicator customizado em `ClamAvHealthIndicator`
- S3: via `S3HealthIndicator` (verificar se já existe)

## Regra 5: Exceção de antivírus vs exceção de S3

- **Antivírus indisponível:** `FileScanningProviderImpl` → `RuntimeException(FILE_SCAN_FAILED)` — mensagem diferente de S3
- **Erro S3:** `S3BucketStorageProviderImpl` → `RuntimeException("Erro ao enviar arquivo para o S3.", e)` — mensagem específica de S3

No log: `exception_type=ConnectException` indica antivírus; `exception_type=SdkException` indica S3.
