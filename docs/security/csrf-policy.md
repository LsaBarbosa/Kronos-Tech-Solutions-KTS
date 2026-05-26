# CSRF Protection Policy

## Objetivo

Documentar a estratégia de proteção contra CSRF adotada pelo back-end Kronos e como ela se integra ao uso de cookies de autenticação.

## Por que CSRF é necessário

O Kronos autentica usuários com cookie e opera com `allowCredentials=true` em CORS. Nesse modelo, um navegador pode anexar automaticamente cookies válidos em requisições cross-site. A proteção CSRF reduz o risco de requisições de escrita disparadas sem intenção do titular autenticado.

## Estratégia adotada

A aplicação usa `CookieCsrfTokenRepository` e protege métodos de escrita por token CSRF.

Na implementação atual:

- o token CSRF é materializado em cookie próprio;
- o front-end pode buscar o token via `GET /auth/csrf`;
- requisições de escrita devem reenviar o valor no header configurado;
- a sessão da aplicação permanece stateless do ponto de vista de autenticação, mas a validação CSRF segue ativa para mutações.

Métodos afetados:

- `POST`
- `PUT`
- `PATCH`
- `DELETE`

## Cookie CSRF

O cookie CSRF é separado do cookie de autenticação.

Comportamento atual:

- o repositório é criado com `withHttpOnlyFalse()`;
- o cookie CSRF pode ser lido pelo front-end quando necessário;
- o cookie usa nome configurável;
- `path`, `secure` e `same-site` são configuráveis.

O cookie CSRF não substitui o cookie de autenticação e não deve transportar identidade do usuário.

## Header CSRF

O header padrão configurado é `X-CSRF-TOKEN`.

Fluxo esperado no cliente:

1. obter o token CSRF por cookie ou por `GET /auth/csrf`;
2. manter `withCredentials=true` nas chamadas autenticadas;
3. reenviar o token no header configurado para todas as operações de escrita.

## Endpoints isentos

Os endpoints abaixo estão explicitamente isentos da validação CSRF porque são usados antes da autenticação completa, em renovação técnica de sessão ou em integração pública específica:

- `POST /auth/login`
- `POST /auth/login-face`
- `POST /auth/recover-password`
- `POST /auth/reset-password`
- `POST /auth/logout`
- `POST /auth/refresh`
- `POST /geolocation/resolve`

Justificativa operacional:

- login e recuperação de conta precisam funcionar antes da sessão autenticada estar estabelecida;
- `refresh` e `logout` participam do ciclo técnico da sessão;
- `geolocation/resolve` está tratado como exceção pública específica no `SecurityConfig`.

A lista de isenções deve permanecer mínima e revisada sempre que novos endpoints públicos forem adicionados.

## Integração com front-end

O front-end deve tratar separadamente:

- cookie de autenticação: `HttpOnly`, não legível por JavaScript;
- cookie/token CSRF: legível pelo front-end quando necessário para preencher o header.

Implicações práticas:

- chamadas autenticadas devem usar credenciais do navegador;
- operações de escrita sem o header CSRF correto podem falhar com erro de acesso negado;
- `GET /auth/csrf` pode ser usado para bootstrap do token antes de fluxos de escrita.

## Regras para produção

- Publicar a aplicação somente sobre HTTPS.
- Manter o cookie de autenticação como `HttpOnly`.
- Manter o cookie CSRF com `secure=true`.
- Definir `same-site` conscientemente. No perfil de produção atual, CSRF e cookies auxiliares usam `SameSite=None`, o que exige HTTPS.
- Revisar a lista de endpoints isentos antes de cada go-live.
- Restringir `frontend.allowed-origins` aos domínios realmente autorizados.

## Variáveis de ambiente relacionadas

As propriedades abaixo controlam a política CSRF na configuração Spring:

| Propriedade | Valor/configuração observada | Observação |
| --- | --- | --- |
| `app.security.csrf.cookie-name` | `KRONOS_CSRF_TOKEN` no perfil de produção | Nome do cookie CSRF. |
| `app.security.csrf.header-name` | `X-CSRF-TOKEN` | Header esperado nas mutações. |
| `app.security.csrf.cookie-path` | `/` | Escopo do cookie. |
| `app.security.csrf.secure` | `true` no perfil de produção | Deve permanecer `true` em produção. |
| `app.security.csrf.same-site` | `None` no perfil de produção | Requer HTTPS quando usado cross-site. |

## Checklist de validação

- [ ] Métodos de escrita exigem CSRF.
- [ ] Cookie de autenticação é `HttpOnly`.
- [ ] Cookie CSRF é acessível ao front quando necessário.
- [ ] Endpoints isentos são mínimos e justificados.
- [ ] Ambiente de produção usa HTTPS.
- [ ] O front-end reenvia `X-CSRF-TOKEN` nas operações de escrita.
