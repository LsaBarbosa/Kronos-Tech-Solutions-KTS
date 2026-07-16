# Skill: Backup PostgreSQL → S3 — Kronos

## Arquitetura do backup

```
cron (deploy, 02:00 BRT) → /home/deploy/scripts/backup-postgres.sh
  → pg_dump kronos_prod → comprime (gzip) → cifra (gpg opcional)
  → aws s3 cp --profile kronos-backup → s3://kronostechsolutions-backups-postgres/postgres/daily/
  → deleta arquivo local somente após confirmar upload
  → envia e-mail via SMTP (MAIL_* vars) em caso de falha
  → registra em /var/log/kronos/backup.log
```

## Variáveis de ambiente separadas

As credenciais de backup usam prefixo `BACKUP_AWS_*` para não colidir com as da aplicação:

| Variável | Uso |
|---|---|
| `BACKUP_AWS_ACCESS_KEY_ID` | IAM user `kronos-backup-agent` — só acesso ao bucket `kronostechsolutions-backups-postgres` |
| `BACKUP_AWS_SECRET_ACCESS_KEY` | Secret key do backup agent |
| `BACKUP_AWS_REGION` | Mesma região da aplicação (`us-east-1`) |
| `BACKUP_S3_BUCKET` | `kronostechsolutions-backups-postgres` |
| `BACKUP_RETENTION_DAILY` | Dias de retenção backup diário (padrão: 7) |
| `BACKUP_RETENTION_WEEKLY` | Semanas de retenção backup semanal (padrão: 4) |
| `BACKUP_RETENTION_MONTHLY` | Meses de retenção backup mensal (padrão: 12) |

## Localização dos scripts

| Script | Função |
|---|---|
| `/home/deploy/scripts/backup-postgres.sh` | Dump + upload S3 + notificação |
| `/home/deploy/scripts/backup-restore-test.sh` | Valida restore em banco temporário |

## Logs e monitoramento

```bash
# Log do backup
tail -50 /var/log/kronos/backup.log

# Verificar último backup
aws s3 ls s3://kronostechsolutions-backups-postgres/postgres/daily/ --profile kronos-backup | sort | tail -5

# Executar backup manual
/home/deploy/scripts/backup-postgres.sh

# Testar restore
/home/deploy/scripts/backup-restore-test.sh
```

## Estrutura de pastas no S3

```
s3://kronostechsolutions-backups-postgres/
└── postgres/
    ├── daily/     ← retenção 7 dias (lifecycle rule)
    ├── weekly/    ← retenção 4 semanas (lifecycle rule — toda segunda)
    └── monthly/   ← retenção 12 meses (lifecycle rule — todo dia 1)
```

## Permissões IAM mínimas (kronos-backup-agent)

```json
{
  "Effect": "Allow",
  "Action": ["s3:PutObject", "s3:GetObject", "s3:ListBucket", "s3:HeadObject"],
  "Resource": [
    "arn:aws:s3:::kronostechsolutions-backups-postgres",
    "arn:aws:s3:::kronostechsolutions-backups-postgres/*"
  ]
}
```
