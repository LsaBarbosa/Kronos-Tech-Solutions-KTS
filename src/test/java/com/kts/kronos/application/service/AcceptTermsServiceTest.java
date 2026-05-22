package com.kts.kronos.application.service;

import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.DocumentUseCase;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.application.port.out.provider.FaceStorageProvider;
import com.kts.kronos.application.port.out.provider.LegalConsentProvider;
import com.kts.kronos.application.port.out.provider.LegalTextProvider;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Document;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.LegalConsent;
import com.kts.kronos.domain.model.LegalText;
import com.kts.kronos.domain.model.enuns.AuditAction;
import com.kts.kronos.domain.model.enuns.ConsentType;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.LegalBasis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    private AuditService auditService;
    @Mock
    private FaceStorageProvider faceStorageProvider;
    @Mock
    private FaceRecognitionProvider faceRecognitionProvider;
    @Mock
    private LegalConsentProvider legalConsentProvider;
    @Mock
    private LegalTextProvider legalTextProvider;

    private static final HexFormat HEX = HexFormat.of();

    @Test
    @DisplayName("aceite: deve encerrar fluxo quando termo já existe")
    void shouldSkipGenerationWhenTermAlreadyExists() {
        UUID employeeId = UUID.randomUUID();
        when(legalConsentProvider.existsActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION))
                .thenReturn(true);

        service.acceptBiometricTerms(employeeId, UUID.randomUUID(), "10.0.0.1", "JUnit", "2026.05.21", "current-hash");

        verifyNoInteractions(employeeProvider, companyProvider, pdfService, documentUseCase, auditService, legalTextProvider);
    }

    @Test
    @DisplayName("aceite: falha quando colaborador não existe")
    void shouldFailAcceptanceWhenEmployeeDoesNotExist() {
        UUID employeeId = UUID.randomUUID();
        when(legalConsentProvider.existsActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION))
                .thenReturn(false);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.acceptBiometricTerms(employeeId, UUID.randomUUID(), "10.0.0.1", "JUnit", "2026.05.21", "current-hash"));
    }

    @Test
    @DisplayName("aceite: falha quando não existe termo biométrico ativo")
    void shouldFailAcceptanceWhenCurrentLegalTextDoesNotExist() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = buildEmployee(employeeId, companyId, "12345678901");
        when(legalConsentProvider.existsActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION))
                .thenReturn(false);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.acceptBiometricTerms(employeeId, UUID.randomUUID(), "10.0.0.1", "JUnit", "2026.05.21", "current-hash"));
    }

    @Test
    @DisplayName("aceite: falha quando versão ou hash não correspondem ao termo ativo")
    void shouldFailAcceptanceWhenCurrentLegalTextPayloadDoesNotMatch() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = buildEmployee(employeeId, companyId, "12345678901");
        when(legalConsentProvider.existsActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION))
                .thenReturn(false);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(currentBiometricTerm()));

        assertThrows(BadRequestException.class,
                () -> service.acceptBiometricTerms(employeeId, UUID.randomUUID(), "10.0.0.1", "JUnit", "2026.05.20", "wrong-hash"));
    }

    @Test
    @DisplayName("aceite: falha quando empresa não existe")
    void shouldFailAcceptanceWhenCompanyDoesNotExist() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = buildEmployee(employeeId, companyId, "12345678901");
        when(legalConsentProvider.existsActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION))
                .thenReturn(false);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(currentBiometricTerm()));
        when(companyProvider.findById(companyId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.acceptBiometricTerms(employeeId, UUID.randomUUID(), "10.0.0.1", "JUnit", "2026.05.21", "current-hash"));
    }

    @Test
    @DisplayName("aceite: deve gerar PDF, persistir documento e registrar auditoria")
    void shouldGenerateDocumentAndAuditWhenTermsAreAccepted() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = buildEmployee(employeeId, companyId, "12345678901");
        Company company = new Company(companyId, "KTS", "12345678000199", "contato@kts.com", true, null, null, 0, 0);
        byte[] pdfBytes = "pdf-content".getBytes(StandardCharsets.UTF_8);
        String expectedFilename = "Termo_Aceite_Biometria_" + employeeId + ".pdf";
        UUID documentId = UUID.randomUUID();
        LegalText currentBiometricTerm = currentBiometricTerm();

        when(legalConsentProvider.existsActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION)).thenReturn(false);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(currentBiometricTerm));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(pdfService.generateConsentTerm(employee, company, "10.0.0.1", "JUnit-Agent", currentBiometricTerm))
                .thenReturn(pdfBytes);
        when(documentProvider.findByEmployeeAndType(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM, true))
                .thenReturn(List.of(
                        new Document(
                                documentId,
                                employeeId,
                                DocumentType.BIOMETRIC_CONSENT_TERM,
                                expectedFilename,
                                "application/pdf",
                                "legal/company/file.pdf",
                                LocalDateTime.now(),
                                null,
                                false,
                                false
                        )
                ));
        when(legalConsentProvider.save(any(LegalConsent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.acceptBiometricTerms(employeeId, userId, "10.0.0.1", "JUnit-Agent", "2026.05.21", "current-hash");

        verify(documentUseCase).uploadGeneratedDocument(
                eq(DocumentType.BIOMETRIC_CONSENT_TERM),
                eq(employeeId),
                isNull(),
                eq(pdfBytes),
                eq(expectedFilename)
        );

        ArgumentCaptor<LegalConsent> consentCaptor = ArgumentCaptor.forClass(LegalConsent.class);
        verify(legalConsentProvider).save(consentCaptor.capture());
        assertEquals(userId, consentCaptor.getValue().userId());
        assertEquals(documentId, consentCaptor.getValue().evidenceDocumentId());
        assertEquals("10.0.0.1", consentCaptor.getValue().ipAddress());
        assertEquals("JUnit-Agent", consentCaptor.getValue().userAgent());
        assertEquals(ConsentType.BIOMETRIC_AUTHENTICATION, consentCaptor.getValue().consentType());
        assertEquals(LegalBasis.CONSENT, consentCaptor.getValue().legalBasis());
        assertEquals("2026.05.21", consentCaptor.getValue().version());
        assertEquals(sha256(pdfBytes), consentCaptor.getValue().evidenceHashSha256());

        ArgumentCaptor<String> auditDetailsCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditService).register(
                eq(AuditAction.BIOMETRIC_CONSENT_ACCEPTED),
                eq(employeeId),
                eq(companyId),
                eq("LEGAL_CONSENT"),
                eq(documentId.toString()),
                eq("MEDIUM"),
                eq("10.0.0.1"),
                eq("JUnit-Agent"),
                auditDetailsCaptor.capture()
        );
        assertFalse(expectedFilename.contains(employee.cpf()));
        assertTrue(auditDetailsCaptor.getValue().contains(documentId.toString()));
        assertTrue(auditDetailsCaptor.getValue().contains(DocumentType.BIOMETRIC_CONSENT_TERM.name()));
        assertFalse(auditDetailsCaptor.getValue().contains("company/"));
        assertFalse(auditDetailsCaptor.getValue().contains("employee/"));
        assertFalse(auditDetailsCaptor.getValue().contains("storagePath"));
        assertFalse(auditDetailsCaptor.getValue().contains("bucket"));
        assertFalse(auditDetailsCaptor.getValue().contains("legal/company/file.pdf"));
    }

    @Test
    @DisplayName("aceite: falha quando documento recém-gerado não é encontrado")
    void shouldFailAcceptanceWhenPersistedDocumentMetadataIsMissing() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = buildEmployee(employeeId, companyId, "12345678901");
        Company company = new Company(companyId, "KTS", "12345678000199", "contato@kts.com", true, null, null, 0, 0);
        byte[] pdfBytes = "pdf-content".getBytes(StandardCharsets.UTF_8);
        LegalText currentBiometricTerm = currentBiometricTerm();

        when(legalConsentProvider.existsActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION)).thenReturn(false);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(currentBiometricTerm));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(pdfService.generateConsentTerm(employee, company, "10.0.0.1", "JUnit-Agent", currentBiometricTerm))
                .thenReturn(pdfBytes);
        when(documentProvider.findByEmployeeAndType(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM, true))
                .thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class,
                () -> service.acceptBiometricTerms(employeeId, UUID.randomUUID(), "10.0.0.1", "JUnit-Agent", "2026.05.21", "current-hash"));
    }

    @Test
    @DisplayName("termo atual: deve retornar o texto biométrico ativo")
    void shouldReturnCurrentBiometricTerm() {
        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(currentBiometricTerm()));

        var result = service.getCurrentBiometricTerm();

        assertEquals("2026.05.21", result.version());
        assertEquals("current-hash", result.contentHashSha256());
    }

    @Test
    @DisplayName("status: deve retornar true quando existir consentimento ativo")
    void shouldReturnAcceptanceStatusWhenConsentIsActive() {
        UUID employeeId = UUID.randomUUID();
        when(legalConsentProvider.existsActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION))
                .thenReturn(true);

        assertTrue(service.hasAcceptedBiometricTerm(employeeId));
        verify(legalConsentProvider).existsActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION);
        verify(employeeProvider, never()).findById(any());
    }

    @Test
    @DisplayName("status: deve retornar false quando só existir documento legado")
    void shouldReturnFalseWhenOnlyLegacyDocumentExists() {
        UUID employeeId = UUID.randomUUID();
        when(legalConsentProvider.existsActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION))
                .thenReturn(false);

        assertFalse(service.hasAcceptedBiometricTerm(employeeId));
        verify(legalConsentProvider).existsActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION);
        verify(documentProvider, never()).existsByEmployeeIdAndType(any(), any());
    }

    @Test
    @DisplayName("status: deve retornar false quando o consentimento estiver revogado")
    void shouldReturnFalseWhenConsentIsRevoked() {
        UUID employeeId = UUID.randomUUID();
        when(legalConsentProvider.existsActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION))
                .thenReturn(false);

        assertFalse(service.hasAcceptedBiometricTerm(employeeId));
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
        var activeConsent = new LegalConsent(
                UUID.randomUUID(),
                employeeId,
                UUID.randomUUID(),
                ConsentType.BIOMETRIC_AUTHENTICATION,
                LegalBasis.CONSENT,
                "Biometric authentication",
                "2026.05.21",
                Instant.parse("2026-05-21T09:00:00Z"),
                null,
                "10.0.0.1",
                "JUnit-Agent",
                documentId,
                "hash",
                Instant.parse("2026-05-21T09:00:00Z"),
                null
        );
        when(legalConsentProvider.findActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION))
                .thenReturn(Optional.of(activeConsent));
        when(legalConsentProvider.save(any(LegalConsent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.revokeBiometricTerms(employeeId, "10.0.0.1", "JUnit-Agent");

        verify(faceStorageProvider).deleteFaceImage("faces/employee/image.jpg");
        verify(faceRecognitionProvider).deleteFacesByExternalImageId(employeeId);
        verify(employeeProvider).save(employee.withFaceS3ObjectKey(null));
        verify(documentProvider, never()).delete(any(), any());
        verify(legalConsentProvider).save(any(LegalConsent.class));

        ArgumentCaptor<String> auditDetailsCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditService).register(
                eq(AuditAction.BIOMETRIC_CONSENT_REVOKED),
                eq(employeeId),
                eq(companyId),
                eq("LEGAL_CONSENT"),
                eq(employeeId.toString()),
                eq("MEDIUM"),
                eq("10.0.0.1"),
                eq("JUnit-Agent"),
                auditDetailsCaptor.capture()
        );
        assertTrue(auditDetailsCaptor.getValue().contains("purgados"));
        assertTrue(auditDetailsCaptor.getValue().contains("evidenceDocumentsPreserved=true"));
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

    private static String sha256(byte[] payload) {
        try {
            return HEX.formatHex(MessageDigest.getInstance("SHA-256").digest(payload));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private LegalText currentBiometricTerm() {
        return new LegalText(
                UUID.randomUUID(),
                DocumentType.BIOMETRIC_CONSENT_TERM,
                "2026.05.21",
                "Termo de Consentimento Biométrico",
                "Parágrafo inicial.\n\n- Item 1\n- Item 2",
                "current-hash",
                true,
                Instant.parse("2026-05-21T09:00:00Z"),
                Instant.parse("2026-05-21T09:05:00Z")
        );
    }
}
