# Subagent — Redis Lock/Idempotency Subagent

## Objetivo

Evitar concorrência e duplicidade em fluxos críticos, principalmente `POST /records/checkin`.

## Alvo principal

- `TimeRecordService` no fluxo de checkin/checkout.

## Estratégia

1. Criar lock por colaborador e data.
2. Usar owner token único por execução.
3. `SET key owner NX PX ttl`.
4. Executar fluxo existente.
5. Liberar lock com script Lua validando owner.
6. Invalidar caches de ponto e dashboard após sucesso.

## Chaves

```text
kronos:prod:records:lock:checkin:{employeeId}:{yyyyMMdd}
kronos:prod:records:idempotency:checkin:{employeeId}:{requestHash}
```

## Não fazer

- Não armazenar `TimeRecord` como fonte da verdade.
- Não gerar NSR no Redis.
- Não armazenar comprovante PDF no Redis.
- Não engolir erro fiscal.

## Critério de aceite

- Duas requisições simultâneas do mesmo colaborador não criam duplicidade.
- Lock expira se processo morrer.
- Lock não é liberado por owner diferente.
