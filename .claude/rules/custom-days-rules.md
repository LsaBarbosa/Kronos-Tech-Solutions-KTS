# Rules: CUSTOM_DAYS — Regras Invioláveis

## Regra 1: fixedWorkDays com 1 a 7 elementos

`CUSTOM_DAYS` exige `fixedWorkDays` com entre 1 e 7 elementos. Valor vazio ou null é inválido.

```java
// ✅ Válido
scheduleType = CUSTOM_DAYS, fixedWorkDays = [MONDAY, WEDNESDAY, FRIDAY]

// ❌ Inválido — deve retornar 400
scheduleType = CUSTOM_DAYS, fixedWorkDays = null
scheduleType = CUSTOM_DAYS, fixedWorkDays = []
```

## Regra 2: Validação no backend antes de persistir

```java
// Em EmployeeService.java
if (request.scheduleType() == WorkScheduleType.CUSTOM_DAYS) {
    if (request.fixedWorkDays() == null || request.fixedWorkDays().isEmpty()) {
        throw new BadRequestException("Escala CUSTOM_DAYS requer ao menos um dia de trabalho em fixedWorkDays.");
    }
}
```

## Regra 3: DayOffScheduler usa fallback seguro

```java
// ✅ CORRETO — fallback para false (DAY_OFF), não lança exceção
case CUSTOM_DAYS -> {
    if (emp.fixedWorkDays() == null || emp.fixedWorkDays().isEmpty()) {
        log.warn("event=scheduler_custom_days result=fallback employee_id={} reason=empty_work_days", emp.employeeId());
        yield false;
    }
    yield emp.fixedWorkDays().contains(today.getDayOfWeek());
}

// ❌ ERRADO — lançar exceção no scheduler interrompe o processamento de todos os funcionários
case CUSTOM_DAYS -> {
    if (emp.fixedWorkDays().isEmpty()) throw new IllegalStateException(...);
```

## Regra 4: Não criar migration

A coluna `fixed_work_days` já existe em `tb_employee`. Criar migration desnecessária pode causar erro de checksum Flyway em produção.

## Regra 5: Mesmo payload no mobile e no desktop

Os componentes `CustomDaysDesktop` e `CustomDaysMobile` recebem as mesmas props (`value: string[]`, `onChange`) e produzem o mesmo resultado. O seletor `CustomDaysSelector` decide qual renderizar. O payload ao backend é **sempre**:
```json
{ "fixedWorkDays": ["MONDAY", "WEDNESDAY", "FRIDAY"] }
```

## Regra 6: Mínimo 1 dia sempre selecionado

- Frontend: desabilitar botão de toggle quando é o último dia
- Backend: validação rejeita array vazio com 400

## Regra 7: Estado reflete o servidor após salvar

Ao editar colaborador com CUSTOM_DAYS, inicializar `fixedWorkDays` com o valor vindo da API (pode ser string[] ou Set como array).

## Regra 8: Nunca alterar escalas existentes

`CUSTOM_DAYS` é additive — apenas adicionado ao enum e ao switch. Os cases `TRADITIONAL_5X2`, `SIX_BY_ONE_FIXED` etc. não devem ser modificados.
