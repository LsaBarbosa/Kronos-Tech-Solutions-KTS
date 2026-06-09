# AGENTS.md — Kronos Back-end — EPIC 5

## Branch

`new-ui`

## Escopo

O EPIC 5 implementa dashboard e registro de ponto no front-end.

No back-end, atue como guardiao de contrato.

## Permitido

- Validar endpoints existentes usados pelo dashboard.
- Validar endpoints existentes usados pelo fluxo de ponto.
- Rodar testes e build.
- Documentar incompatibilidade real se encontrada.

## Proibido sem necessidade comprovada

- Alterar controllers.
- Alterar DTOs.
- Alterar payloads.
- Alterar regras de ponto.
- Alterar entidades.
- Criar migrations.
- Alterar autenticacao ou autorizacao.

## Excecao

Alteracao minima so e aceitavel se o front-end estiver bloqueado por falha real de contrato e a correcao for documentada.

## Validacao

Rodar `./gradlew test` e `./gradlew build` quando aplicavel.
