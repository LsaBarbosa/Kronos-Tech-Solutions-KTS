# Subagent — Redis Token Subagent

## Objetivo

Migrar tokens temporários e blacklist para Redis.

## Alvos

- `TokenBlacklistProvider`
- `PasswordResetTokenProvider`
- `AuthService.logout`
- `AuthService.refreshToken`
- `AuthService.recoverPassword`
- `AuthService.resetPassword`
- `JwtAuthenticationFilter`, se ele consulta blacklist

## Regras

- Token cru nunca é chave nem valor.
- Chave usa SHA-256 do token.
- Blacklist expira na expiração original do JWT.
- Reset token expira por configuração.
- Delete após reset bem-sucedido.
- Fluxo recover-password permanece neutro.

## Critério de aceite

- Logout revoga token até expiração.
- Refresh rejeita token em blacklist.
- Refresh adiciona token antigo à blacklist.
- Reset token expira corretamente.
- Reset token é de uso único.
