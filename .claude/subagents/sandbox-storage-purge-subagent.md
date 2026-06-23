# Subagent — Sandbox Storage and Purge

## Missão

Projetar e revisar a deleção física/lógica da sandbox.

## Pontos críticos

1. O purge não pode depender apenas do nome `Kronos Teste`.
2. O purge deve limpar resíduos parciais.
3. O purge deve ser reentrante e idempotente.
4. O purge não pode tocar empresas reais.
5. O purge deve remover storage local da sandbox.
6. O purge deve invalidar sessão/cache/rate-limit.

## Ordem de deleção sugerida

A ordem real deve respeitar FKs do banco, mas a estratégia conceitual é:

1. lock;
2. marcar sandbox como `PURGE_IN_PROGRESS`, se houver campo/status;
3. invalidar autenticação/sessão/cache;
4. coletar IDs sandbox:
   - companyIds;
   - userIds;
   - employeeIds;
   - documentIds;
   - requestIds;
   - pointRecordIds;
5. deletar tabelas de detalhe/documento/assinatura/consentimento;
6. deletar solicitações e aprovações;
7. deletar registros de ponto e derivados;
8. deletar vínculos usuário-empresa;
9. deletar employee;
10. deletar refresh/session;
11. deletar user;
12. deletar company;
13. deletar arquivos físicos;
14. validar;
15. auditar.

## Query strategy

Criar repositories/queries específicas para demo.

Evitar cascades cegos sem auditar contadores.

Preferir métodos explícitos:

```text
deleteDocumentsByCompanyId
deletePointRecordsByEmployeeIds
deleteVacationRequestsByEmployeeIds
deleteManualAdjustmentsByEmployeeIds
deleteEmployeesByCompanyId
deleteUserCompanyAccessByCompanyIdOrUserId
deleteRefreshTokensByUserIds
deleteUsersByUsername
deleteSandboxCompaniesBySandboxKey
```

## Validação

Após purge, retornar lista de issues.

Exemplo:

```json
{
  "clean": false,
  "issues": [
    {
      "type": "ORPHAN_DOCUMENT",
      "severity": "HIGH",
      "count": 2,
      "cleanupAttempted": true,
      "cleanupResult": "SUCCESS"
    }
  ]
}
```

## Arquivos

Usar caminho base:

```text
/opt/kronos/sandbox/kronos-teste
```

Remover somente se o path resolvido estiver dentro dessa raiz.

Nunca executar deleção com path vazio, `/`, `/opt/kronos`, `/opt/kronos/documents` ou qualquer path fora da raiz sandbox.
