# CLAUDE.md — Kronos Back-end — ÉPICO 2

## Contexto

Repositório back-end Kronos na branch `new-ui`.

## Escopo atual

O ÉPICO 2 pertence ao App Shell responsivo do front-end. Neste back-end, atue apenas como guardião de contrato.

## Não alterar neste épico

- controllers;
- DTOs;
- endpoints;
- entities;
- migrations;
- segurança;
- autenticação;
- autorização;
- regras de negócio.

## Permitido

- Rodar testes e build.
- Confirmar compatibilidade.
- Registrar ausência de alteração.

## Validação

```bash
./gradlew test
./gradlew build
```

## Declaração esperada

```text
Back-end validado apenas como contrato. Nenhuma alteração necessária para o ÉPICO 2.
```
