# RULES — Back-end Gap Closure

## Escopo

O back-end deve ser tratado como contrato de suporte para a branch new-ui.

## Permitido

- Comparar contratos.
- Rodar testes.
- Documentar bloqueios.
- Corrigir erro bloqueador somente se não mudar contrato funcional.

## Evitar

- Nova regra de negócio.
- Novo endpoint sem aprovação.
- Mudança de DTO sem alinhamento com o front.
- Alteração de security, CORS ou cookies sem evidência.

## Saída

Relatório de compatibilidade com riscos e validações.
