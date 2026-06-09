# Back-end Contract Guard — ÉPICO 4

## Objetivo

Registrar que o back-end na branch `new-ui` deve permanecer apenas como contrato de compatibilidade durante o ÉPICO 4.

## Permitido

- Rodar build.
- Rodar testes.
- Registrar falha real, se existir.

## Não fazer

- Não alterar controllers.
- Não alterar DTOs.
- Não alterar endpoints.
- Não alterar payloads.
- Não alterar entities.
- Não criar migrations.
- Não alterar regra de negócio.

## Validação

```bash
./gradlew test
./gradlew build
```

## Declaração esperada

```text
Back-end validado apenas como contrato. Nenhuma alteração necessária para o ÉPICO 4.
```