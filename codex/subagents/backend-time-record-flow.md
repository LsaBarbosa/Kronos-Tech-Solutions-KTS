# Subagent — Registro de Ponto

## Foco

Adaptar o domínio de ponto para permitir registro a partir do colaborador identificado pelo terminal.

## Arquivos principais

- `src/main/java/com/kts/kronos/application/service/TimeRecordService.java`
- `src/main/java/com/kts/kronos/application/port/in/usecase/TimeRecordUseCase.java`
- `src/main/java/com/kts/kronos/adapter/in/web/http/TimeRecordController.java`
- `src/main/java/com/kts/kronos/adapter/in/web/dto/timerecord/GeolocationRequest.java`
- providers de ponto, empresa, NSR, AFD, recibo e localização.

## Tasks

1. Preservar `registerTime(GeolocationRequest)` para a aplicação autenticada.
2. Criar método interno ou caso de uso que registre ponto recebendo `employeeId` explicitamente.
3. Reutilizar regras atuais de entrada, saída, pausa implícita, folga e ausência.
4. Manter validação de horário, geolocalização, recibo, NSR e AFD.
5. Evitar duplicidade em retry do navegador.
6. Retornar `ActionResponse` ou resposta equivalente para compor o DTO do terminal.

## Testes mínimos

- entrada;
- saída;
- entrada após pausa;
- conversão de folga ou ausência;
- geolocalização fora do raio;
- status incompatível;
- retry sem duplicar ponto.
