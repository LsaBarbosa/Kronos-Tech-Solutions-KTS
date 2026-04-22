package com.kts.kronos.application.service;

import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReceiptPdfServiceTest {

    private final ReceiptPdfService service = new ReceiptPdfService();

    @Test
    @DisplayName("generateReceipt: deve gerar PDF de comprovante")
    void shouldGenerateReceiptPdf() {
        UUID companyId = UUID.randomUUID();

        byte[] pdf = service.generateReceipt(
                company(companyId, "KTS", "12345678000199"),
                employee(UUID.randomUUID(), companyId, "Ana Paula", "12345678901"),
                LocalDateTime.of(2026, 4, 21, 8, 15),
                123L
        );

        assertTrue(pdf.length > 0);
    }

    @Test
    @DisplayName("generateReceipt: deve tolerar campos opcionais nulos no texto")
    void shouldGenerateReceiptPdfWithNullDisplayValues() {
        UUID companyId = UUID.randomUUID();

        byte[] pdf = service.generateReceipt(
                company(companyId, null, null),
                employee(UUID.randomUUID(), companyId, null, null),
                LocalDateTime.of(2026, 4, 21, 8, 15),
                123L
        );

        assertTrue(pdf.length > 0);
    }

    @Test
    @DisplayName("calculateSha256: deve calcular hash estável")
    void shouldCalculateStableHash() {
        String hash = ReflectionTestUtils.invokeMethod(service, "calculateSha256", "abc");

        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", hash);
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
    @DisplayName("generateReceipt: deve encapsular falha durante geração")
    void shouldWrapGenerationFailure() {
        UUID companyId = UUID.randomUUID();

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.generateReceipt(
                company(companyId, "KTS", "12345678000199"),
                employee(UUID.randomUUID(), companyId, "Ana Paula", "12345678901"),
                null,
                123L
        ));

        assertEquals("Erro na geração do comprovante de ponto", exception.getMessage());
    }

    private static Company company(UUID companyId, String name, String cnpj) {
        return new Company(
                companyId,
                name,
                cnpj,
                "contato@kts.com",
                true,
                new Address("Rua A", "10", "65000000", "Sao Luis", "MA"),
                null,
                0,
                0
        );
    }

    private static Employee employee(UUID employeeId, UUID companyId, String name, String cpf) {
        return new Employee(
                employeeId,
                name,
                cpf,
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
}
