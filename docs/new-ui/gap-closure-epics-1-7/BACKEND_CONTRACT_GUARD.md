# BACKEND CONTRACT GUARD — Gap Closure EPICs 01 a 07

## Objetivo

Validar que o back-end da branch `new-ui` continua compatível com o front-end durante a correção dos gaps dos EPICs 01 a 07.

## Regra

O back-end é suporte contratual nesta trilha. Não implementar novas regras de negócio por causa de ajuste visual do front-end.

## Verificações

- controllers consumidos pelo front;
- DTOs de request e response;
- autenticação e autorização;
- CORS, cookies e headers;
- endpoints de empresas, colaboradores, avisos, documentos, login e ponto.

## Saída esperada

Relatório com:

- contratos preservados;
- diferenças encontradas;
- riscos;
- se há bloqueio para tela de usuários de acesso.
