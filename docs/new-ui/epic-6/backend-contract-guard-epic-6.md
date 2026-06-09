# Backend Contract Guard — EPIC 6 — Gestão administrativa

## Objetivo

Orientar o uso do back-end durante o EPIC 6.

O EPIC 6 é uma entrega de telas administrativas no front-end. O back-end deve ser tratado como contrato existente, salvo bloqueio real comprovado.

## Branch

`new-ui`

## Áreas funcionais relacionadas

- Empresas.
- Colaboradores.
- Usuários de acesso.
- Administradores.
- Roles e permissões.

## Permitido

- Validar endpoints existentes usados pelas telas administrativas.
- Validar payloads atuais.
- Rodar testes e build.
- Documentar incompatibilidade real entre front e back.
- Corrigir erro bloqueador comprovado sem alterar contrato funcional.

## Proibido sem aprovação explícita

- Criar endpoint novo.
- Alterar DTO.
- Alterar payload.
- Alterar entity.
- Alterar migration.
- Alterar autenticação.
- Alterar autorização.
- Alterar regra de negócio de empresa, colaborador ou usuário.
- Alterar roles.

## Checklist

- [ ] Branch `new-ui` confirmada.
- [ ] Endpoints de empresa preservados.
- [ ] Endpoints de colaborador preservados.
- [ ] Endpoints de usuário preservados.
- [ ] DTOs preservados.
- [ ] Entities preservadas.
- [ ] Migrations não alteradas.
- [ ] Permissões preservadas.
- [ ] Build/testes executados quando aplicável.

## Declaração esperada

```text
Back-end validado como contrato para o EPIC 6 — Gestão administrativa.
Nenhuma alteração contratual foi necessária.
```
