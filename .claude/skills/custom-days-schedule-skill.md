# Skill: CUSTOM_DAYS — Nova Escala de Trabalho

## Descobertas críticas (FASE 0)

O campo `fixedWorkDays` **já existe** na stack completa. Não é necessária migration.

| Camada | Campo | Estado |
|---|---|---|
| `EmployeeEntity.java` | `fixed_work_days` VARCHAR | ✅ Coluna existe no banco |
| `Employee.java` (domain record) | `fixedWorkDays: Set<DayOfWeek>` | ✅ Campo no domain |
| `CreateEmployeeRequest.java` | `Set<DayOfWeek> fixedWorkDays` | ✅ No DTO |
| `UpdateEmployeeManagerRequest.java` | `Set<DayOfWeek> fixedWorkDays` | ✅ No DTO |
| Frontend `useCreateCollaborator.ts` | `fixedWorkDays: z.array(z.string())` | ✅ No schema Zod |
| Frontend payload | `fixedWorkDays: data.fixedWorkDays \|\| []` | ✅ Enviado ao backend |

## Conversão no banco (EmployeeEntity)

O campo é armazenado como `String` comma-separated e convertido por:
```java
// String → Set<DayOfWeek>
private static Set<DayOfWeek> convertStringToSet(String data) {
    if (data == null || data.isBlank()) return Collections.emptySet();
    return Arrays.stream(data.split(",")).map(DayOfWeek::valueOf).collect(Collectors.toSet());
}
// Set<DayOfWeek> → String
private static String convertSetToString(Set<DayOfWeek> days) {
    if (days == null || days.isEmpty()) return null;
    return days.stream().map(DayOfWeek::name).collect(Collectors.joining(","));
}
```

## WorkScheduleType existentes

```java
TRADITIONAL_5X2        // Seg-Sex, usa isBusinessDay()
SIX_BY_ONE_FIXED       // 6x1 folga fixa, usa preferredDayOff
ROTATING_24X72         // Plantão, usa calculateRotating(scaleStartDate, 4)
ROTATING_12X36         // Plantão, usa calculateRotating(scaleStartDate, 2)
SIX_BY_ONE_TWO_WEEKENDS // 6x1 + 2 FDS mês, usa preferredDayOff + trRepo.countWeekendDaysOff
SIX_BY_ONE_ONE_WEEKEND  // 6x1 + 1 FDS mês, usa weekendOffIndex
```

## Como adicionar CUSTOM_DAYS ao DayOffScheduler

O switch `shouldWorkToday()` está em `DayOffScheduler.java` linha 303. Adicionar antes do `default`:

```java
case CUSTOM_DAYS -> {
    if (employee.fixedWorkDays() == null || employee.fixedWorkDays().isEmpty()) {
        log.warn("event=scheduler_custom_days result=fallback employee_id={} reason=empty_work_days",
                employee.employeeId());
        yield false; // Trata como folga se não configurado
    }
    yield employee.fixedWorkDays().contains(today.getDayOfWeek());
}
```

## Validação no backend

Localização: `EmployeeService.java` — no método que processa `CreateEmployeeRequest`.

Adicionar checagem condicional:
```java
if (request.scheduleType() == WorkScheduleType.CUSTOM_DAYS) {
    if (request.fixedWorkDays() == null || request.fixedWorkDays().isEmpty()) {
        throw new BadRequestException("Escala CUSTOM_DAYS requer ao menos um dia de trabalho em fixedWorkDays.");
    }
}
```

## Regra de negócio

- `CUSTOM_DAYS` + `fixedWorkDays = [MONDAY, WEDNESDAY, FRIDAY]` → scheduler marca ABSENCE nas Ter/Qui/Sáb/Dom
- `CUSTOM_DAYS` + `fixedWorkDays` vazio → scheduler usa fallback `false` (DAY_OFF) + log WARN
- `fixedWorkDays` com `CUSTOM_DAYS` = null → mesma regra de fallback

## Arquivo de teste relevante

`src/test/java/com/kts/kronos/application/scheduler/DayOffSchedulerTest.java`

Adicionar casos:
- CUSTOM_DAYS, workDays=[MONDAY,WEDNESDAY], hoje=MONDAY → `true`
- CUSTOM_DAYS, workDays=[MONDAY,WEDNESDAY], hoje=SATURDAY → `false`
- CUSTOM_DAYS, workDays=null → `false` sem lançar exceção
- CUSTOM_DAYS, workDays=[] → `false` sem lançar exceção
