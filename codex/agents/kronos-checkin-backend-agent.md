# Agent — Backend Check-in

## Responsabilidade

Conduzir a implementação back-end do terminal isolado de check-in/checkout.

## Leitura obrigatória

1. `README.md`
2. `build.gradle`
3. `src/main/java/com/kts/kronos/config/SecurityConfig.java`
4. `src/main/java/com/kts/kronos/constants/ApiPaths.java`
5. `src/main/java/com/kts/kronos/adapter/in/web/http/AuthController.java`
6. `src/main/java/com/kts/kronos/adapter/in/web/http/TimeRecordController.java`
7. `src/main/java/com/kts/kronos/application/port/in/usecase/AuthUseCase.java`
8. `src/main/java/com/kts/kronos/application/port/in/usecase/TimeRecordUseCase.java`
9. `src/main/java/com/kts/kronos/application/service/AuthService.java`
10. `src/main/java/com/kts/kronos/application/service/TimeRecordService.java`
11. `src/test/java/com/kts/kronos/adapter/in/web/http/webmvc/AuthControllerWebMvcTest.java`
12. `src/test/java/com/kts/kronos/adapter/in/web/http/webmvc/TimeRecordControllerWebMvcTest.java`
13. `src/test/java/com/kts/kronos/application/service/TimeRecordServiceTest.java`

## Entregas esperadas

- DTO de request do terminal com foto, latitude, longitude, precisão e liveness opcional.
- DTO de response com mensagem de identificação, mensagem do ponto, tipo de ação e tempo de exibição.
- Endpoint específico para o terminal.
- Caso de uso isolado para coordenar identificação facial e registro de ponto.
- Refatoração mínima em `TimeRecordService` para registrar ponto a partir de colaborador identificado pela biometria.
- Testes de sucesso, falha de face, falta de consentimento, usuário inativo, geolocalização inválida e status incompatível.

## Restrições

- Não remover endpoints existentes.
- Não alterar contratos de relatório, férias, documentos ou LGPD.
- Não persistir imagem facial fora do fluxo já previsto pelo sistema.
- Não expor dados sensíveis em logs.
