package com.kts.kronos.application.service;

import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.DocumentUseCase;
import com.kts.kronos.application.port.out.provider.AuditLogProvider;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.application.port.out.provider.FaceStorageProvider;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Document;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.enuns.DocumentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
    @Mock
    private FaceStorageProvider faceStorageProvider;
    @Mock
    private FaceRecognitionProvider faceRecognitionProvider;

    @Test
    @DisplayName("aceite: deve encerrar fluxo quando termo já existe")
    void shouldSkipGenerationWhenTermAlreadyExists() {
        UUID employeeId = UUID.randomUUID();
        when(documentProvider.existsByEmployeeIdAndType(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(true);

        service.acceptBiometricTerms(employeeId, "10.0.0.1", "JUnit");

        verifyNoInteractions(employeeProvider, companyProvider, pdfService, documentUseCase, auditLogProvider);
    }

    @Test
    @DisplayName("aceite: falha quando colaborador não existe")
    void shouldFailAcceptanceWhenEmployeeDoesNotExist() {
        UUID employeeId = UUID.randomUUID();
        when(documentProvider.existsByEmployeeIdAndType(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(false);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.acceptBiometricTerms(employeeId, "10.0.0.1", "JUnit"));
    }

    @Test
    @DisplayName("aceite: falha quando empresa não existe")
    void shouldFailAcceptanceWhenCompanyDoesNotExist() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = buildEmployee(employeeId, companyId, "12345678901");
        when(documentProvider.existsByEmployeeIdAndType(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(false);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.acceptBiometricTerms(employeeId, "10.0.0.1", "JUnit"));
    }

    @Test
    @DisplayName("aceite: deve gerar PDF, persistir documento e registrar auditoria")
    void shouldGenerateDocumentAndAuditWhenTermsAreAccepted() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = buildEmployee(employeeId, companyId, "12345678901");
        Company company = new Company(companyId, "KTS", "12345678000199", "contato@kts.com", true, null, null, 0, 0);
        byte[] pdfBytes = "pdf-content".getBytes(StandardCharsets.UTF_8);

        when(documentProvider.existsByEmployeeIdAndType(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM)).thenReturn(false);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(pdfService.generateConsentTerm(employee, company, "10.0.0.1", "JUnit-Agent")).thenReturn(pdfBytes);
        when(documentProvider.findByEmployeeAndType(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM, true))
                .thenReturn(List.of(
                        new Document(
                                UUID.randomUUID(),
                                employeeId,
                                DocumentType.BIOMETRIC_CONSENT_TERM,
                                "Termo_Aceite_Biometria_12345678901.pdf",
                                "application/pdf",
                                "legal/company/file.pdf",
                                LocalDateTime.now(),
                                null,
                                false,
                                false
                        )
                ));

        service.acceptBiometricTerms(employeeId, "10.0.0.1", "JUnit-Agent");

        verify(documentUseCase).uploadGeneratedDocument(
                eq(DocumentType.BIOMETRIC_CONSENT_TERM),
                eq(employeeId),
                isNull(),
                eq(pdfBytes),
                eq("Termo_Aceite_Biometria_12345678901.pdf")
        );

        ArgumentCaptor<com.kts.kronos.domain.model.AuditLog> auditCaptor =
                ArgumentCaptor.forClass(com.kts.kronos.domain.model.AuditLog.class);
        verify(auditLogProvider).registerLog(auditCaptor.capture());
        assertTrue(auditCaptor.getValue().details().contains("legal/company/file.pdf"));
        assertTrue(auditCaptor.getValue().action().contains("ACEITE_TERMOS_BIOMETRIA"));
    }

    @Test
    @DisplayName("aceite: falha quando documento recém-gerado não é encontrado")
    void shouldFailAcceptanceWhenPersistedDocumentMetadataIsMissing() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = buildEmployee(employeeId, companyId, "12345678901");
        Company company = new Company(companyId, "KTS", "12345678000199", "contato@kts.com", true, null, null, 0, 0);
        byte[] pdfBytes = "pdf-content".getBytes(StandardCharsets.UTF_8);

        when(documentProvider.existsByEmployeeIdAndType(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM)).thenReturn(false);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(pdfService.generateConsentTerm(employee, company, "10.0.0.1", "JUnit-Agent")).thenReturn(pdfBytes);
        when(documentProvider.findByEmployeeAndType(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM, true))
                .thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class,
                () -> service.acceptBiometricTerms(employeeId, "10.0.0.1", "JUnit-Agent"));
    }

    @Test
    @DisplayName("status: deve refletir se colaborador já aceitou o termo")
    void shouldReturnAcceptanceStatus() {
        UUID employeeId = UUID.randomUUID();
        when(documentProvider.existsByEmployeeIdAndType(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(true);

        assertTrue(service.hasAcceptedBiometricTerm(employeeId));
        verify(documentProvider).existsByEmployeeIdAndType(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM);
        verify(employeeProvider, never()).findById(any());
    }

    @Test
    @DisplayName("revogação: falha quando colaborador não existe")
    void shouldFailRevocationWhenEmployeeDoesNotExist() {
        UUID employeeId = UUID.randomUUID();
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.revokeBiometricTerms(employeeId, "10.0.0.1", "JUnit-Agent"));
    }

    @Test
    @DisplayName("revogação: deve remover artefatos biométricos, documentos e registrar auditoria")
    void shouldRevokeBiometricArtifactsAndAudit() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = buildEmployee(employeeId, companyId, "12345678901").withFaceS3ObjectKey("faces/employee/image.jpg");
        UUID documentId = UUID.randomUUID();

        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(documentProvider.findByEmployeeAndType(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM, true))
                .thenReturn(List.of(
                        new Document(
                                documentId,
                                employeeId,
                                DocumentType.BIOMETRIC_CONSENT_TERM,
                                "termo.pdf",
                                "application/pdf",
                                "legal/company/file.pdf",
                                LocalDateTime.now(),
                                null,
                                false,
                                false
                        )
                ));

        service.revokeBiometricTerms(employeeId, "10.0.0.1", "JUnit-Agent");

        verify(faceStorageProvider).deleteFaceImage("faces/employee/image.jpg");
        verify(faceRecognitionProvider).deleteFacesByExternalImageId(employeeId);
        verify(employeeProvider).save(employee.withFaceS3ObjectKey(null));
        verify(documentProvider).delete(employeeId, documentId);

        ArgumentCaptor<com.kts.kronos.domain.model.AuditLog> auditCaptor =
                ArgumentCaptor.forClass(com.kts.kronos.domain.model.AuditLog.class);
        verify(auditLogProvider).registerLog(auditCaptor.capture());
        assertTrue(auditCaptor.getValue().details().contains("purgados"));
    }

    private Employee buildEmployee(UUID employeeId, UUID companyId, String cpf) {
        return new Employee(
                employeeId,
                "Teste",
                cpf,
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
}
