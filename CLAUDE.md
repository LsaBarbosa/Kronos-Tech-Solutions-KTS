# CLAUDE.md — Kronos Back-end — EPIC 6

## Contexto

Repositório back-end Kronos na branch `new-ui`.

## Escopo atual

O EPIC 6 pertence às telas administrativas do front-end.

Neste back-end, atue como guardião de contrato.

## Leia primeiro

```text
AGENTS.md
docs/new-ui/epic-6/backend-contract-guard-epic-6.md
.cursor/rules/600-kronos-epic-6-backend-contract.mdc
```

## Não alterar sem decisão explícita

- controllers;
- DTOs;
- endpoints;
- entities;
- migrations;
- autenticação;
- autorização;
- roles;
- regras de negócio.

## Permitido

- Validar endpoints existentes.
- Validar build.
- Validar testes.
- Documentar incompatibilidade real.

## Validação

```bash
./gradlew test
./gradlew build
```

## Declaração esperada

```text
Back-end validado como contrato para o EPIC 6 — Gestão administrativa.
Nenhuma alteração contratual foi necessária.
```
