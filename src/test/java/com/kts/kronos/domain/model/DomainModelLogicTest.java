package com.kts.kronos.domain.model;

import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.domain.model.enuns.WorkScheduleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainModelLogicTest {

    @Test
    @DisplayName("Address.withNumber: deve retornar novo endereço com número atualizado")
    void shouldUpdateAddressNumber() {
        Address address = new Address("Rua A", "10", "12345678", "Rio", "RJ");

        Address updated = address.withNumber("99");

        assertEquals("10", address.number());
        assertEquals("99", updated.number());
        assertEquals(address.street(), updated.street());
    }

    @Test
    @DisplayName("Company.withActive / withEmployeeCounts: devem retornar novos estados")
    void shouldUpdateCompanyFlagsAndCounts() {
        Company company = new Company(
                UUID.randomUUID(),
                "Kronos Tech",
                "12345678000199",
                "contato@kronos.com",
                true,
                new Address("Rua A", "10", "12345678", "Rio", "RJ"),
                new Location(-22.90, -43.20),
                0,
                0
        );

        Company inactive = company.withActive(false);
        Company counted = company.withEmployeeCounts(7, 2);

        assertTrue(company.active());
        assertFalse(inactive.active());
        assertEquals(7, counted.activeEmployees());
        assertEquals(2, counted.inactiveEmployees());
    }

    @Test
    @DisplayName("User.withActive / withPassword: devem retornar novos estados")
    void shouldUpdateUserState() {
        User user = new User(
                UUID.randomUUID(),
                "john",
                "old-password",
                Role.MANAGER,
                true,
                UUID.randomUUID()
        );

        User inactive = user.withActive(false);
        User updatedPassword = user.withPassword("new-password");

        assertTrue(user.active());
        assertFalse(inactive.active());
        assertEquals("old-password", user.password());
        assertEquals("new-password", updatedPassword.password());
    }

    @Test
    @DisplayName("Document.markDeletedByEmployee / markDeletedByManager: devem marcar flags")
    void shouldMarkDocumentAsDeleted() {
        Document document = new Document(
                UUID.randomUUID(),
                UUID.randomUUID(),
                DocumentType.PAYSLIP,
                "holerite.pdf",
                "application/pdf",
                "docs/holerite.pdf",
                LocalDateTime.of(2026, 4, 17, 9, 0),
                10L,
                false,
                false
        );

        Document deletedByEmployee = document.markDeletedByEmployee();
        Document deletedByManager = document.markDeletedByManager();

        assertFalse(document.deletedByEmployee());
        assertFalse(document.deletedByManager());

        assertTrue(deletedByEmployee.deletedByEmployee());
        assertFalse(deletedByEmployee.deletedByManager());

        assertFalse(deletedByManager.deletedByEmployee());
        assertTrue(deletedByManager.deletedByManager());
    }

    @Test
    @DisplayName("Employee withers: devem preservar imutabilidade")
    void shouldApplyEmployeeWithers() {
        Employee employee = buildEmployee();

        Employee withFace = employee.withFaceS3ObjectKey("faces/new-face.jpg");
        Employee withEmail = employee.withEmail("novo@kronos.com");
        Employee withPhone = employee.withPhone("21988888888");
        Employee inactive = employee.withActive(false);

        assertEquals("faces/original.jpg", employee.faceS3ObjectKey());
        assertEquals("faces/new-face.jpg", withFace.faceS3ObjectKey());

        assertEquals("maria@kronos.com", employee.email());
        assertEquals("novo@kronos.com", withEmail.email());

        assertEquals("21999999999", employee.phone());
        assertEquals("21988888888", withPhone.phone());

        assertTrue(employee.active());
        assertFalse(inactive.active());
    }

    @Test
    @DisplayName("Employee.getDailyWorkMinutes: deve descontar intervalo")
    void shouldCalculateDailyWorkMinutesSubtractingBreak() {
        Employee employee = buildEmployee();

        long minutes = employee.getDailyWorkMinutes();

        assertEquals(480, minutes);
    }

    @Test
    @DisplayName("Employee.getDailyWorkMinutes: deve retornar 480 quando horários forem nulos")
    void shouldReturnDefaultDailyMinutesWhenTimesAreNull() {
        Employee employee = new Employee(
                UUID.randomUUID(),
                "Maria Silva",
                "12345678909",
                "12345678901",
                "Developer",
                "maria@kronos.com",
                5000.0,
                "21999999999",
                true,
                new Address("Rua A", "10", "12345678", "Rio", "RJ"),
                UUID.randomUUID(),
                LocalDateTime.of(2026, 4, 17, 8, 0),
                true,
                "faces/original.jpg",
                null,
                null,
                null,
                null,
                WorkScheduleType.TRADITIONAL_5X2,
                LocalDate.of(2026, 4, 1),
                DayOfWeek.MONDAY,
                2,
                Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)
        );

        assertEquals(480, employee.getDailyWorkMinutes());
    }

    private Employee buildEmployee() {
        return new Employee(
                UUID.randomUUID(),
                "Maria Silva",
                "12345678909",
                "12345678901",
                "Developer",
                "maria@kronos.com",
                5000.0,
                "21999999999",
                true,
                new Address("Rua A", "10", "12345678", "Rio", "RJ"),
                UUID.randomUUID(),
                LocalDateTime.of(2026, 4, 17, 8, 0),
                true,
                "faces/original.jpg",
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0),
                WorkScheduleType.TRADITIONAL_5X2,
                LocalDate.of(2026, 4, 1),
                DayOfWeek.MONDAY,
                2,
                Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)
        );
    }
}