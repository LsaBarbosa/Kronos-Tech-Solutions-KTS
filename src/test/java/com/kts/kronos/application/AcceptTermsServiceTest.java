package com.kts.kronos.application;

import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.DocumentUseCase;
import com.kts.kronos.application.port.out.provider.AuditLogProvider;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.service.AcceptTermsService;
import com.kts.kronos.application.service.BiometricTermPdfService;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Document;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.enuns.DocumentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcceptTermsServiceTest {

    @InjectMocks
    private AcceptTermsService service;

    @Mock
    private EmployeeProvider employeeProvider;
    @Mock
    private CompanyProvider companyProvider;
    @Mock
    private BiometricTermPdfService pdfService;
    @Mock
    private DocumentUseCase documentUseCase;
    @Mock
    private DocumentProvider documentProvider;
    @Mock
    private AuditLogProvider auditLogProvider;

    @Test
    void shouldPersistLegalDocumentUsingSingleCanonicalStorageAndAuditSameArtifact() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = buildEmployee(employeeId, companyId);
        Company company = buildCompany(companyId);
        byte[] pdfBytes = "pdf".getBytes();

        Document persistedDocument = new Document(
                UUID.randomUUID(),
                employeeId,
                DocumentType.BIOMETRIC_CONSENT_TERM,
                "Termo_Aceite_Biometria_12345678901.pdf",
                "application/pdf",
                "employee-id/receipts/uuid-Termo_Aceite_Biometria_12345678901.pdf",
                LocalDateTime.now(),
                null,
                false,
                false
        );

        when(documentProvider.existsByEmployeeIdAndType(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(false);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(pdfService.generateConsentTerm(employee, company, "10.0.0.1", "JUnit"))
                .thenReturn(pdfBytes);
        when(documentProvider.findByEmployeeAndType(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM, true))
                .thenReturn(List.of(persistedDocument));

        service.acceptBiometricTerms(employeeId, "10.0.0.1", "JUnit");

        verify(documentUseCase).uploadGeneratedDocument(
                DocumentType.BIOMETRIC_CONSENT_TERM,
                employeeId,
                null,
                pdfBytes,
                "Termo_Aceite_Biometria_12345678901.pdf"
        );

        ArgumentCaptor<com.kts.kronos.domain.model.AuditLog> auditCaptor =
                ArgumentCaptor.forClass(com.kts.kronos.domain.model.AuditLog.class);

        verify(auditLogProvider).registerLog(auditCaptor.capture());
        assertEquals(
                "Documento gerado e armazenado em: " + persistedDocument.storagePath(),
                auditCaptor.getValue().details()
        );
    }

    @Test
    void shouldNotCreateDuplicateArtifactWhenTermAlreadyExists() {
        UUID employeeId = UUID.randomUUID();

        when(documentProvider.existsByEmployeeIdAndType(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(true);

        service.acceptBiometricTerms(employeeId, "10.0.0.1", "JUnit");

        verifyNoInteractions(employeeProvider, companyProvider, pdfService, documentUseCase, auditLogProvider);
    }

    private Employee buildEmployee(UUID employeeId, UUID companyId) {
        return new Employee(
                employeeId,
                "Nome",
                "12345678901",
                "12345678901",
                "Dev",
                "dev@kts.com",
                1000.0,
                "11999999999",
                true,
                null,
                companyId,
                null,
                false,
                null,
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0),
                null,
                null,
                null,
                null,
                null
        );
    }

    private Company buildCompany(UUID companyId) {
        return new Company(
                companyId,
                "KTS",
                "00000000000100",
                "empresa@kts.com",
                true,
                null,
                null,
                0,
                0
        );
    }
}