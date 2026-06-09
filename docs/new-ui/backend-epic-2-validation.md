# Back-end Validation — ÉPICO 2

## Objetivo

Manter o back-end como referência de compatibilidade enquanto o front-end implementa o App Shell responsivo.

## Regra

Não mudar código funcional do back-end para este épico.

## Não alterar

- controllers;
- DTOs;
- endpoints;
- entities;
- migrations;
- autenticação;
- autorização;
- regras de negócio.

## Validação

```bash
./gradlew test
./gradlew build
```

## Declaração esperada

```text
Back-end validado apenas como contrato. Nenhuma alteração necessária para o ÉPICO 2.
```
