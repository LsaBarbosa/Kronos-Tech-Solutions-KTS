# EPIC 7 — Backend Contract Guard

## Branch

`new-ui`

## Objetivo

Manter o back-end como camada de compatibilidade para comunicação e documentos.

## Áreas

- Avisos.
- Documentos.
- Upload.
- Download.
- Exclusão.
- Permissões.

## Permitido

- Conferir endpoints existentes.
- Conferir payloads atuais.
- Rodar testes.
- Rodar build.
- Documentar incompatibilidade real.

## Não fazer sem decisão explícita

- Criar endpoint novo.
- Alterar DTO.
- Alterar payload.
- Alterar entity.
- Criar migration.
- Alterar regra de negócio.

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
