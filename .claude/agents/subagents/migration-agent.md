# Subagent: Migration Flyway — CUSTOM_DAYS

## Status: NÃO NECESSÁRIA

A coluna `fixed_work_days` **já existe** em `tb_employee` (mapeada em `EmployeeEntity.java` linha 102).

```java
// EmployeeEntity.java linha 101-103
// Salvamos a lista de dias (ex: "MONDAY,TUESDAY") como texto no banco
@Column(name = "fixed_work_days")
private String fixedWorkDays;
```

O campo já é persistido como VARCHAR comma-separated e convertido por `convertStringToSet` / `convertSetToString`.

## Última migration existente: V50

```
V50__add_terminal_flag_to_company.sql
```

## Se eventualmente precisar de migration (cenário hipotético)

Por exemplo, para adicionar constraint ou índice:

```sql
-- V51__add_custom_days_constraint.sql
-- Não necessário no estado atual — apenas documentação preventiva

-- Futura adição de CHECK constraint para scheduleType=CUSTOM_DAYS:
-- ALTER TABLE tb_employee ADD CONSTRAINT chk_custom_days
--     CHECK (
--         schedule_type != 'CUSTOM_DAYS'
--         OR (fixed_work_days IS NOT NULL AND fixed_work_days != '')
--     );
```

## Ação requerida por este subagent

**Nenhuma.** O campo já existe. Confirmar apenas que o banco está em estado esperado:

```bash
sudo -u postgres psql -d kronos_prod -c \
  "\d tb_employee" | grep "fixed_work_days"
# Deve mostrar: fixed_work_days | character varying | ...
```
