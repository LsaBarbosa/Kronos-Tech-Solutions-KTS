# Back-end Contract Guard — ÉPICO 1

## Objetivo

Registrar que o back-end na branch `new-ui` deve permanecer como contrato de compatibilidade durante o ÉPICO 1.

## Escopo

O ÉPICO 1 é uma entrega de Design System do front-end.

No back-end, execute apenas validação.

## Não alterar

- controllers;
- DTOs;
- endpoints;
- entities;
- migrations;
- autenticação;
- autorização;
- regras de negócio;
- rotinas agendadas;
- integrações externas.

## Permitido

- Rodar `./gradlew test`.
- Rodar `./gradlew build`.
- Registrar falha pré-existente, se houver.
- Corrigir apenas erro bloqueador comprovado sem alterar contrato.

## Declaração para PR

```text
Back-end validado apenas como contrato. Nenhuma alteração necessária para o ÉPICO 1.
```

## Checklist

- [ ] Branch `new-ui` confirmada.
- [ ] Nenhum endpoint alterado.
- [ ] Nenhum DTO alterado.
- [ ] Nenhuma entity alterada.
- [ ] Nenhuma migration criada.
- [ ] Nenhuma regra de negócio alterada.
- [ ] Build/testes executados quando aplicável.
