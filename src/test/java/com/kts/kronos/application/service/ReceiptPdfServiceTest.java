package com.kts.kronos.application.service;

import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReceiptPdfServiceTest {

    @InjectMocks
    private ReceiptPdfService receiptPdfService;

    @Test
    @DisplayName("Deve gerar um PDF válido (não vazio)")
    void shouldGeneratePdf() {
        Company company = mock(Company.class);
        when(company.name()).thenReturn("Empresa Teste");
        when(company.cnpj()).thenReturn("00000000000100");

        Employee employee = mock(Employee.class);
        when(employee.fullName()).thenReturn("João da Silva");
        when(employee.cpf()).thenReturn("12345678900");

        byte[] pdfBytes = receiptPdfService.generateReceipt(company, employee, LocalDateTime.now(), 123L);

        assertTrue(pdfBytes.length > 0, "O PDF gerado está vazio");
        
        // Verifica assinatura mágica do PDF (%PDF)
        String header = new String(pdfBytes, 0, 4);
        assertTrue(header.startsWith("%PDF"), "O arquivo gerado não é um PDF válido");
    }
}