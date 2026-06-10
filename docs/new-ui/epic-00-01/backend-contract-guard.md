# Back-end Contract Guard — New UI Épicos 0 e 1

## Escopo

O back-end na branch `new-ui` atua apenas como guardião de contrato para os Épicos 0 e 1 do front-end.

## Regra central

Não alterar endpoints, DTOs, autenticação, migrations, segurança ou regras de negócio para resolver gaps documentais ou visuais do front-end.

## Permitido

- Validar que a branch `new-ui` compila.
- Rodar testes quando viável.
- Conferir se endpoints críticos já existem.
- Documentar incompatibilidade real encontrada.

## Proibido

- Criar endpoint novo para passar teste de front sem decisão explícita.
- Remover validação de segurança.
- Alterar LGPD/termos/biometria por demanda visual.
- Fazer migration sem demanda funcional confirmada.

## Comandos

```bash
git branch --show-current
git status --short
./gradlew test
./gradlew build
```

## Saída esperada

Criar ou atualizar:

```text
docs/new-ui/epic-00-01/backend-contract-validation.md
```

com status da validação e conclusão objetiva.
