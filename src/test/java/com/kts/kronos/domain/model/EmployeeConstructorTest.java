package com.kts.kronos.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class EmployeeConstructorTest {

    @Test
    void shouldCreateEmployeeUsingConvenienceConstructor() {
        var employee = new Employee(
                "Ana Paula",
                "12345678901",
                "12345678901",
                "Analista",
                "ana@kts.com",
                1000.0,
                "11999999999",
                true,
                null,
                UUID.randomUUID(),
                null,
                false,
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0),
                null,
                null,
                null,
                null,
                null
        );

        assertNotNull(employee.employeeId());
        assertNull(employee.faceS3ObjectKey());
        assertNull(employee.scheduleType());
    }
}
