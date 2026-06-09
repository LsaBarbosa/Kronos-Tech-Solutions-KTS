# Backend Contract Guard — EPIC 5

## Objetivo

Garantir que o back-end da branch `new-ui` seja tratado como contrato de compatibilidade durante o EPIC 5.

## Contexto

O EPIC 5 implementa dashboard e registro de ponto no front-end.

## Permitido

- Validar endpoints existentes.
- Validar build.
- Validar testes.
- Documentar incompatibilidade real.

## Nao alterar sem necessidade comprovada

- controllers;
- DTOs;
- payloads;
- regras de ponto;
- entities;
- migrations;
- autenticacao;
- autorizacao.

## Checklist

- Branch `new-ui` confirmada.
- Nenhum contrato alterado.
- Nenhum endpoint criado sem necessidade.
- Nenhum payload alterado sem necessidade.
- Nenhuma migration criada.
- Regras de ponto preservadas.

## Declaracao final

```text
Back-end validado como contrato para o EPIC 5. Nenhuma alteracao funcional necessaria.
```
