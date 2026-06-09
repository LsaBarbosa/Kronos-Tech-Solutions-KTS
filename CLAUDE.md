# CLAUDE.md — Kronos Back-end — EPIC 7

## Contexto

Repositório back-end Kronos na branch `new-ui`.

## Escopo atual

O EPIC 7 pertence às telas de comunicação e documentos do front-end.

Neste back-end, atue como guardião de contrato.

## Leia primeiro

```text
AGENTS.md
docs/new-ui/epic-7/backend-contract-guard-epic-7.md
.cursor/rules/700-kronos-epic-7-backend-contract.mdc
```

## Regra central

Não altere contrato de API sem decisão explícita.

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
Back-end validado como contrato para o EPIC 7.
Nenhuma alteração contratual foi necessária.
```
