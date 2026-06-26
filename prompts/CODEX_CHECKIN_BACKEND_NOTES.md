# Notas para execução — Back-end Check-in

Branch: `checkin`.

## Objetivo

Implementar o back-end do terminal isolado de ponto. A UI enviará foto e coordenadas em uma chamada. A API deve identificar o colaborador, registrar entrada ou saída e devolver mensagens para a tela.

## Ler primeiro

- `README.md`
- `build.gradle`
- `src/main/java/com/kts/kronos/config/SecurityConfig.java`
- `src/main/java/com/kts/kronos/constants/ApiPaths.java`
- `src/main/java/com/kts/kronos/adapter/in/web/http/AuthController.java`
- `src/main/java/com/kts/kronos/adapter/in/web/http/TimeRecordController.java`
- `src/main/java/com/kts/kronos/application/service/AuthService.java`
- `src/main/java/com/kts/kronos/application/service/TimeRecordService.java`
- `src/test/java/com/kts/kronos/adapter/in/web/http/webmvc/AuthControllerWebMvcTest.java`
- `src/test/java/com/kts/kronos/adapter/in/web/http/webmvc/TimeRecordControllerWebMvcTest.java`
- `src/test/java/com/kts/kronos/application/service/TimeRecordServiceTest.java`

## Sequência

1. Criar DTOs do terminal.
2. Reutilizar identificação facial existente.
3. Preservar contratos atuais.
4. Adaptar regra de ponto para colaborador identificado.
5. Criar rota específica do terminal.
6. Retornar mensagens e tempo de exibição de 10 segundos.
7. Criar testes.

## Validação

```bash
./gradlew test
./gradlew check
```
