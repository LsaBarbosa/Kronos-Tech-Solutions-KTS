package com.kts.kronos.application.service;

import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.domain.model.enuns.DocumentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

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
}
