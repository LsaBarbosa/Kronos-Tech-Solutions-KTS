package com.kts.kronos.domain.model;

import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import com.kts.kronos.domain.model.enuns.WorkScheduleType;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DomainModelBehaviorTest {

    @Test
    void deveAlterarNumeroNoAddress() {
        var address = new Address("Rua A", "10", "65000000", "São Luís", "MA");

        var updated = address.withNumber("20");

        assertEquals("20", updated.number());
        assertEquals("Rua A", updated.street());
    }

    @Test
    void deveCriarAuditLogComTimestamp() {
        var log = AuditLog.create(UUID.randomUUID(), "LOGIN", "127.0.0.1", "JUnit", "ok");

        assertEquals("LOGIN", log.action());
        assertNotNull(log.timestamp());
    }

    @Test
    void deveAplicarWithersDeCompanyEDocument() {
        var company = new Company(
                UUID.randomUUID(),
                "KTS",
                "12345678000199",
                "contato@kts.com",
                true,
                new Address("Rua A", "10", "65000000", "São Luís", "MA"),
                new Location(-2.53, -44.30),
                0,
                0
        );

        var companyInactive = company.withActive(false).withEmployeeCounts(5, 2);

        assertFalse(companyInactive.active());
        assertEquals(5, companyInactive.activeEmployees());
        assertEquals(2, companyInactive.inactiveEmployees());

        var doc = new Document(
                UUID.randomUUID(),
                UUID.randomUUID(),
                DocumentType.TIME_OFF,
                "arquivo.pdf",
                "application/pdf",
                "docs/arquivo.pdf",
                LocalDateTime.now(),
                10L,
                false,
                false
        );

        assertTrue(doc.markDeletedByEmployee().deletedByEmployee());
        assertTrue(doc.markDeletedByManager().deletedByManager());
    }

    @Test
    void deveAplicarWithersDoEmployeeECalcularCargaDiaria() {
        var now = LocalDateTime.now();
        var employee = new Employee(
                UUID.randomUUID(),
                "Lucas",
                "12345678901",
                "12345678901",
                "Dev",
                "lucas@kts.com",
                1000.0,
                "21999999999",
                true,
                new Address("Rua A", "10", "65000000", "São Luís", "MA"),
                UUID.randomUUID(),
                now,
                false,
                null,
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0),
                WorkScheduleType.TRADITIONAL_5X2,
                LocalDate.of(2026, 4, 1),
                DayOfWeek.SUNDAY,
                1,
                Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)
        );

        assertEquals(480, employee.getDailyWorkMinutes());
        assertEquals("novo@kts.com", employee.withEmail("novo@kts.com").email());
        assertEquals("21988888888", employee.withPhone("21988888888").phone());
        assertEquals("obj-key", employee.withFaceS3ObjectKey("obj-key").faceS3ObjectKey());
        assertFalse(employee.withActive(false).active());
        assertEquals("20", employee.withAddress(new Address("Rua A", "20", "65000000", "São Luís", "MA")).address().number());
        assertNotNull(employee.withLastSeenMessageTimestamp(LocalDateTime.now()).lastSeenMessageTimestamp());
    }

    @Test
    void deveAplicarWithersDoTimeRecordEUser() {
        var employeeId = UUID.randomUUID();
        var timeRecord = new TimeRecord(employeeId)
                .withCheckin(LocalDateTime.of(2026, 4, 17, 8, 0))
                .withCheckout(LocalDateTime.of(2026, 4, 17, 17, 0), -2.5, -44.3, 200L)
                .withStatus(StatusRecord.CREATED)
                .withEdited(true)
                .withActive(false)
                .withId(10L);

        assertEquals(10L, timeRecord.timeRecordId());
        assertEquals(StatusRecord.CREATED, timeRecord.statusRecord());
        assertTrue(timeRecord.edited());
        assertFalse(timeRecord.active());
        assertEquals(200L, timeRecord.nsrCheckout());

        var user = new User(
                UUID.randomUUID(),
                "lucas",
                "encoded",
                Role.MANAGER,
                true,
                employeeId
        );

        assertFalse(user.withActive(false).active());
        assertEquals("novo-encoded", user.withPassword("novo-encoded").password());
    }

    @Test
    void deveAplicarRegrasDoStatusRecord() {
        assertEquals(StatusRecord.CREATED, StatusRecord.PENDING.onCheckout());
        assertEquals(StatusRecord.UPDATED, StatusRecord.CREATED.onUpdate());
        assertEquals(StatusRecord.UPDATED, StatusRecord.UPDATED.onUpdate());
        assertEquals(StatusRecord.UPDATED, StatusRecord.PENDING.onUpdate());
        assertEquals(StatusRecord.UPDATED, StatusRecord.PENDING_APPROVAL.onUpdate());

        assertThrows(IllegalStateException.class, () -> StatusRecord.DAY_OFF.onCheckout());
        assertThrows(IllegalStateException.class, () -> StatusRecord.DAY_OFF.onUpdate());
    }
}