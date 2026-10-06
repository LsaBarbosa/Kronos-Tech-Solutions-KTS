# Subagent: Backend Enum — CUSTOM_DAYS

## Arquivo alvo

`src/main/java/com/kts/kronos/domain/model/enuns/WorkScheduleType.java`

## Roteiro exato

Adicionar após `SIX_BY_ONE_ONE_WEEKEND`:

```java
// 7. Dias específicos da semana — definidos em Employee.fixedWorkDays
CUSTOM_DAYS
```

## Estado atual do arquivo (6 valores)

```java
public enum WorkScheduleType {
    TRADITIONAL_5X2,
    SIX_BY_ONE_FIXED,
    ROTATING_24X72,
    ROTATING_12X36,
    SIX_BY_ONE_TWO_WEEKENDS,
    SIX_BY_ONE_ONE_WEEKEND
}
```

## Impacto em switches existentes

O DayOffScheduler tem `default -> true` que já absorve CUSTOM_DAYS até o scheduler-agent implementar o case correto. Verificar se existem outros switches no projeto que precisam de atualização:

```bash
grep -r "case TRADITIONAL_5X2\|case SIX_BY_ONE\|scheduleType ==" \
  src/main/java --include="*.java" | grep -v "test"
```

## Validação

```bash
./gradlew compileJava
# Deve compilar sem erros
```

## Não fazer

- NÃO modificar campos do Employee (fixedWorkDays já existe)
- NÃO criar migration (fixed_work_days já existe em tb_employee)
- NÃO alterar DTOs (fixedWorkDays já está em CreateEmployeeRequest e UpdateEmployeeManagerRequest)
