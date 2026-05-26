# Politica de Retencao LGPD

## Regras tecnicas

- Politicas `TIME_BASED` exigem `retentionDays > 0`.
- Politicas `CONSENT_BASED` e `LEGAL_HOLD` podem usar `retentionDays = null`.
- `retentionDays = -1` e proibido.
- Toda politica ativa precisa declarar `resourceType`, `policyType`, `action` e processor compativel.
- `DRY_RUN` nunca modifica dados.
- `APPLY` so executa com `kronos.lgpd.retention.allow-apply=true`.

## Catalogo ativo

- O catalogo ativo fica em `RetentionPolicyCatalog`.
- Politicas sem processor dedicado permanecem cadastradas, mas inativas.
- Politicas biometricas com consentimento ativo usam `PRESERVE_WHILE_CONSENT_ACTIVE`.

## Exigencias administrativas

- `APPLY` manual exige justificativa e confirmacao explicita.
- Toda execucao em lote gera auditoria agregada.
- A auditoria por politica continua sendo gerada pelo `RetentionPolicyExecutor`.
