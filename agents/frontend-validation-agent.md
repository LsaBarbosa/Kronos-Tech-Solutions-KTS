# Agent — Frontend Contract Validation Agent

## Responsabilidade

Validar que o front-end `PROD_HOSTINGER_v2` continua funcionando sem alterações de contrato.

## Regras

- Não implementar Redis no front-end.
- Não expor estado de Redis ao usuário final.
- Não alterar payloads dos endpoints salvo necessidade justificada.
- Validar cookies/CSRF/withCredentials.
- Validar comportamento de erro 429 em login, login facial, recuperação de senha e checkin.

## Tarefas

1. Ler API client/Axios.
2. Ler chamadas para endpoints afetados.
3. Verificar se 429 já é tratado adequadamente.
4. Rodar `npm run lint`, `npm run test`, `npm run build`.
5. Se contrato OpenAPI existir, regenerar tipos somente se backend mudou contrato.
6. Registrar que Redis é transparente ao front.
