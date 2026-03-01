package com.kts.kronos.application.service;

import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiometricTermPdfServiceTest {

    @Test
    void generateConsentTermShouldReturnPdfBytes() throws Exception {
        var fixedClock = Clock.fixed(Instant.parse("2025-01-01T12:00:00Z"), ZoneId.of("UTC"));
        var service = new BiometricTermPdfService(fixedClock);
        ReflectionTestUtils.setField(service, "secretSalt", "segredo-teste");

        var employee = new Employee(
                UUID.randomUUID(), "Maria Silva", "12345678900", "12345678901", "Analista",
                "maria@kts.com", 3200.0, "11999999999", true, null, UUID.randomUUID(),
                LocalDateTime.now(), true, null, LocalTime.of(9, 0), LocalTime.of(18, 0),
                LocalTime.of(12, 0), LocalTime.of(13, 0), null, null, null, null, null
        );

        var company = new Company(UUID.randomUUID(), "KTS", "00111222000199", "contato@kts.com", true, null, null, 0L, 0L);

        byte[] bytes = service.generateConsentTerm(employee, company, " ", "");

        assertNotNull(bytes);
        assertTrue(bytes.length > 1000);
    }
}
