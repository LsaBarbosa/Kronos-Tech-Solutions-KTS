# Subagent — Redis Rate Limit Subagent

## Objetivo

Migrar rate limits em memória para Redis.

## Alvos

- `AuthenticationRateLimitService`
- `BiometricProtectionService`

## Endpoints afetados

- `POST /auth/login`
- `POST /auth/login-face`
- `POST /auth/recover-password`
- `GET /employee/check-cpf?cpf=`
- `GET /users/check-username?username=`
- `GET /companies/check-cnpj?cnpj=`
- `POST /records/checkin`
- `POST /employee/manager/{employeeId}/biometric-enrollment`

## Estratégia

- Criar `RateLimitStore`.
- Implementar Redis com `INCR` + `EXPIRE`.
- Implementar cooldown com chave bloqueada e TTL.
- Usar HMAC para CPF/e-mail/username/IP.
- Preservar mensagens `TooManyRequestsException`.

## Testes

- permite até limite;
- bloqueia após limite;
- expira após janela;
- cooldown aumenta corretamente;
- sucesso limpa falhas de username;
- Redis indisponível segue configuração.
