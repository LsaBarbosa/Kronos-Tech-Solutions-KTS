package com.kts.kronos.application.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Company;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.ADDRESS_NOT_REGISTERED;
import static com.kts.kronos.constants.Messages.COMPANY_NOT_FOUND;
import static com.kts.kronos.constants.Messages.ERROR_GENERATING_TECHNICAL_CERTIFICATE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TechnicalCertificateServiceTest {

    @Mock
    CompanyProvider companyProvider;

    @InjectMocks
    TechnicalCertificateService service;

    @Test
    void generateCertificateReturnsPdfBytesWithCompanyAddress() throws IOException {
        UUID id = UUID.randomUUID();
        Address address = new Address("Rua A", "123", "20000-000", "Rio de Janeiro", "RJ");
        Company company = new Company(id, "Empresa Teste", "12.345.678/0001-90", "contato@empresa.com", true, address, null, 0, 0);
        when(companyProvider.findById(id)).thenReturn(Optional.of(company));

        byte[] bytes = service.generateCertificate(id);
        String pdfText = extractPdfText(bytes);

        assertAll(
                () -> assertNotNull(bytes),
                () -> assertTrue(bytes.length > 0),
                () -> assertTrue(pdfText.contains("ATESTADO TÉCNICO E TERMO DE RESPONSABILIDADE")),
                () -> assertTrue(pdfText.contains("Empresa Teste")),
                () -> assertTrue(pdfText.contains("Rua A, 123 - Rio de Janeiro, RJ - 20000-000"))
        );
    }

    @Test
    void generateCertificateUsesFallbackWhenAddressIsNull() throws IOException {
        UUID id = UUID.randomUUID();
        Company company = new Company(id, "Empresa Sem Endereço", null, "email@teste.com", true, null, null, 0, 0);
        when(companyProvider.findById(id)).thenReturn(Optional.of(company));

        byte[] bytes = service.generateCertificate(id);
        String pdfText = extractPdfText(bytes);

        assertAll(
                () -> assertNotNull(bytes),
                () -> assertTrue(bytes.length > 0),
                () -> assertTrue(pdfText.contains(ADDRESS_NOT_REGISTERED))
        );
    }

    @Test
    void generateCertificateThrowsResourceNotFoundWhenCompanyDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(companyProvider.findById(id)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.generateCertificate(id));

        assertTrue(ex.getMessage().contains(COMPANY_NOT_FOUND));
    }

    @Test
    void generateCertificateWrapsUnexpectedErrorsDuringPdfGeneration() {
        UUID id = UUID.randomUUID();
        Company company = mock(Company.class);
        when(companyProvider.findById(id)).thenReturn(Optional.of(company));
        when(company.name()).thenThrow(new IllegalStateException("falha inesperada"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.generateCertificate(id));

        assertAll(
                () -> assertTrue(ex.getMessage().contains(ERROR_GENERATING_TECHNICAL_CERTIFICATE)),
                () -> assertNotNull(ex.getCause()),
                () -> assertTrue(ex.getCause() instanceof IllegalStateException)
        );
    }

    private String extractPdfText(byte[] bytes) throws IOException {
        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(bytes));
             PdfDocument pdfDocument = new PdfDocument(reader)) {
            StringBuilder content = new StringBuilder();
            for (int i = 1; i <= pdfDocument.getNumberOfPages(); i++) {
                content.append(PdfTextExtractor.getTextFromPage(pdfDocument.getPage(i)));
            }
            return content.toString();
        }
    }
}
