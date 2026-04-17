package com.kts.kronos.adapter.in.web.dto.timerecord.vacation;

import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import com.kts.kronos.domain.model.enuns.WorkScheduleType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VacationRequestResponseTest {

    @Test
    void deveFalharQuandoPeriodoEstiverVazio() {
        var employee = employee();

        assertThrows(IllegalArgumentException.class,
                () -> VacationRequestResponse.fromConsolidatedPeriod(employee, List.of()));
    }

    @Test
    void deveConsolidarPeriodoOrdenandoEPreservandoIds() {
        var employee = employee();

        var day3 = timeRecord(30L, LocalDate.of(2026, 4, 3));
        var day1 = timeRecord(10L, LocalDate.of(2026, 4, 1));
        var day2 = timeRecord(20L, LocalDate.of(2026, 4, 2));

        var response = VacationRequestResponse.fromConsolidatedPeriod(
                employee,
                List.of(day3, day1, day2)
        );

        assertEquals(employee.employeeId(), response.employeeId());
        assertEquals(LocalDate.of(2026, 4, 1), response.startDate());
        assertEquals(LocalDate.of(2026, 4, 3), response.endDate());
        assertEquals(StatusRecord.REQUEST_VACATION.name(), response.status());
        assertEquals(List.of(10L, 20L, 30L), response.timeRecordIdsForApproval());
    }

    private static Employee employee() {
        return new Employee(
                UUID.randomUUID(),
                "Lucas",
                "12345678901",
                null,
                "Dev",
                "lucas@kts.com",
                1000.0,
                "21999999999",
                true,
                new Address("Rua A", "10", "65000000", "São Luís", "MA"),
                UUID.randomUUID(),
                null,
                false,
                null,
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0),
                WorkScheduleType.TRADITIONAL_5X2,
                null,
                null,
                null,
                Set.of()
        );
    }

    private static TimeRecord timeRecord(Long id, LocalDate day) {
        return new TimeRecord(
                id,
                day.atTime(9, 0),
                null,
                StatusRecord.REQUEST_VACATION,
                false,
                true,
                UUID.randomUUID(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}