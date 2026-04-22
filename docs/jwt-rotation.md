# Rotação de Segredo JWT

## Variáveis obrigatórias por ambiente

Cada ambiente deve ter seus próprios valores. Não reutilize segredo, issuer, audience ou key id entre `local`, `test`, `staging` e `prod`.

- `JWT_SECRET`: segredo HMAC atual em Base64, com pelo menos 32 bytes após decodificação.
- `JWT_CURRENT_KEY_ID`: identificador público da chave atual, emitido no header `kid`.
- `JWT_ISSUER`: emissor esperado no claim `iss`.
- `JWT_AUDIENCE`: público esperado no claim `aud`.
- `JWT_PREVIOUS_SECRETS`: lista opcional de chaves anteriores no formato `kid:secretBase64,kid2:secretBase64`.
- `JWT_EXPIRATION`: TTL máximo do token em milissegundos.
- `JWT_ALLOWED_CLOCK_SKEW_SECONDS`: tolerância de relógio para validação temporal.
- `JWT_NOT_BEFORE_SKEW_SECONDS`: ajuste operacional para o claim `nbf`.

## Claims e validação

Tokens emitidos pela aplicação devem conter `iss`, `aud`, `jti`, `nbf`, `iat`, `exp` e `kid`.

A validação rejeita tokens quando:

- `iss` não corresponde a `JWT_ISSUER`.
- `aud` não corresponde a `JWT_AUDIENCE`.
- `kid` está ausente ou não existe na chave atual nem em `JWT_PREVIOUS_SECRETS`.
- A assinatura não é válida para a chave indicada por `kid`.
- As janelas `nbf`/`exp` não são válidas, considerando apenas o clock skew configurado.

## Procedimento de rotação

1. Gere um novo segredo Base64 com pelo menos 32 bytes reais de entropia.
2. Defina um novo `JWT_CURRENT_KEY_ID`, por exemplo `prod-2026-05`.
3. Mova a chave ativa anterior para `JWT_PREVIOUS_SECRETS` no formato `kidAnterior:secretAnteriorBase64`.
4. Atualize `JWT_SECRET` com o novo segredo e faça o deploy.
5. Aguarde `JWT_EXPIRATION` mais `JWT_ALLOWED_CLOCK_SKEW_SECONDS` para que todos os tokens antigos expirem.
6. Remova o item antigo de `JWT_PREVIOUS_SECRETS` e faça novo deploy.

## Regras operacionais

- Nunca remova uma chave anterior antes de expirar a janela máxima de tokens emitidos com ela.
- Nunca reutilize `JWT_CURRENT_KEY_ID` para outro segredo.
- Nunca registre valores de `JWT_SECRET` ou `JWT_PREVIOUS_SECRETS` em logs, issues ou documentos versionados.
- Em incidente de vazamento, force troca imediata de `JWT_SECRET`, altere `JWT_CURRENT_KEY_ID` e esvazie `JWT_PREVIOUS_SECRETS` para invalidar tokens antigos.
