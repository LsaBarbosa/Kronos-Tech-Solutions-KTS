package com.kts.kronos.application;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.kts.kronos.application.service.BiometricTermPdfService;
import com.kts.kronos.application.service.ReceiptPdfService;
import com.kts.kronos.application.service.TechnicalCertificatePdfService;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfGenerationServicesTest {

    @Test
    @DisplayName("BiometricTermPdfService: deve gerar termo com dados das partes e auditoria")
    void shouldGenerateBiometricConsentTermWithAuditData() throws Exception {
        BiometricTermPdfService service = new BiometricTermPdfService();
        ReflectionTestUtils.setField(service, "secretSalt", "test-salt");
        Company company = company("Kronos Cliente", "12345678000199");
        Employee employee = employee("Ana Paula", "12345678901");
        String longUserAgent = "Mozilla/5.0 ".repeat(12);

        byte[] pdf = service.generateConsentTerm(employee, company, "192.0.2.10", longUserAgent);

        String text = extractText(pdf);
        assertTrue(text.contains("TERMO DE CONSENTIMENTO"));
        assertTrue(text.contains("Kronos Cliente"));
        assertTrue(text.contains("Ana Paula"));
        assertTrue(text.contains("192.0.2.10"));
        assertTrue(text.contains("CÓDIGO DE VALIDAÇÃO"));
    }

    @Test
    @DisplayName("BiometricTermPdfService: deve usar fallback para IP e user agent ausentes")
    void shouldGenerateBiometricConsentTermWithFallbackAuditData() throws Exception {
        BiometricTermPdfService service = new BiometricTermPdfService();
        ReflectionTestUtils.setField(service, "secretSalt", "test-salt");

        byte[] pdf = service.generateConsentTerm(employee("Bruno Lima", "98765432100"), company("Empresa Sem IP", "11222333000144"), null, "");

        String text = extractText(pdf);
        assertTrue(text.contains("IP Não Identificado"));
        assertTrue(text.contains("Dispositivo Desconhecido"));
    }

    @Test
    @DisplayName("TechnicalCertificatePdfService: deve gerar atestado tecnico com CNPJ formatado")
    void shouldGenerateTechnicalCertificatePdfWithFormattedCnpj() throws Exception {
        TechnicalCertificatePdfService service = new TechnicalCertificatePdfService();
        ReflectionTestUtils.setField(service, "devName", "KRONOS TESTE LTDA");
        ReflectionTestUtils.setField(service, "devCnpj", "00.000.000/0001-00");
        ReflectionTestUtils.setField(service, "softwareVersion", "2.5");

        byte[] pdf = service.generateCertificate(company("Padaria Central", "12345678000199"));

        String text = extractText(pdf);
        assertTrue(text.contains("ATESTADO TÉCNICO"));
        assertTrue(text.contains("KRONOS TESTE LTDA"));
        assertTrue(text.contains("KRONOS SYSTEM v2.5"));
        assertTrue(text.contains("Padaria Central"));
        assertTrue(text.contains("12.345.678/0001-99"));
    }

    @Test
    @DisplayName("TechnicalCertificatePdfService: deve aceitar CNPJ nulo sem falhar")
    void shouldGenerateTechnicalCertificatePdfWhenClientCnpjIsNull() {
        TechnicalCertificatePdfService service = new TechnicalCertificatePdfService();
        ReflectionTestUtils.setField(service, "devName", "KRONOS TESTE LTDA");
        ReflectionTestUtils.setField(service, "devCnpj", "00.000.000/0001-00");
        ReflectionTestUtils.setField(service, "softwareVersion", "2.5");
        Company company = company("Cliente Sem CNPJ", null);

        assertDoesNotThrow(() -> service.generateCertificate(company));
    }

    @Test
    @DisplayName("ReceiptPdfService: deve gerar comprovante com dados de ponto e hash")
    void shouldGenerateReceiptPdfWithTimeRecordData() throws Exception {
        ReceiptPdfService service = new ReceiptPdfService();
        LocalDateTime recordDate = LocalDateTime.of(2026, 4, 20, 8, 15, 30);

        byte[] pdf = service.generateReceipt(
                company("Kronos Cliente", "12345678000199"),
                employee("Carla Souza", "52998224725"),
                recordDate,
                123L
        );

        String text = extractText(pdf);
        assertTrue(text.contains("Comprovante de Registro de Ponto"));
        assertTrue(text.contains("Kronos Cliente"));
        assertTrue(text.contains("Carla Souza"));
        assertTrue(text.contains("20/04/2026 08:15:30"));
        assertTrue(text.contains("123"));
        assertTrue(text.contains("Código Hash"));
    }

    @Test
    @DisplayName("ReceiptPdfService: deve aceitar valores opcionais nulos no PDF")
    void shouldGenerateReceiptPdfWithNullOptionalValues() {
        ReceiptPdfService service = new ReceiptPdfService();
        Company company = company(null, null);
        Employee employee = employee("Funcionario Sem CPF", null);

        assertDoesNotThrow(() -> service.generateReceipt(company, employee, LocalDateTime.of(2026, 4, 20, 12, 0), 1L));
    }

    private static Company company(String name, String cnpj) {
        return new Company(
                UUID.randomUUID(),
                name,
                cnpj,
                "contato@cliente.com",
                true,
                new Address("Rua A", "100", "65000000", "Sao Luis", "MA"),
                null,
                0,
                0
        );
    }

    private static Employee employee(String fullName, String cpf) {
        return new Employee(
                UUID.randomUUID(),
                fullName,
                cpf,
                "12345678901",
                "Analista",
                "employee@kronos.com",
                1000.0,
                "11999999999",
                true,
                new Address("Rua B", "200", "65000001", "Sao Luis", "MA"),
                UUID.randomUUID(),
                null,
                false,
                null,
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0),
                null,
                LocalDate.of(2026, 1, 1),
                DayOfWeek.SUNDAY,
                1,
                Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)
        );
    }

    private static String extractText(byte[] pdfBytes) throws Exception {
        try (var reader = new PdfReader(new ByteArrayInputStream(pdfBytes));
             var pdf = new PdfDocument(reader)) {
            var text = new StringBuilder();
            for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
                text.append(PdfTextExtractor.getTextFromPage(pdf.getPage(page)));
            }
            return text.toString();
        }
    }
}
