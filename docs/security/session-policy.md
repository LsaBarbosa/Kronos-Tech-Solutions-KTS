# Session Policy

## Objetivo

Documentar a política de autenticação e sessão do back-end Kronos com base na implementação atual, sem expor segredos operacionais.

## Escopo

Este documento cobre:

- login por usuário e senha;
- login biométrico facial;
- emissão e transporte do token JWT;
- renovação de sessão;
- logout e revogação;
- invalidação de sessões após troca ou reset de senha;
- controles mínimos esperados em produção.

Este documento não substitui assessoria jurídica, revisão de infraestrutura ou threat modeling formal.

## Componentes envolvidos

- `SecurityConfig`: configura JWT, CSRF, CORS, endpoints públicos e `SessionCreationPolicy.STATELESS`.
- `AuthController`: expõe `/auth/login`, `/auth/login-face`, `/auth/logout`, `/auth/refresh` e `/auth/csrf`.
- `AuthService`: autentica, gera token, renova token, adiciona token à blacklist no logout e revoga sessões por `sessionVersion`.
- `AuthCookieService`: cria, expira e extrai o cookie de autenticação.
- `JwtAuthenticationFilter`: lê o token do cookie, valida assinatura/expiração, consulta blacklist e compara `sessionVersion`.
- `JwtUtils`: assina e valida JWT, incluindo o claim `session_version`.
- `UserService` e `AuthService.resetPassword(...)`: incrementam `sessionVersion` em troca/reset de senha.

## Estratégia de autenticação

O sistema usa autenticação stateless baseada em JWT.

- O token é emitido após `POST /auth/login` ou `POST /auth/login-face`.
- O token é enviado ao cliente via `Set-Cookie`, não via corpo da resposta.
- O back-end autentica cada requisição lendo o cookie e reconstruindo o contexto de segurança.
- O estado de sessão no servidor é mínimo e se limita a controles de revogação e invalidação, como blacklist e `sessionVersion`.

## Cookies de autenticação

O cookie de autenticação é emitido por `AuthCookieService`.

Propriedades aplicadas na implementação atual:

- `HttpOnly=true`
- `Secure` configurável
- `SameSite` configurável
- `Path` configurável
- `Domain` opcional
- `Max-Age` configurável

Defaults observados:

- `AUTH_COOKIE_NAME=KRONOS_ACCESS_TOKEN`
- `AUTH_COOKIE_SECURE=true` em `application.yml` e `application-prod.yml`
- `AUTH_COOKIE_SECURE=false` em `application-local.yml`
- `AUTH_COOKIE_SAME_SITE=Lax`
- `AUTH_COOKIE_PATH=/`
- `AUTH_COOKIE_MAX_AGE_SECONDS=900` em perfis padrão/prod

O token de autenticação deve permanecer em cookie `HttpOnly`. O front-end não deve depender de leitura direta desse cookie.

## Expiração de sessão

O tempo de vida do JWT é controlado por `JWT_EXPIRATION`.

- Valor padrão nos perfis principal e produção: `900000` ms, equivalente a 15 minutos.
- Valor default no perfil local: `3600000` ms, equivalente a 60 minutos.
- Expiração curta reduz a janela de abuso em caso de roubo do token.

O filtro JWT rejeita tokens expirados ou inválidos antes de montar autenticação no contexto da requisição.

## Refresh token / renovação

A implementação atual não mantém um refresh token persistido separado. O fluxo de renovação usa `POST /auth/refresh` com o token atual presente no cookie.

Comportamento atual:

- o endpoint lê o token do cookie;
- a renovação só ocorre quando o token apresentado já expirou e ainda pode ter seus claims lidos com segurança;
- se o token estiver na blacklist, a renovação falha;
- se o usuário estiver inativo, a renovação falha;
- se o `sessionVersion` atual do usuário diferir do claim `session_version`, a renovação falha;
- quando a renovação ocorre, o token antigo é colocado na blacklist e um novo JWT é emitido em novo cookie.

## Logout e blacklist

O logout é exposto por `POST /auth/logout`.

Fluxo atual:

- o controller extrai o token do cookie;
- `AuthService.logout(...)` valida o JWT recebido;
- se válido, o token é adicionado à blacklist com a mesma expiração do JWT;
- a resposta expira o cookie de autenticação no cliente.

