# AGENTS.md — Kronos Back-end — EPIC 7

## Branch

`new-ui`

## Escopo

O EPIC 7 implementa comunicação e documentos no front-end.

No back-end, atue como guardião de contrato.

## Áreas relacionadas

- Avisos.
- Criação de aviso.
- Documentos.
- Upload de documentos.
- Download de documentos.
- Exclusão de documentos.
- Permissões por perfil.

## Leia antes de qualquer ação

```text
docs/new-ui/epic-7/contract-guard.md
.cursor/rules/700-kronos-epic-7-backend-contract.mdc
```

## Permitido

- Validar endpoints existentes.
- Validar payloads atuais.
- Rodar testes e build.
- Documentar incompatibilidade real.

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
Back-end validado como contrato para o EPIC 7 — Comunicação e documentos.
Nenhuma alteração contratual foi necessária.
```
