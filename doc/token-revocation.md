# Revogacao e Versionamento de Token JWT

## Abordagem

A aplicacao usa `tokenVersion` persistido em `tb_user.token_version`.

Cada token JWT emitido recebe a claim `token_version`. Durante a autenticacao, o filtro valida assinatura, `iss`, `aud`, `kid`, `jti`, `nbf` e entao compara a claim `token_version` com a versao atual do usuario no banco. Se o usuario estiver inativo, ausente ou com versao diferente, o token e tratado como revogado.

## Eventos que incrementam `tokenVersion`

- Reset de senha.
- Troca de senha pelo usuario autenticado.
- Alteracao sensivel de usuario: username, senha, perfil ou status ativo.
- Desativacao/reativacao via `toggleActivate`.
- Aceite ou revogacao de consentimento biometrico.

## Resultado operacional

Incrementar `tokenVersion` invalida todos os tokens emitidos anteriormente para o usuario sem manter blacklist em memoria. A revogacao e global por usuario e passa a valer assim que a nova versao e persistida.

## Logout global

Para implementar logout global, chame o servico central de revogacao para incrementar `tokenVersion` do usuario autenticado. O proximo request com tokens antigos sera rejeitado pelo filtro JWT.
