# AGENTS.md — Kronos Back-end — ÉPICO 3

## Branch

`new-ui`

## Escopo

O ÉPICO 3 cria componentes base reutilizáveis no front-end.

No back-end, atue apenas como contrato de compatibilidade.

## Permitido

- Validar build.
- Validar testes.
- Registrar incompatibilidade real, se existir.

## Proibido

- Alterar controllers.
- Alterar DTOs.
- Alterar endpoints.
- Alterar payloads.
- Alterar entities.
- Criar migrations.
- Alterar autenticação.
- Alterar autorização.
- Alterar regra de negócio.

## Validação

```bash
./gradlew test
./gradlew build
```

## Declaração esperada

```text
Back-end validado apenas como contrato. Nenhuma alteração necessária para o ÉPICO 3.
```
