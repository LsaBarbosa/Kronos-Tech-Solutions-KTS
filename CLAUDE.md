# CLAUDE.md — Kronos Back-end — EPIC 5

## Contexto

Repositorio back-end Kronos na branch `new-ui`.

## Escopo atual

O EPIC 5 pertence ao dashboard e registro de ponto do front-end.

Neste back-end, atue apenas como guardiao de contrato, salvo bloqueio real comprovado.

## Nao alterar sem justificativa

- controllers;
- DTOs;
- endpoints;
- entities;
- migrations;
- autenticacao;
- autorizacao;
- regras de ponto.

## Permitido

- Rodar testes.
- Rodar build.
- Conferir compatibilidade de endpoints existentes.
- Registrar ausencia de alteracao.

## Validacao

```bash
./gradlew test
./gradlew build
```

## Declaracao esperada

```text
Back-end validado como contrato para o EPIC 5. Nenhuma alteracao funcional necessaria.
```
