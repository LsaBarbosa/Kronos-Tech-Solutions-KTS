# Regras — Kronos Check-in/Checkout Passwordless

## Escopo obrigatório

- Implementar somente o fluxo isolado de check-in/checkout biométrico sem senha.
- A branch de trabalho é `checkin`, criada a partir de `homolog`.
- Não alterar comportamento do login por senha, dashboard, administração, documentos, férias, LGPD ou relatórios fora do necessário para expor o novo contrato.
- O front-end não deve precisar chamar o login facial e depois o registro de ponto com a mesma foto. O novo fluxo deve aceitar uma única imagem enviada pelo navegador e registrar o ponto em uma única operação de negócio.

## Contrato preferencial

Criar um endpoint específico para o terminal de ponto:

```http
POST /auth/checkin-face
Content-Type: application/json
```

Entrada esperada:

```json
{
  "faceImageBase64": "base64-sem-prefixo-data-url",
  "latitude": -22.9,
  "longitude": -43.2,
  "accuracy": 18.5,
  "livenessPassed": true
}
```

Saída esperada:

```json
{
  "loginMessage": "Login realizado com sucesso.",
  "recordMessage": "Entrada às 08:01! (NSR: 123)",
  "actionType": "CHECKIN",
  "autoLogoutAfterSeconds": 10,
  "recordedAt": "2026-06-26T08:01:00-03:00"
}
```

## Regras de autenticação e sessão

- O endpoint deve autenticar por biometria facial sem senha.
- O endpoint deve registrar o ponto no mesmo processamento da requisição.
- A sessão criada para esse fluxo, se necessária, deve ser curta e própria do terminal de ponto.
- O front-end exibirá sucesso por 10 segundos e depois encerrará a sessão, além de fornecer botão `Sair`.
- Não registrar imagem, token, cookie, CPF ou e-mail em log, auditoria, tracing ou métrica.

## Regras de domínio

- Reutilizar as validações existentes de reconhecimento facial, consentimento biométrico, usuário ativo, colaborador vinculado, geolocalização, horário, NSR, AFD, recibo, entrada, saída, pausa implícita, folga e ausência.
- Não duplicar registro de ponto se houver retry de rede do navegador.
- Não quebrar o endpoint atual `POST /records/checkin`.

## Regra crítica sobre envio único de foto

- O navegador deve enviar a foto uma única vez.
- O back-end deve resolver o colaborador pela face e reutilizar esse resultado para registrar o ponto.
- Evitar dois fluxos independentes de reconhecimento usando a mesma imagem dentro da mesma operação.

## Segurança

- Aplicar ao novo endpoint o mesmo padrão de exposição controlada já usado pelos endpoints de autenticação existentes.
- Não ampliar permissões para outros endpoints.
- Rate limit biométrico obrigatório.
- Auditoria obrigatória para sucesso e falha.
- Sanitização obrigatória de dados sensíveis em logs e observabilidade.

## Qualidade mínima

Executar antes de concluir:

```bash
./gradlew test
./gradlew check
```

Se algum teste legado falhar por motivo não relacionado, documentar qual teste falhou, stack trace resumida e motivo provável.
