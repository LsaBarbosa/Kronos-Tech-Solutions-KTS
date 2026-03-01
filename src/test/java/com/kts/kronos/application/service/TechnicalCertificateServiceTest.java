package com.kts.kronos.application.service;

import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.domain.model.Company;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TechnicalCertificateServiceTest {

    @Mock CompanyProvider companyProvider;
    @InjectMocks TechnicalCertificateService service;

    @Test
    void generateCertificateReturnsPdfBytes() {
        UUID id = UUID.randomUUID();
        Company company = org.mockito.Mockito.mock(Company.class);
        when(companyProvider.findById(id)).thenReturn(Optional.of(company));
        byte[] bytes = service.generateCertificate(id);
        assertNotNull(bytes);
    }
}
