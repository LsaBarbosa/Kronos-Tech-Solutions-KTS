# Subagent: Scheduler — CUSTOM_DAYS em shouldWorkToday

## Arquivo alvo

`src/main/java/com/kts/kronos/application/scheduler/DayOffScheduler.java`

## Localização exata

Método `shouldWorkToday()` — linhas 300-315. O switch atual:

```java
return switch (emp.scheduleType()) {
    case TRADITIONAL_5X2 -> isBusinessDay(today);
    case SIX_BY_ONE_FIXED -> !today.getDayOfWeek().equals(emp.preferredDayOff());
    case ROTATING_24X72 -> calculateRotating(emp.scaleStartDate(), today, 4);
    case ROTATING_12X36 -> calculateRotating(emp.scaleStartDate(), today, 2);
    case SIX_BY_ONE_TWO_WEEKENDS -> calculateType5_TwoWeekends(emp, today);
    case SIX_BY_ONE_ONE_WEEKEND -> calculateType6_OneWeekend(emp, today);
    default -> true;
};
```

## Mudança a aplicar

Adicionar antes do `default`:

```java
case CUSTOM_DAYS -> {
    if (emp.fixedWorkDays() == null || emp.fixedWorkDays().isEmpty()) {
        log.warn("event=scheduler_custom_days result=fallback employee_id={} reason=empty_work_days",
                emp.employeeId());
        yield false;
    }
    yield emp.fixedWorkDays().contains(today.getDayOfWeek());
}
```

## Por que `yield false` e não `yield true` no fallback

- `fixedWorkDays` vazio com CUSTOM_DAYS é dado inválido (erro de cadastro)
- Falhar como folga (DAY_OFF) é mais seguro que marcar ABSENCE incorretamente
- Log WARN alerta o operador sem lançar exceção (evita interromper o processamento dos demais)

## Impacto no shouldAnalyzeSwap

O método `shouldAnalyzeSwap()` trata apenas `SIX_BY_ONE_FIXED` e `TRADITIONAL_5X2`.
CUSTOM_DAYS retorna `false` no switch existente (`default -> false`) — correto, não precisa alterar.

## Validação

```bash
./gradlew test --tests "*DayOffSchedulerTest*"
# Deve passar com os novos casos adicionados pelo test-agent
```
