package com.kts.kronos.application.service;

import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.AuditLogProvider;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.S3StorageProvider;
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
import java.util.Optional;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.COMPANY_NOT_FOUND;
import static com.kts.kronos.constants.Messages.EMPLOYEE_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.*;
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
    void acceptBiometricTermsReturnsEarlyWhenConsentAlreadyExistsBeforeLock() throws IOException {
        UUID employeeId = UUID.randomUUID();
        when(documentProvider.findLatestByEmployeeIdAndType(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(existingDocument(employeeId));

        service.acceptBiometricTerms(employeeId, "127.0.0.1", "agent");

        verify(employeeProvider, never()).findByIdForUpdate(any());
        verifyNoInteractions(pdfService);
        verify(documentProvider, never()).save(any());
        verify(auditLogProvider, never()).registerLog(any());
    }

    @Test
    void acceptBiometricTermsReturnsEarlyWhenConsentAppearsAfterLock() throws IOException {
        UUID employeeId = UUID.randomUUID();
        Employee employee = employee(employeeId, UUID.randomUUID());

        when(documentProvider.findLatestByEmployeeIdAndType(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(null, existingDocument(employeeId));
        when(employeeProvider.findByIdForUpdate(employeeId)).thenReturn(Optional.of(employee));

        service.acceptBiometricTerms(employeeId, "127.0.0.1", "agent");

        verify(employeeProvider).findByIdForUpdate(employeeId);
        verifyNoInteractions(pdfService);
        verify(documentProvider, never()).save(any());
        verify(auditLogProvider, never()).registerLog(any());
    }

    @Test
    void acceptBiometricTermsThrowsWhenEmployeeNotFound() {
        UUID employeeId = UUID.randomUUID();

        when(documentProvider.findLatestByEmployeeIdAndType(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(null);
        when(employeeProvider.findByIdForUpdate(employeeId)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.acceptBiometricTerms(employeeId, "127.0.0.1", "agent"));

        assertEquals(EMPLOYEE_NOT_FOUND, ex.getMessage());
        verify(companyProvider, never()).findById(any());
    }

    @Test
    void acceptBiometricTermsThrowsWhenCompanyNotFound() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = employee(employeeId, companyId);

        when(documentProvider.findLatestByEmployeeIdAndType(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn((Document) null, (Document) null);
        when(employeeProvider.findByIdForUpdate(employeeId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.acceptBiometricTerms(employeeId, "127.0.0.1", "agent"));

        assertEquals(COMPANY_NOT_FOUND, ex.getMessage());
        verifyNoInteractions(pdfService);
    }

    @Test
    void acceptBiometricTermsCreatesDocumentAndAuditOnSuccess() throws IOException {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = employee(employeeId, companyId);
        Company company = company(companyId);
        byte[] pdf = new byte[]{1, 2, 3};

        when(documentProvider.findLatestByEmployeeIdAndType(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn((Document) null, (Document) null);
        when(employeeProvider.findByIdForUpdate(employeeId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(pdfService.generateConsentTerm(employee, company, "127.0.0.1", "agent")).thenReturn(pdf);
        when(s3StorageProvider.uploadFile(anyString(), eq(pdf))).thenReturn("s3://bucket/legal/file.pdf");

        service.acceptBiometricTerms(employeeId, "127.0.0.1", "agent");

        verify(documentProvider).save(argThat(doc ->
                doc.employeeId().equals(employeeId)
                        && doc.type() == DocumentType.BIOMETRIC_CONSENT_TERM
                        && doc.fileName().contains(employee.cpf())
                        && "application/pdf".equals(doc.contentType())
                        && "s3://bucket/legal/file.pdf".equals(doc.storagePath())
        ));
        verify(auditLogProvider).registerLog(argThat(audit ->
                audit.userId().equals(employeeId)
                        && "ACEITE_TERMOS_BIOMETRIA".equals(audit.action())
                        && "127.0.0.1".equals(audit.ipAddress())
                        && "agent".equals(audit.userAgent())
                        && audit.details().contains("s3://bucket/legal/file.pdf")
        ));
    }

    @Test
    void acceptBiometricTermsPropagatesIOExceptionFromPdfGeneration() throws IOException {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = employee(employeeId, companyId);
        Company company = company(companyId);

        when(documentProvider.findLatestByEmployeeIdAndType(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn((Document) null, (Document) null);
        when(employeeProvider.findByIdForUpdate(employeeId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(pdfService.generateConsentTerm(any(), any(), anyString(), anyString()))
                .thenThrow(new IOException("pdf error"));

        assertThrows(IOException.class, () -> service.acceptBiometricTerms(employeeId, "127.0.0.1", "agent"));

        verify(s3StorageProvider, never()).uploadFile(anyString(), any());
        verify(documentProvider, never()).save(any());
        verify(auditLogProvider, never()).registerLog(any());
    }

    @Test
    void acceptBiometricTermsPropagatesRuntimeExceptionFromStorageUpload() throws IOException {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = employee(employeeId, companyId);
        Company company = company(companyId);
        byte[] pdf = new byte[]{1, 2, 3};

        when(documentProvider.findLatestByEmployeeIdAndType(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn((Document) null, (Document) null);
        when(employeeProvider.findByIdForUpdate(employeeId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(pdfService.generateConsentTerm(any(), any(), anyString(), anyString())).thenReturn(pdf);
        when(s3StorageProvider.uploadFile(anyString(), eq(pdf))).thenThrow(new RuntimeException("s3 down"));

        assertThrows(RuntimeException.class, () -> service.acceptBiometricTerms(employeeId, "127.0.0.1", "agent"));

        verify(documentProvider, never()).save(any());
        verify(auditLogProvider, never()).registerLog(any());
    }

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

    private Employee employee(UUID employeeId, UUID companyId) {
        return new Employee(
                employeeId,
                "Employee Name",
                "12345678900",
                "12345678901",
                "Developer",
                "employee@example.com",
                1500.0,
                "11999999999",
                true,
                null,
                companyId,
                null,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private Company company(UUID companyId) {
        return new Company(
                companyId,
                "KTS",
                "12345678000190",
                "company@example.com",
                true,
                null,
                null,
                1,
                0
        );
    }

    private Document existingDocument(UUID employeeId) {
        return new Document(
                UUID.randomUUID(),
                employeeId,
                DocumentType.BIOMETRIC_CONSENT_TERM,
                "consent.pdf",
                "application/pdf",
                "s3://bucket/existing.pdf",
                null,
                null,
                false,
                false
        );
    }
}
