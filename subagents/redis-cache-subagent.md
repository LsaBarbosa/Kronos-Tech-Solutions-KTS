# Subagent — Redis Cache Subagent

## Objetivo

Implementar cache-aside seguro e com TTL nos endpoints de leitura.

## Alvos

- `GET /dashboard/summary`
- `GET /records/me/today`
- `GET /records/me/recent`
- `GET /records/me/requests`
- `GET /employee/own-profile`
- `GET /users/own-profile`
- `GET /employee?active=`
- `GET /users/search?active=`
- `GET /companies/{cnpj}`
- `POST /geolocation/resolve`
- `GET /public/privacy/*`

## Regras

- Cachear DTO, não Entity JPA.
- Chave precisa conter escopo de tenant/usuário/papel quando aplicável.
- PII em chave só via HMAC.
- TTL obrigatório.
- Escritas invalidam caches relacionados.
- Cache falha aberto.

## Critério de aceite

- Cache hit/miss testado.
- Invalidação testada.
- Não há vazamento cross-tenant.
