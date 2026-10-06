# Rules: Backup Seguro — Kronos

## Regra 1: Nunca sobrescrever credenciais da aplicação

Usar exclusivamente variáveis `BACKUP_AWS_*` e profile `--profile kronos-backup`.
Jamais usar `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` (reservadas para a aplicação).

## Regra 2: S3 confirmado antes de deletar arquivo local

```bash
# ✅ CORRETO
aws s3 cp "$DUMP_FILE" "s3://$BUCKET/postgres/daily/$FILENAME" --profile kronos-backup
if aws s3api head-object --bucket "$BUCKET" --key "postgres/daily/$FILENAME" --profile kronos-backup >/dev/null 2>&1; then
    rm -f "$DUMP_FILE"
else
    log_error "Upload não confirmado — arquivo local mantido"
    exit 1
fi

# ❌ ERRADO
aws s3 cp ... && rm -f "$DUMP_FILE"  # o && não garante que o objeto existe no S3
```

## Regra 3: Exit code diferente de 0 = falha + notificação

O script deve retornar exit 1 em qualquer falha e enviar e-mail via SMTP.
Nunca silenciar erros com `|| true` em operações críticas.

## Regra 4: Credenciais nunca no log

```bash
# ❌ ERRADO
echo "Usando key: $BACKUP_AWS_ACCESS_KEY_ID"

# ✅ CORRETO
log_info "Iniciando backup com profile kronos-backup"
```

## Regra 5: Arquivo de dump deve ter permissão restrita

```bash
touch "$DUMP_FILE"
chmod 600 "$DUMP_FILE"
pg_dump ... > "$DUMP_FILE"
```

## Regra 6: Restore testado periodicamente

O script `backup-restore-test.sh` deve ser executado ao menos mensalmente.
Restaura em banco temporário `kronos_restore_test` e verifica contagens de tabela.
