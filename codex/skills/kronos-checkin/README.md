# Kronos Check-in Backend

Objetivo: orientar o Codex na implementação do novo terminal de check-in/checkout.

Leitura inicial obrigatória:

- `README.md`
- `build.gradle`
- `src/main/java/com/kts/kronos/config/SecurityConfig.java`
- `src/main/java/com/kts/kronos/constants/ApiPaths.java`
- `src/main/java/com/kts/kronos/adapter/in/web/http/AuthController.java`
- `src/main/java/com/kts/kronos/adapter/in/web/http/TimeRecordController.java`
- `src/main/java/com/kts/kronos/application/service/AuthService.java`
- `src/main/java/com/kts/kronos/application/service/TimeRecordService.java`

Direção técnica:

- criar contrato específico para terminal de ponto;
- reutilizar validações existentes de biometria, consentimento e ponto;
- preservar o fluxo atual de `/records/checkin`;
- retornar mensagem de identificação e mensagem de registro;
- cobrir controller, service e erros com testes.
