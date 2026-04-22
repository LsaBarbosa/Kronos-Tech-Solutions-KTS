package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TimeRecordResponseTest {

    @Test
    @DisplayName("fromDomain: deve usar saldo diário informado")
    void shouldUseProvidedDailyBalance() {
        UUID employeeId = UUID.randomUUID();
        TimeRecord record = record(
                employeeId,
                StatusRecord.CREATED,
                LocalDateTime.of(2026, 4, 21, 8, 0),
                LocalDateTime.of(2026, 4, 21, 17, 30)
        );
        EmployeeData employeeData = new EmployeeData("Ana", "KTS");

        TimeRecordResponse response = TimeRecordResponse.fromDomain(
                record,
                Duration.ofHours(8),
                employeeData,
                "/documents/1",
                "+01:30"
        );

        assertEquals("08:00", response.startHour());
        assertEquals("17:30", response.endHour());
        assertEquals("09:30", response.hoursWork());
        assertEquals("+01:30", response.balance());
        assertEquals(employeeData, response.employeeData());
        assertEquals("/documents/1", response.documentDownloadPath());
        assertEquals(employeeId, response.employeeId());
    }

    @Test
    @DisplayName("fromDomain: deve deixar campos de saída vazios quando não há checkout")
    void shouldLeaveCheckoutFieldsEmptyWhenRecordIsOpen() {
        TimeRecord record = record(
                UUID.randomUUID(),
                StatusRecord.PENDING,
                LocalDateTime.of(2026, 4, 21, 8, 0),
                null
        );

        TimeRecordResponse response = TimeRecordResponse.fromDomain(
                record,
                Duration.ofHours(8),
                null,
                null,
                null
        );

        assertNull(response.endWork());
        assertEquals("", response.endHour());
        assertEquals("", response.hoursWork());
        assertEquals("", response.balance());
    }

    @Test
    @DisplayName("fromDomain: deve calcular ausência como saldo negativo")
    void shouldCalculateAbsenceAsNegativeBalance() {
        TimeRecord record = record(
                UUID.randomUUID(),
                StatusRecord.ABSENCE,
                LocalDateTime.of(2026, 4, 21, 0, 0),
                LocalDateTime.of(2026, 4, 21, 0, 0)
        );

        TimeRecordResponse response = TimeRecordResponse.fromDomain(
                record,
                Duration.ofHours(8),
                null,
                null,
                null
        );

        assertEquals("-08:00", response.balance());
    }

    @Test
    @DisplayName("fromDomain: deve zerar saldo para status abonados")
    void shouldUseZeroBalanceForZeroBalanceStatuses() {
        TimeRecord record = record(
                UUID.randomUUID(),
                StatusRecord.DAY_OFF,
                LocalDateTime.of(2026, 4, 21, 0, 0),
                LocalDateTime.of(2026, 4, 21, 0, 0)
        );

        TimeRecordResponse response = TimeRecordResponse.fromDomain(
                record,
                Duration.ofHours(8),
                null,
                null,
                null
        );

        assertEquals("+00:00", response.balance());
    }

    @Test
    @DisplayName("fromDomain: deve calcular saldo individual quando dailyBalance não é informado")
    void shouldCalculateIndividualBalanceWhenDailyBalanceIsMissing() {
        TimeRecord record = record(
                UUID.randomUUID(),
                StatusRecord.CREATED,
                LocalDateTime.of(2026, 4, 21, 8, 0),
                LocalDateTime.of(2026, 4, 21, 15, 30)
        );

        TimeRecordResponse response = TimeRecordResponse.fromDomain(
                record,
                Duration.ofHours(8),
                null,
                null,
                null
        );

        assertEquals("-00:30", response.balance());
    }

    private static TimeRecord record(
            UUID employeeId,
            StatusRecord status,
            LocalDateTime start,
            LocalDateTime end
    ) {
        return new TimeRecord(
                10L,
                start,
                end,
                status,
                true,
                true,
                employeeId,
                -2.53,
                -44.30,
                -2.54,
                -44.31,
                100L,
                101L,
                null,
                null
        );
    }
}