Isso evita reutilização do token até o vencimento natural.

## Revogação por troca de senha

O projeto usa `sessionVersion` para invalidar sessões anteriores.

- `AuthService.resetPassword(...)` salva o usuário com `incrementSessionVersion()`.
- `UserService.changeOwnPassword(...)` também salva o usuário com `incrementSessionVersion()`.
- `JwtAuthenticationFilter` compara o `session_version` do token com o valor atual do usuário.
- Se houver divergência, a autenticação não é estabelecida.
- `AuthService.refreshToken(...)` também bloqueia renovação quando a versão da sessão mudou.

Na prática, troca ou reset de senha revogam sessões emitidas anteriormente mesmo sem apagar todos os cookies imediatamente.

## Regras para produção

- Usar `JWT_SECRET` forte e sem valor default.
- Manter `AUTH_COOKIE_SECURE=true`.
- Manter `HttpOnly=true` para o cookie de autenticação.
- Definir `AUTH_COOKIE_SAME_SITE` conscientemente, com justificativa operacional quando diferente de `Lax`.
- Restringir `AUTH_COOKIE_DOMAIN` ao domínio necessário.
- Manter `AUTH_COOKIE_PATH=/` salvo necessidade comprovada de escopo menor.
- Publicar somente sobre HTTPS.
- Limitar `frontend.allowed-origins` aos front-ends legítimos, porque o sistema opera com `allowCredentials=true`.
- Não documentar nem versionar segredos reais.

## Variáveis de ambiente relacionadas

O código consome propriedades Spring como `jwt.*` e `kronos.security.auth-cookie.*`, normalmente abastecidas por variáveis de ambiente em caixa alta.

| Variável de ambiente | Propriedade Spring associada | Observação |
| --- | --- | --- |
| `JWT_SECRET` | `jwt.secret` | Obrigatória fora do perfil local. Não documentar valor real. |
| `JWT_EXPIRATION` | `jwt.expiration` | Tempo de vida do JWT em milissegundos. |
| `AUTH_COOKIE_NAME` | `kronos.security.auth-cookie.name` | Nome do cookie de autenticação. |
| `AUTH_COOKIE_SECURE` | `kronos.security.auth-cookie.secure` | Deve ser `true` em produção. |
| `AUTH_COOKIE_SAME_SITE` | `kronos.security.auth-cookie.same-site` | Default `Lax`. |
| `AUTH_COOKIE_PATH` | `kronos.security.auth-cookie.path` | Default `/`. |
| `AUTH_COOKIE_DOMAIN` | `kronos.security.auth-cookie.domain` | Opcional. |
| `AUTH_COOKIE_MAX_AGE_SECONDS` | `kronos.security.auth-cookie.max-age-seconds` | Default 900 s em produção/padrão. |

## Riscos conhecidos

- O perfil local aceita defaults de desenvolvimento para `JWT_SECRET` e `AUTH_COOKIE_SECURE=false`; isso não é aceitável em produção.
- A renovação usa o token expirado anterior, não um refresh token dedicado com armazenamento próprio; isso simplifica o fluxo, mas reduz separação entre token de acesso e mecanismo de renovação.
- O sistema depende de CORS com `allowCredentials=true`, então a lista de origens permitidas precisa permanecer estrita.
- O logout revoga o token atual, mas não força remoção física imediata de todos os cookies já distribuídos em outros navegadores; a contenção ampla vem da combinação entre expiração curta, blacklist e `sessionVersion`.

## Checklist de validação

- [ ] `JWT_SECRET` não possui valor default em produção.
- [ ] `AUTH_COOKIE_SECURE=true` em produção.
- [ ] `AUTH_COOKIE_SAME_SITE` está definido conscientemente.
- [ ] Logout adiciona o token atual à blacklist.
- [ ] Troca de senha invalida sessões antigas por `sessionVersion`.
- [ ] Reset de senha invalida sessões antigas por `sessionVersion`.
- [ ] Endpoints sensíveis exigem autenticação.
- [ ] O cookie de autenticação permanece `HttpOnly`.
- [ ] O ambiente produtivo publica a API apenas sobre HTTPS.
