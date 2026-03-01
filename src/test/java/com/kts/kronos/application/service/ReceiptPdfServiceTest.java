package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReceiptPdfServiceTest {

    @Test
    void generateReceiptReturnsNonEmptyPdf() {
        ReceiptPdfService service = new ReceiptPdfService();
        Company company = new Company(UUID.randomUUID(), "Empresa", "12345678000100", "a@a.com", true,
                new Address("Rua", "1", "12345678", "Cidade", "ST"), new Location(1.0, 2.0), 0, 0);
        Employee employee = new Employee(UUID.randomUUID(), "Nome", "12345678901", "12345678901", "Dev", "e@e.com", 1.0,
                "11999999999", true, new Address("Rua", "1", "12345678", "Cidade", "ST"), company.companyId(),
                LocalDateTime.now(), false, null, LocalTime.of(8,0), LocalTime.of(17,0), LocalTime.of(12,0), LocalTime.of(13,0),
                null, null, null, null, null);

        byte[] pdf = service.generateReceipt(company, employee, LocalDateTime.now(), 10L);
        assertTrue(pdf.length > 0);
    }
}
