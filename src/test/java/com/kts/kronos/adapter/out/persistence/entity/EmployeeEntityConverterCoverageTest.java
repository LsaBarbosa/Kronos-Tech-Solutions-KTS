package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.enuns.WorkScheduleType;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EmployeeEntityConverterCoverageTest {

    private Employee buildEmployee(Set<DayOfWeek> fixedWorkDays) {
        return new Employee(
                UUID.randomUUID(), "Ana", "12345678901", "98765432100",
                "Dev", "ana@kts.com", 5000.0, "11999999999", true,
                new Address("Rua A", "1", "01001000", "SP", "SP"),
                UUID.randomUUID(), LocalDateTime.now(), false, null,
                LocalTime.of(9, 0), LocalTime.of(18, 0),
                LocalTime.of(12, 0), LocalTime.of(13, 0),
                WorkScheduleType.TRADITIONAL_5X2, LocalDate.now(),
                DayOfWeek.MONDAY, null, fixedWorkDays
        );
    }

    // ── convertSetToString: days == null = TRUE → short-circuit → return null ──

    @Test
    void fromDomain_withNullFixedWorkDays_storesNullString() {
        Employee emp = buildEmployee(null); // null → days==null=TRUE → return null
        EmployeeEntity entity = EmployeeEntity.fromDomain(emp);
        assertNull(entity.getFixedWorkDays());
    }

    // ── convertSetToString: days.isEmpty()=TRUE → return null ─────────────────

    @Test
    void fromDomain_withEmptyFixedWorkDays_storesNullString() {
        Employee emp = buildEmployee(Set.of()); // empty → isEmpty()=TRUE → null
        EmployeeEntity entity = EmployeeEntity.fromDomain(emp);
        assertNull(entity.getFixedWorkDays());
    }

    // ── convertStringToSet: data.isBlank()=TRUE → emptySet ────────────────────

    @Test
    void toDomain_withBlankFixedWorkDays_returnsEmptySet() {
        Employee emp = buildEmployee(Set.of(DayOfWeek.MONDAY));
        EmployeeEntity entity = EmployeeEntity.fromDomain(emp);
        entity.setFixedWorkDays("   "); // blank → isBlank()=TRUE → emptySet
        Employee domain = entity.toDomain();
        assertTrue(domain.fixedWorkDays().isEmpty());
    }
}
