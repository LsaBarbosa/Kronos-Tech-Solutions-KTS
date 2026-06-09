# TASKS — Back-end Gap Closure

## TASK-BE-GAP-001 — Preparar validação

- Confirmar branch `new-ui`.
- Ler `BACKEND_CONTRACT_GUARD.md`, `AGENTS.md` e `RULES.md`.

## TASK-BE-GAP-002 — Mapear contratos consumidos

- Mapear endpoints consumidos pelo front-end nos EPICs 01 a 07.
- Priorizar login, empresas, colaboradores, avisos, documentos e ponto.

## TASK-BE-GAP-003 — Validar usuários de acesso

- Confirmar se existe API para listagem/manutenção de usuários de acesso.
- Registrar bloqueio se não existir contrato suficiente.

## TASK-BE-GAP-004 — Executar validações

```bash
./gradlew test
./gradlew build
```

## TASK-BE-GAP-005 — Relatório de contrato

- Registrar contratos preservados.
- Registrar riscos.
- Registrar bloqueios para o front-end.
