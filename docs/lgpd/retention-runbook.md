# Runbook de Execucao de Retencao

## Scheduler

- Habilitar com `LGPD_RETENTION_SCHEDULER_ENABLED=true`.
- Definir modo com `LGPD_RETENTION_SCHEDULER_MODE=DRY_RUN` ou `APPLY`.
- Em producao, `DRY_RUN` e o modo padrao seguro e nao impede startup.
- Para scheduler em `APPLY`, configurar tambem:
- `LGPD_RETENTION_ALLOW_APPLY=true`
- `LGPD_RETENTION_SCHEDULER_APPLY_CONFIRMED=true`
- `LGPD_RETENTION_SCHEDULER_JUSTIFICATION=<motivo operacional>`
- Antes de habilitar `APPLY`, validar se todas as politicas ativas possuem processor com `supportsApply() = true`.

## Endpoints manuais

- `POST /admin/retention/policies/{policyCode}/dry-run`
- `POST /admin/retention/policies/{policyCode}/apply`

## Payload obrigatorio para APPLY manual

```json
{
  "justification": "Atendimento a janela operacional aprovada",
  "confirmed": true
}
```

## Validacoes esperadas

- Sem `confirmed=true`, o endpoint de `APPLY` responde com erro de validacao.
- Sem `allow-apply=true`, o executor retorna `BLOCKED`.
- Se o processor nao suportar `APPLY`, o executor retorna `BLOCKED`.
- Politicas de `TIME_RECORD` e `EMPLOYEE_CONTRACT` permanecem em preservacao legal e nao devem ser aplicadas automaticamente.
- O lote registra auditoria agregada com modo, trigger, justificativa e totais.
