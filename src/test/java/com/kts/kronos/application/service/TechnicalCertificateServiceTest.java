package com.kts.kronos.application.service;

import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.domain.model.Company;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TechnicalCertificateServiceTest {

    @Mock private CompanyProvider companyProvider;
    @InjectMocks private TechnicalCertificateService certificateService;

    @Test
    @DisplayName("Deve gerar Atestado Técnico PDF")
    void shouldGenerateCertificate() {
        UUID compId = UUID.randomUUID();
        Company company = mock(Company.class);
        when(company.name()).thenReturn("Cliente Teste");
        when(companyProvider.findById(compId)).thenReturn(Optional.of(company));

        byte[] pdfBytes = certificateService.generateCertificate(compId);

        assertTrue(pdfBytes.length > 0);
        assertTrue(new String(pdfBytes, 0, 4).startsWith("%PDF"));
    }
}