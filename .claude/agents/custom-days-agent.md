# Agent: Implementador CUSTOM_DAYS — End-to-End

## Objetivo

Implementar a escala CUSTOM_DAYS completa: backend (enum + scheduler + validação + testes) e frontend (componentes desktop/mobile + integração no formulário).

## Descoberta crítica

O campo `fixedWorkDays` **já existe** em toda a stack. Não é necessária migration Flyway.
- Coluna `fixed_work_days` VARCHAR em `tb_employee` — persistida como "MONDAY,TUESDAY,..."
- Domain `Employee.fixedWorkDays(): Set<DayOfWeek>`
- DTOs `CreateEmployeeRequest.fixedWorkDays` e `UpdateEmployeeManagerRequest.fixedWorkDays`
- Frontend `fixedWorkDays` no schema Zod e no payload enviado

## Subagentes e sequência

```
backend-enum-agent       → BACK-01: adicionar CUSTOM_DAYS ao enum
      ↓
scheduler-agent          → BACK-05: implementar shouldWorkToday(CUSTOM_DAYS)
      ↓
validation-agent         → BACK-06: validação fixedWorkDays obrigatório p/ CUSTOM_DAYS
      ↓
test-agent               → BACK-07: testes unitários DayOffSchedulerTest
      ↓
[paralelo]
frontend-desktop-agent   → FRONT-02: CustomDaysDesktop.tsx
frontend-mobile-agent    → FRONT-03: CustomDaysMobile.tsx
      ↓
frontend-selector-agent  → FRONT-04: CustomDaysSelector.tsx (orquestrador)
      ↓
frontend-integration-agent → FRONT-05/06/07: integrar no formulário
      ↓
validation-agent         → VAL-01/02: builds sem erro
```

## Critérios de sucesso por subagente

| Subagente | Critério |
|---|---|
| `backend-enum-agent` | `WorkScheduleType.CUSTOM_DAYS` compilando sem erros |
| `scheduler-agent` | `shouldWorkToday(CUSTOM_DAYS, [MONDAY], MONDAY)` → `true` |
| `validation-agent` | `POST /employee` com `CUSTOM_DAYS` e `fixedWorkDays=[]` → 400 |
| `test-agent` | `./gradlew test --tests "*DayOffScheduler*"` → VERDE |
| `frontend-desktop-agent` | 7 pills renderizando, toggle funcional, resumo ao vivo |
| `frontend-mobile-agent` | Stepper +/- funcional, strip de pills atualizado |
| `frontend-selector-agent` | Troca Desktop↔Mobile ao redimensionar janela |
| `frontend-integration-agent` | `CUSTOM_DAYS` no select, `CustomDaysSelector` visível, payload correto |
| Build | `./gradlew bootJar -x test` e `npm run build` — exit 0 |

## Arquivos principais modificados

### Backend
- `src/main/java/com/kts/kronos/domain/model/enuns/WorkScheduleType.java` — +1 enum
- `src/main/java/com/kts/kronos/application/scheduler/DayOffScheduler.java` — +1 case
- `src/main/java/com/kts/kronos/application/service/EmployeeService.java` — +1 validação
- `src/test/java/com/kts/kronos/application/scheduler/DayOffSchedulerTest.java` — +4 testes

### Frontend
- `src/features/collaborators/create/constants.ts` — +1 option
- `src/features/collaborators/create/components/CustomDaysDesktop.tsx` — NOVO
- `src/features/collaborators/create/components/CustomDaysMobile.tsx` — NOVO
- `src/features/collaborators/create/components/CustomDaysSelector.tsx` — NOVO
- `src/features/collaborators/create/components/CreateCollaboratorDesktop.tsx` — integrar
- `src/features/collaborators/create/components/CreateCollaboratorMobile.tsx` — integrar
