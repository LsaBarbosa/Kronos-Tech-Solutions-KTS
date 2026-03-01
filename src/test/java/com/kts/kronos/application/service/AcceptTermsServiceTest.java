package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Document;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.enuns.DocumentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcceptTermsServiceTest {

    @Mock EmployeeProvider employeeProvider;
    @Mock CompanyProvider companyProvider;
    @Mock BiometricTermPdfService pdfService;
    @Mock DocumentProvider documentProvider;
    @Mock S3StorageProvider s3StorageProvider;
    @Mock AuditLogProvider auditLogProvider;

    @InjectMocks AcceptTermsService service;

    @Test
    void hasAcceptedBiometricTermReturnsTrueWhenProviderReturnsTrue() {
        UUID employeeId = UUID.randomUUID();
        when(documentProvider.existsByEmployeeIdAndType(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM)).thenReturn(true);

        assertTrue(service.hasAcceptedBiometricTerm(employeeId));
    }

    @Test
    void hasAcceptedBiometricTermReturnsFalseWhenProviderReturnsFalse() {
        UUID employeeId = UUID.randomUUID();
        when(documentProvider.existsByEmployeeIdAndType(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM)).thenReturn(false);

        assertFalse(service.hasAcceptedBiometricTerm(employeeId));
    }

    @Test
    void acceptBiometricTermsReturnsEarlyWhenAlreadyAccepted() throws IOException {
        UUID employeeId = UUID.randomUUID();
        Document existing = mock(Document.class);
        when(documentProvider.findLatestByEmployeeIdAndType(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM)).thenReturn(existing);

        service.acceptBiometricTerms(employeeId, "1.1.1.1", "UA");

        verify(employeeProvider, never()).findByIdForUpdate(any());
        verify(documentProvider, never()).save(any());
        verify(auditLogProvider, never()).registerLog(any());
    }

    @Test
    void acceptBiometricTermsGeneratesAndPersistsDocumentAndAudit() throws IOException {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        Employee employee = new Employee(employeeId, "Nome", "12345678901", "12345678901", "Dev", "mail@kts.com", 1000.0,
                "11999999999", true, new Address("Rua", "1", "12345678", "Cidade", "ST"), companyId,
                LocalDateTime.now(), false, null, LocalTime.of(8,0), LocalTime.of(17,0), LocalTime.of(12,0), LocalTime.of(13,0),
                null, null, null, null, null);
        Company company = new Company(companyId, "KTS", "12345678000100", "mail@kts.com", true,
                new Address("Rua", "1", "12345678", "Cidade", "ST"), new Location(1.0, 2.0), 0, 0);

        when(documentProvider.findLatestByEmployeeIdAndType(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(null)
                .thenReturn(null);
        when(employeeProvider.findByIdForUpdate(employeeId)).thenReturn(java.util.Optional.of(employee));
        when(companyProvider.findById(companyId)).thenReturn(java.util.Optional.of(company));
        when(pdfService.generateConsentTerm(employee, company, "1.1.1.1", "UA")).thenReturn(new byte[]{1,2,3});
        when(s3StorageProvider.uploadFile(anyString(), any())).thenReturn("legal/path.pdf");

        service.acceptBiometricTerms(employeeId, "1.1.1.1", "UA");

        verify(documentProvider).save(any(Document.class));
        verify(auditLogProvider).registerLog(any());
    }
}
