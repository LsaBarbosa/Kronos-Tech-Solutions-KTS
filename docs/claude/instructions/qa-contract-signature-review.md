# QA — Revisão da Assinatura de Contratos

## Checklist

- Validar isolamento por tenant.
- Validar que colaborador acessa apenas atribuições próprias.
- Validar que gestor acessa apenas dados do próprio tenant.
- Validar upload apenas de PDF.
- Validar uso de `SERVICE_CONTRACT_TERMS` para o documento original.
- Validar bloqueio de conclusão duplicada.
- Validar que item concluído não aparece nas pendências.
- Validar auditoria minimizada.
- Validar build e testes.

## Regressões que não podem ocorrer

- Login e logout.
- Assinatura do espelho de ponto.
- Upload e download de documentos.
- Registro de ponto.
- Rotas existentes do front-end.
- Fluxos LGPD já existentes.
