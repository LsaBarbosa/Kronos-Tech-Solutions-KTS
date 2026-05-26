# Modelo de Politicas de Retencao LGPD

## Regras vigentes

- `RetentionPolicyType.TIME_BASED` exige `retentionDays > 0`.
- `RetentionPolicyType.CONSENT_BASED` pode operar com `retentionDays = null`.
- `RetentionPolicyType.LEGAL_HOLD` pode operar com `retentionDays = null`.
- Valores magicos como `retentionDays = -1` nao sao permitidos.
- Politicas ativas precisam declarar `resourceType`, `action` e um tipo executavel pelo catalogo atual.

## Catalogo ativo na Sprint 1

- Politicas biometricas por consentimento usam `PRESERVE_WHILE_CONSENT_ACTIVE`.
- Politicas temporais usam `resourceType` direto no catalogo, sem `switch` textual posterior.
- Politicas de `TIME_RECORD` e `EMPLOYEE_CONTRACT` permanecem cadastradas, mas inativas, ate a entrega dos processors dedicados.

## Garantias operacionais

- `DRY_RUN` continua sem modificar dados.
- `APPLY` continua bloqueado sem `kronos.lgpd.retention.allow-apply=true`.
- Flags `preserveLaborData` e `preserveFiscalData` ficam declaradas por politica no proprio catalogo.
