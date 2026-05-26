# Documentação técnica e compliance — Kronos

Esta pasta centraliza documentos técnicos, operacionais e de conformidade do back-end Kronos.

Esta documentação apoia auditoria, operação e desenvolvimento, mas não substitui assessoria jurídica especializada.

## Índice atual

### Segurança

- [Session Policy](security/session-policy.md)
- [CSRF Protection Policy](security/csrf-policy.md)

### LGPD e documentos legais

- [LGPD Overview](legal/lgpd-overview.md)
- [Data Retention Policy](legal/data-retention.md)
- [Biometric Data Policy](legal/biometric-data-policy.md)
- [Data Subject Rights](legal/data-subject-rights.md)
- [LGPD Security Incident Response](legal/incident-response-lgpd.md)

### LGPD já versionado

- [Biometric Retention](lgpd/biometric-retention.md)
- [Retention Policy](lgpd/retention-policy.md)
- [Retention Policy Model](lgpd/retention-policy-model.md)
- [Retention Runbook](lgpd/retention-runbook.md)

### Operação e banco

- [Production Deployment](production/hostinger-deploy.md)
- [Database Migrations](database/migrations.md)
- [Pre-Production Checklist](../PRE_PRODUCTION_CHECKLIST.md)

## Inventário dos links locais do README principal

Os caminhos abaixo foram extraídos do `README.md` principal e formam a lista objetiva de arquivos obrigatórios mapeada na história `DOCS-001`.

### docs/security

- `docs/security/session-policy.md` — obrigatório, existente
- `docs/security/csrf-policy.md` — obrigatório, existente

### docs/legal

- `docs/legal/data-retention.md` — obrigatório, existente

### docs/production

- `docs/production/hostinger-deploy.md` — obrigatório, existente

### docs/database

- `docs/database/migrations.md` — obrigatório, existente

### Raiz do projeto

- `PRE_PRODUCTION_CHECKLIST.md` — obrigatório, existente

## Estrutura base criada neste épico

- `docs/security/`
- `docs/legal/`
- `docs/production/`
- `docs/database/`
- `docs/lgpd/`

## Próximos documentos previstos no backlog

- Validação documental automática por `scripts/check-doc-links.sh`
- Revisões incrementais dos documentos conforme evolução de segurança, LGPD e operação
