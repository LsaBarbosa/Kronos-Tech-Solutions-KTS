# AGENTS.md — Kronos Back-end — ÉPICO 1

## Escopo

Este arquivo orienta agentes no repositório back-end `Kronos-Tech-Solutions-KTS`.

Branch obrigatória:

```text
new-ui
```

## Regra principal

O ÉPICO 1 é um épico de Design System no front-end.

No back-end, o papel deste repositório é apenas servir como contrato de compatibilidade.

## Permitido

- Validar build.
- Validar testes.
- Conferir que endpoints continuam compatíveis.
- Documentar eventual incompatibilidade real.

## Proibido neste épico

Não alterar sem justificativa formal:

- controllers;
- DTOs;
- payloads;
- endpoints;
- entities;
- migrations;
- autenticação;
- autorização;
- regras de negócio;
- schedulers;
- integrações externas.

## Exceção

Uma alteração mínima só é aceitável se:

1. a branch `new-ui` estiver quebrada;
2. a falha bloquear validação do ÉPICO 1;
3. a correção não mudar contrato funcional;
4. o motivo ficar documentado.

## Validação

```bash
./gradlew test
./gradlew build
```

## Declaração esperada

```text
Back-end validado apenas como contrato. Nenhuma alteração necessária para o ÉPICO 1.
```
