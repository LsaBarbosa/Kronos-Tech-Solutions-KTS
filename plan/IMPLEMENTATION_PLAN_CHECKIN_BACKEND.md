# Plano de Ação — Back-end Check-in/Checkout Passwordless

## 0. Contexto validado

- Branch de análise: `homolog`.
- Branch de implementação: `checkin`.
- Stack: Java 21, Spring Boot, Spring Security, JPA, Redis/cache, Flyway, PostgreSQL.
- O fluxo atual possui login facial separado do registro de ponto.
- O novo terminal deve fazer identificação facial + geolocalização + ponto em uma operação única para a UI.

## 1. Leitura obrigatória

### Back-end

1. `README.md`
2. `build.gradle`
3. `src/main/java/com/kts/kronos/config/SecurityConfig.java`
4. `src/main/java/com/kts/kronos/constants/ApiPaths.java`
5. `src/main/java/com/kts/kronos/adapter/in/web/http/AuthController.java`
6. `src/main/java/com/kts/kronos/adapter/in/web/http/TimeRecordController.java`
7. `src/main/java/com/kts/kronos/adapter/in/web/dto/security/FaceLoginRequest.java`
8. `src/main/java/com/kts/kronos/adapter/in/web/dto/timerecord/GeolocationRequest.java`
9. `src/main/java/com/kts/kronos/adapter/in/web/dto/timerecord/ActionResponse.java`
10. `src/main/java/com/kts/kronos/application/port/in/usecase/AuthUseCase.java`
11. `src/main/java/com/kts/kronos/application/port/in/usecase/TimeRecordUseCase.java`
12. `src/main/java/com/kts/kronos/application/service/AuthService.java`
13. `src/main/java/com/kts/kronos/application/service/TimeRecordService.java`
14. `src/main/java/com/kts/kronos/application/security/BiometricProtectionService.java`
15. `src/test/java/com/kts/kronos/adapter/in/web/http/webmvc/AuthControllerWebMvcTest.java`
16. `src/test/java/com/kts/kronos/adapter/in/web/http/webmvc/TimeRecordControllerWebMvcTest.java`
17. `src/test/java/com/kts/kronos/application/service/TimeRecordServiceTest.java`

### Documentação

1. `../kronos-business/README.md`
2. `../kronos-business/04-fluxos-aplicacao.md`
3. `../kronos-business/06-contratos-api.md`
4. `../kronos-business/10-regras-negocio.md`
5. `../kronos-business/11-autenticacao-seguranca.md`
6. `../kronos-business/15-lgpd-privacidade.md`

## 2. Contrato alvo

Criar contrato específico para a tela isolada de ponto:

```http
POST /auth/checkin-face
```

Request:

```json
{
  "faceImageBase64": "base64-sem-data-url",
  "latitude": -22.9,
  "longitude": -43.2,
  "accuracy": 18.5,
  "livenessPassed": true
}
```

Response:

```json
{
  "loginMessage": "Login realizado com sucesso.",
  "recordMessage": "Entrada às 08:01! (NSR: 123)",
  "actionType": "CHECKIN",
  "autoLogoutAfterSeconds": 10,
  "recordedAt": "2026-06-26T08:01:00-03:00"
}
```

## 3. Tasks de implementação

### Task 1 — Criar DTOs

- Criar `FaceCheckinRequest` em pacote de DTO de segurança ou check-in.
- Criar `FaceCheckinResponse`.
- Validar:
  - `faceImageBase64` obrigatório;
  - `latitude` obrigatório;
  - `longitude` obrigatório;
  - `accuracy` opcional;
  - `livenessPassed` opcional.

### Task 2 — Extrair identificação biométrica reutilizável

- Mapear a lógica atual em `AuthService.loginFace`.
- Extrair serviço/caso de uso para retornar usuário e colaborador identificados.
- Preservar o comportamento atual de `loginFace`.
- Garantir que o novo fluxo não faça duas identificações biométricas independentes com a mesma imagem.

### Task 3 — Refatorar registro de ponto por colaborador explícito

- Preservar `TimeRecordService.registerTime(GeolocationRequest)`.
- Criar método/caso de uso interno para registrar ponto recebendo `employeeId`.
- Reutilizar toda a regra de negócio existente.
- Manter geração de NSR, recibo, AFD, cache e métricas.

### Task 4 — Criar orquestrador do terminal

- Criar `PasswordlessCheckinUseCase` ou nome equivalente.
- Orquestrar:
  1. identificação facial;
  2. validações de consentimento e status;
  3. registro do ponto;
  4. montagem do response.

### Task 5 — Criar endpoint

- Adicionar método em `AuthController` ou controller dedicado.
- Usar rota `POST /auth/checkin-face`.
- Aplicar o mesmo padrão de exposição controlada usado pelos endpoints de autenticação existentes.
- Retornar 200 com DTO de sucesso.

### Task 6 — Sessão curta do terminal

- Se a implementação emitir sessão, ela deve ser curta e específica para o terminal.
- O response deve informar `autoLogoutAfterSeconds: 10`.
- O botão `Sair` e o timer do front poderão chamar o logout existente.

### Task 7 — Auditoria, métricas e logs

- Registrar sucesso e falha de identificação.
- Registrar sucesso e falha de ponto.
- Não logar imagem, tokens, CPF, e-mail ou cookie.
- Usar padrões de observabilidade já existentes.

### Task 8 — Testes

Criar ou ajustar testes para:

- sucesso de entrada;
- sucesso de saída;
- biometria não correspondente;
- imagem inválida;
- consentimento pendente;
- usuário inativo;
- geolocalização inválida;
- status de ponto incompatível;
- manutenção do endpoint atual `/records/checkin`.

## 4. Validação final

Rodar:

```bash
./gradlew test
./gradlew check
```

## 5. Critério final de aceite

- A UI envia uma única foto.
- A API identifica o colaborador e registra o ponto no mesmo request.
- O comportamento existente da plataforma permanece estável.
- O front recebe mensagem suficiente para exibir sucesso por 10 segundos e limpar sessão.
