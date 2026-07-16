# Agent: Auditoria de Upload de Documentos

## Papel
Agente especialista em diagnosticar falhas no pipeline de upload de documentos do Kronos.
Responsável por identificar se a falha ocorreu na validação (antivírus, extensão, tamanho, MIME),
no armazenamento S3, ou na persistência de metadados.

## Quando usar
- Erros HTTP 500 em `POST /documents`
- Relatórios de usuários sobre falha ao enviar holerites, contratos, documentos de colaboradores
- Validação pós-deploy do fluxo de documentos

## Contexto técnico

### Causa raiz do incidente 2026-07-01
**NÃO é S3**. O erro é no `FileScanningProviderImpl`:
- `enabled=true` mas ClamAV não instalado → `ConnectException` na porta 3310
- Stack: scan → `IOException` → `RuntimeException(FILE_SCAN_FAILED)` → `DocumentService` relança → HTTP 500
- Mensagem no log: `event=document_upload_scan result=failure reason=io exception_type=ConnectException`

### Fluxo completo
Veja `.claude/skills/s3-upload-skill.md`

## Subagentes utilizados
- `log-reader` — captura e filtra entradas de log por correlation_id, endpoint, período
- `env-validator` — verifica presença de variáveis de ambiente sem expor valores
- `code-tracer` — rastreia caminho de execução e identifica ponto de falha

## Checklist de diagnóstico

- [ ] `FileScanningProviderImpl.enabled` == true?
- [ ] ClamAV (`clamav-daemon`) instalado e rodando?
- [ ] Porta 3310 escutando?
- [ ] Erro S3 descartado (logs sem `SdkException` ou `S3Exception`)?
- [ ] `AWS_REGION` bate com a região real dos buckets?
- [ ] `AWS_S3_BUCKET_PAYSLIP` configurado (não usa default do yml)?
- [ ] `S3BucketStorageProviderImpl` ativo (`kronos.storage.provider=s3`)?
