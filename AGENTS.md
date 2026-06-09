# AGENTS.md — Kronos Back-end — EPIC 6

## Branch

`new-ui`

## Escopo

O EPIC 6 implementa telas administrativas no front-end.

No back-end, atue como guardião de contrato.

## Áreas relacionadas

- Empresas.
- Colaboradores.
- Usuários de acesso.
- Administradores.
- Roles e permissões.

## Leia antes de qualquer ação

```text
docs/new-ui/epic-6/backend-contract-guard-epic-6.md
.cursor/rules/600-kronos-epic-6-backend-contract.mdc
```

## Permitido

- Validar endpoints existentes usados pelo front-end administrativo.
- Validar payloads atuais.
- Rodar testes e build.
- Documentar incompatibilidade real se encontrada.

## Não fazer neste EPIC sem decisão explícita

- Criar endpoint novo.
- Alterar DTO.
- Alterar payload.
- Alterar entity.
- Criar migration.
- Alterar autenticação.
- Alterar autorização.
- Alterar roles.
- Alterar regra de negócio.

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
