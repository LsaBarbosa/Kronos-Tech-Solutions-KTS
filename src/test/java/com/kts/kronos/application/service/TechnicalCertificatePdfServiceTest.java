package com.kts.kronos.application.service;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Company;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.ERROR_GENERATING_TECHNICAL_CERTIFICATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

class TechnicalCertificatePdfServiceTest {

    private TechnicalCertificatePdfService service;

    @BeforeEach
    void setUp() {
        service = new TechnicalCertificatePdfService();
        ReflectionTestUtils.setField(service, "devName", "KRONOS TEST LTDA");
        ReflectionTestUtils.setField(service, "devCnpj", "11111111000111");
        ReflectionTestUtils.setField(service, "softwareVersion", "2.5.0");
    }

    @Test
    @DisplayName("generateCertificate: deve gerar PDF com dados configurados")
    void shouldGenerateTechnicalCertificatePdf() {
        byte[] pdf = service.generateCertificate(company("12345678000199"));

        assertTrue(pdf.length > 0);
    }

    @Test
    @DisplayName("generateCertificate: deve tolerar campos nulos do cliente")
    void shouldGenerateTechnicalCertificatePdfWithNullClientValues() {
        byte[] pdf = service.generateCertificate(company(null));

        assertTrue(pdf.length > 0);
    }

    @Test
    @DisplayName("generateCertificate: deve entrar no catch quando cliente é nulo")
    void shouldEnterCatchWhenClientCompanyIsNull() {
        assertThrows(NullPointerException.class, () -> service.generateCertificate(null));
    }

    @Test
    @DisplayName("generateCertificate: deve encapsular falha de IO ao carregar fonte")
    void shouldWrapIOExceptionDuringPdfGeneration() {
        Company company = company("12345678000199");

        try (var mocked = mockStatic(PdfFontFactory.class)) {
            mocked.when(() -> PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                    .thenThrow(new IOException("font failed"));

            RuntimeException exception = assertThrows(RuntimeException.class, () -> service.generateCertificate(company));

            assertEquals(ERROR_GENERATING_TECHNICAL_CERTIFICATE, exception.getMessage());
            assertEquals(IOException.class, exception.getCause().getClass());
        }
    }

    @Test
    @DisplayName("formatCnpj: deve formatar apenas CNPJ com 14 dígitos")
    void shouldFormatCnpjOnlyWhenItHasFourteenDigits() {
        assertEquals("12.345.678/0001-99",
                ReflectionTestUtils.invokeMethod(service, "formatCnpj", "12345678000199"));
        assertEquals("123", ReflectionTestUtils.invokeMethod(service, "formatCnpj", "123"));
        assertNull(ReflectionTestUtils.invokeMethod(service, "formatCnpj", (String) null));
    }

    private static Company company(String cnpj) {
        return new Company(
                UUID.randomUUID(),
                "Kronos Cliente",
                cnpj,
                "contato@cliente.com",
                true,
                new Address("Rua A", "100", "65000000", "Sao Luis", "MA"),
                null,
                0,
                0
        );
    }
}
