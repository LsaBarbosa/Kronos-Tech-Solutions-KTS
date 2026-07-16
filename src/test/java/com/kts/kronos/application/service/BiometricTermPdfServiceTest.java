package com.kts.kronos.application.service;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.LegalText;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.application.security.PrivacyLogReferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

class BiometricTermPdfServiceTest {

    private BiometricTermPdfService service;

    @BeforeEach
    void setUp() {
        service = new BiometricTermPdfService(new PrivacyLogReferenceService("test-log-secret"));
        ReflectionTestUtils.setField(service, "secretSalt", "test-salt");
    }

    @Test
    @DisplayName("generateConsentTerm: deve gerar PDF usando fallbacks de IP e user agent")
    void shouldGenerateConsentTermWithFallbackAuditData() {
        byte[] pdf = service.generateConsentTerm(
                employee(UUID.randomUUID(), UUID.randomUUID()),
                company(UUID.randomUUID()),
                "",
                "",
                legalText()
        );

        assertTrue(pdf.length > 0);
    }

    @Test
    @DisplayName("generateConsentTerm: deve truncar user agent longo")
    void shouldGenerateConsentTermWithLongUserAgent() {
        byte[] pdf = service.generateConsentTerm(
                employee(UUID.randomUUID(), UUID.randomUUID()),
                company(UUID.randomUUID()),
                "198.51.100.42",
                "Mozilla/5.0 ".repeat(20),
                legalText()
        );

        assertTrue(pdf.length > 0);
    }

    @Test
    @DisplayName("bytesToHex: deve preservar zero à esquerda")
    void shouldConvertBytesToHexWithLeadingZero() {
        String hex = ReflectionTestUtils.invokeMethod(service, "bytesToHex", new byte[]{0x0f, (byte) 0xa0});

        assertEquals("0fa0", hex);
    }

    @Test
    @DisplayName("calculateSha256: deve traduzir ausência de SHA-256")
    void shouldWrapMissingSha256Algorithm() {
        try (var mocked = org.mockito.Mockito.mockStatic(java.security.MessageDigest.class)) {
            mocked.when(() -> java.security.MessageDigest.getInstance("SHA-256"))
                    .thenThrow(new java.security.NoSuchAlgorithmException("missing"));

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> ReflectionTestUtils.invokeMethod(service, "calculateSha256", "abc"));

            assertEquals("Erro ao calcular Hash SHA-256", exception.getMessage());
        }
    }

    @Test
    @DisplayName("generateConsentTerm: deve encapsular falha de geração")
    void shouldWrapRuntimeFailureDuringGeneration() {
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                service.generateConsentTerm(null, company(UUID.randomUUID()), "198.51.100.42", "JUnit", legalText()));

        assertEquals("Falha na geração do Termo PDF: ", exception.getMessage());
    }

    @Test
    @DisplayName("generateConsentTerm: deve encapsular falha de IO ao carregar fonte")
    void shouldWrapIOExceptionDuringPdfGeneration() {
        Employee employee = employee(UUID.randomUUID(), UUID.randomUUID());
        Company company = company(UUID.randomUUID());

        try (var mocked = mockStatic(PdfFontFactory.class)) {
            mocked.when(() -> PdfFontFactory.createFont(StandardFonts.HELVETICA))
                    .thenThrow(new IOException("font failed"));

            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                    service.generateConsentTerm(employee, company, "198.51.100.42", "JUnit", legalText()));

            assertEquals("Falha na geração do Termo PDF: ", exception.getMessage());
            assertEquals(IOException.class, exception.getCause().getClass());
        }
    }

    @Test
    @DisplayName("generateConsentTerm: deve usar fallback quando userAgent é null")
    void shouldGenerateConsentTermWithNullUserAgent() {
        byte[] pdf = service.generateConsentTerm(
                employee(UUID.randomUUID(), UUID.randomUUID()),
                company(UUID.randomUUID()),
                "198.51.100.1",
                null,
                legalText()
        );
        assertTrue(pdf.length > 0);
    }

    private static Company company(UUID companyId) {
        return new Company(
                companyId,
                "KTS",
                "12345678000199",
                "contato@kts.com",
                true,
                new Address("Rua A", "10", "65000000", "Sao Luis", "MA"),
                null,
                0,
                0
        );
    }

    private static Employee employee(UUID employeeId, UUID companyId) {
        return new Employee(
                employeeId,
                "Ana Paula",
                "12345678901",
                "12345678901",
                "Analista",
                "ana@kts.com",
                1000.0,
                "11999999999",
                true,
                new Address("Rua A", "10", "65000000", "Sao Luis", "MA"),
                companyId,
                null,
                false,
                null,
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
    }

    private static LegalText legalText() {
        return new LegalText(
                UUID.randomUUID(),
                DocumentType.BIOMETRIC_CONSENT_TERM,
                "2026.05.21",
                "Termo de Consentimento Biométrico",
                "Parágrafo inicial.\n\n- Item 1\n- Item 2",
                "current-hash",
                true,
                Instant.parse("2026-05-21T09:00:00Z"),
                Instant.parse("2026-05-21T09:05:00Z")
        );
    }
}
