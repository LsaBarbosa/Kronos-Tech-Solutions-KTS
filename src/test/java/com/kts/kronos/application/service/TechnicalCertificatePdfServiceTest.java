package com.kts.kronos.application.service;

import com.kts.kronos.domain.model.Company;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TechnicalCertificatePdfServiceTest {

    @Test
    void generateCertificateShouldReturnPdfBytes() {
        var service = new TechnicalCertificatePdfService();
        ReflectionTestUtils.setField(service, "devName", "KTS DEV");
        ReflectionTestUtils.setField(service, "devCnpj", "00111222000199");
        ReflectionTestUtils.setField(service, "softwareVersion", "2.0.1");

        var company = new Company(UUID.randomUUID(), "Cliente XPTO", "12345678000195", "cliente@xpto.com", true, null, null, 0L, 0L);

        var pdf = service.generateCertificate(company);

        assertNotNull(pdf);
        assertTrue(pdf.length > 600);
    }
}
