package com.kts.kronos.adapter.in.web.dto.employee;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.enuns.WorkScheduleType;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmployeeResponseTest {

    @Test
    void fromDomain_shouldReturnMaskedCpf() {
        Employee employee = employee();

        EmployeeResponse response = EmployeeResponse.fromDomain(employee, "Kronos Tech", "MANAGER");

        assertNotEquals(employee.cpf(), response.maskedCpf());
        assertFalse(response.maskedCpf().contains(employee.cpf()));
        assertEquals("***.456.789-**", response.maskedCpf());
    }

    @Test
    void shouldNotExposeFaceS3ObjectKey() throws Exception {
        Employee employee = employee();
        EmployeeResponse response = EmployeeResponse.fromDomain(employee, "Kronos Tech", "MANAGER");
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        String json = objectMapper.writeValueAsString(response);

        assertFalse(json.contains("faceS3ObjectKey"));
        assertFalse(json.contains("\"cpf\""));
        assertTrue(json.contains("\"maskedCpf\""));
    }

    private static Employee employee() {
        return new Employee(
                UUID.randomUUID(),
                "Maria Silva",
                "12345678901",
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
                "faces/maria.jpg",
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
