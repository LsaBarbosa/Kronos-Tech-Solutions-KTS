# Subagent — Frontend Contract Validation Subagent

## Objetivo

Garantir que Redis seja transparente para o front-end.

## Pontos de validação

- Login continua retornando `204` com cookie HttpOnly.
- Login facial continua retornando `204` com cookie HttpOnly.
- Recover password continua retornando `204` neutro.
- Reset password continua retornando `204`.
- Logout continua retornando `204` com cookie expirado.
- Refresh continua retornando `204` com novo cookie.
- Checkin continua retornando `ActionResponse`.
- Erro de rate limit retorna status 429 padronizado.

## Tarefas

- Validar interceptors Axios.
- Validar CSRF.
- Validar React Query cache client-side não conflita com backend cache.
- Atualizar OpenAPI/tipos somente se contrato mudou.
