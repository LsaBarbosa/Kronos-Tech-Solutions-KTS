# Back-end Contract Guard — ÉPICO 2

## Objetivo

Registrar que o back-end na branch `new-ui` deve permanecer como contrato de compatibilidade durante o ÉPICO 2 — App Shell responsivo.

## Regra principal

O ÉPICO 2 cria shell, navegação e layout autenticado no front-end. O back-end não deve receber alteração funcional para essa entrega.

## Não alterar

- controllers;
- DTOs;
- endpoints;
- entities;
- migrations;
- autenticação;
- autorização;
- roles;
- filtros de segurança;
- regras de negócio;
- integrações externas.

## Permitido

- Rodar `./gradlew test`;
- rodar `./gradlew build`;
- registrar compatibilidade;
- documentar falha pré-existente, se houver.

## Exceção

Alteração mínima só é aceitável se:

1. a branch `new-ui` estiver quebrada;
2. o erro bloquear validação do EPIC 2;
3. a correção não alterar contrato funcional;
4. a decisão for documentada.

## Declaração esperada

```text
Back-end validado apenas como contrato. Nenhuma alteração necessária para o ÉPICO 2.
```

## Checklist

- [ ] Branch `new-ui` confirmada.
- [ ] Nenhum endpoint alterado.
- [ ] Nenhum DTO alterado.
- [ ] Nenhuma entity alterada.
- [ ] Nenhuma migration criada.
- [ ] Nenhuma regra de autenticação alterada.
- [ ] Nenhuma regra de autorização alterada.
- [ ] Build/testes executados quando aplicável.
