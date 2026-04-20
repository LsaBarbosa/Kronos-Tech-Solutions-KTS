package com.kts.kronos.application;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.service.TechnicalCertificateService;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Company;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.ADDRESS_NOT_REGISTERED;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TechnicalCertificateServiceTest {

    @InjectMocks
    private TechnicalCertificateService service;

    @Mock
    private CompanyProvider companyProvider;

    @Test
    @DisplayName("generateCertificate: deve gerar PDF com dados da empresa e endereco")
    void shouldGenerateCertificateWithCompanyAddress() throws Exception {
        UUID companyId = UUID.randomUUID();
        Company company = company(companyId, new Address("Rua A", "100", "65000000", "Sao Luis", "MA"));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));

        byte[] pdf = service.generateCertificate(companyId);

        String text = extractText(pdf);
        assertTrue(text.contains("ATESTADO TÉCNICO"));
        assertTrue(text.contains("Kronos Cliente"));
        assertTrue(text.contains("12345678000199"));
        assertTrue(text.contains("Rua A, 100 - Sao Luis, MA - 65000000"));
    }

    @Test
    @DisplayName("generateCertificate: deve gerar PDF com endereco nao cadastrado")
    void shouldGenerateCertificateWhenAddressIsMissing() throws Exception {
        UUID companyId = UUID.randomUUID();
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company(companyId, null)));

        byte[] pdf = service.generateCertificate(companyId);

        String text = extractText(pdf);
        assertTrue(text.contains(ADDRESS_NOT_REGISTERED));
    }

    @Test
    @DisplayName("generateCertificate: deve falhar quando empresa nao existir")
    void shouldThrowWhenCompanyDoesNotExist() {
        UUID companyId = UUID.randomUUID();
        when(companyProvider.findById(companyId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.generateCertificate(companyId));
    }

    private static Company company(UUID companyId, Address address) {
        return new Company(
                companyId,
                "Kronos Cliente",
                "12345678000199",
                "contato@cliente.com",
                true,
                address,
                null,
                0,
                0
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
