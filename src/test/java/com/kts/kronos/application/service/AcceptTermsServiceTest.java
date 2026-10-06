package com.kts.kronos.application.service;

import com.kts.kronos.application.security.PrivacyLogReferenceService;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class AcceptTermsServiceTest {


    @Mock
    private PrivacyLogReferenceService privacyLogReferenceService;

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
    @Mock
    private com.kts.kronos.application.port.out.provider.UserProvider userProvider;
    @Mock
    private com.kts.kronos.observability.application.KronosMetrics kronosMetrics;
    @Mock
    private com.kts.kronos.application.port.out.provider.CacheProvider cacheProvider;

    private static final HexFormat HEX = HexFormat.of();

    @Test
    @DisplayName("aceite: deve encerrar fluxo quando termo já existe")
    void shouldSkipGenerationWhenTermAlreadyExists() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LegalConsent existingConsent = buildConsent(employeeId);
        com.kts.kronos.domain.model.User user = new com.kts.kronos.domain.model.User(
                userId,
                "test@kts.com",
                "hash",
                com.kts.kronos.domain.model.enuns.Role.MANAGER,
                true,
                employeeId,
                1L,
                null,
                null,
                null
        );
        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(currentBiometricTerm()));
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.of(user));
        when(legalConsentProvider.findValidCurrentConsent(
                employeeId,
                ConsentType.BIOMETRIC_AUTHENTICATION,
                "2026.05.21",
                "current-hash"
        )).thenReturn(Optional.of(existingConsent));

        var result = service.acceptBiometricTerms(employeeId, userId, "10.0.0.1", "JUnit", "2026.05.21", "current-hash");

        assertEquals(employeeId, result.employeeId());
        assertEquals(userId, result.userId());
        verifyNoInteractions(employeeProvider, companyProvider, pdfService, documentUseCase, auditService);
    }

    @Test
    @DisplayName("aceite: falha quando colaborador não existe")
    void shouldFailAcceptanceWhenEmployeeDoesNotExist() {
        UUID employeeId = UUID.randomUUID();
        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(currentBiometricTerm()));
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.acceptBiometricTerms(employeeId, UUID.randomUUID(), "10.0.0.1", "JUnit", "2026.05.21", "current-hash"));
    }

    @Test
    @DisplayName("aceite: falha quando não existe termo biométrico ativo")
    void shouldFailAcceptanceWhenCurrentLegalTextDoesNotExist() {
        UUID employeeId = UUID.randomUUID();
        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.acceptBiometricTerms(employeeId, UUID.randomUUID(), "10.0.0.1", "JUnit", "2026.05.21", "current-hash"));
    }

    @Test
    @DisplayName("aceite: falha quando versão ou hash não correspondem ao termo ativo")
    void shouldFailAcceptanceWhenCurrentLegalTextPayloadDoesNotMatch() {
        UUID employeeId = UUID.randomUUID();
        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(currentBiometricTerm()));

        assertThrows(BadRequestException.class,
                () -> service.acceptBiometricTerms(employeeId, UUID.randomUUID(), "10.0.0.1", "JUnit", "2026.05.20", "wrong-hash"));
    }

    @Test
    @DisplayName("aceite: falha controlada quando termo atual não tem hash configurado")
    void shouldFailAcceptanceWhenCurrentLegalTextHashIsMissing() {
        UUID employeeId = UUID.randomUUID();
        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(currentBiometricTermWithHash(null)));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.acceptBiometricTerms(employeeId, UUID.randomUUID(), "10.0.0.1", "JUnit", "2026.05.21", "current-hash"));

        assertEquals("Current biometric term content hash is not configured", exception.getMessage());
    }

    @Test
    @DisplayName("aceite: falha quando empresa não existe")
    void shouldFailAcceptanceWhenCompanyDoesNotExist() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Employee employee = buildEmployee(employeeId, companyId, "12345678901");
        com.kts.kronos.domain.model.User user = new com.kts.kronos.domain.model.User(
                userId,
                "test@kts.com",
                "hash",
                com.kts.kronos.domain.model.enuns.Role.MANAGER,
                true,
                employeeId,
                1L,
                null,
                null,
                null
        );
        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(currentBiometricTerm()));
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.of(user));
        when(legalConsentProvider.findValidCurrentConsent(
                employeeId,
                ConsentType.BIOMETRIC_AUTHENTICATION,
                "2026.05.21",
                "current-hash"
        )).thenReturn(Optional.empty());
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.acceptBiometricTerms(employeeId, userId, "10.0.0.1", "JUnit", "2026.05.21", "current-hash"));
    }

    @Test
    @DisplayName("aceite: deve gerar PDF, persistir documento e registrar auditoria")
    void shouldGenerateDocumentAndAuditWhenTermsAreAccepted() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = buildEmployee(employeeId, companyId, "12345678901");
        Company company = new Company(companyId, "KTS", "12345678000199", "contato@kts.com", true, null, null, 0, 0);
        com.kts.kronos.domain.model.User user = new com.kts.kronos.domain.model.User(
                userId,
                "test@kts.com",
                "hash",
                com.kts.kronos.domain.model.enuns.Role.MANAGER,
                true,
                employeeId,
                1L,
                null,
                null,
                null
        );
        byte[] pdfBytes = "pdf-content".getBytes(StandardCharsets.UTF_8);
        String expectedFilename = "Termo_Aceite_Biometria_" + employeeId + ".pdf";
        UUID documentId = UUID.randomUUID();
        LegalText currentBiometricTerm = currentBiometricTerm();

        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(currentBiometricTerm));
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.of(user));
        when(legalConsentProvider.findValidCurrentConsent(
                employeeId,
                ConsentType.BIOMETRIC_AUTHENTICATION,
                "2026.05.21",
                "current-hash"
        )).thenReturn(Optional.empty());
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
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

        var result = service.acceptBiometricTerms(employeeId, userId, "10.0.0.1", "JUnit-Agent", "2026.05.21", "current-hash");

        assertEquals(employeeId, result.employeeId());
        assertEquals(userId, result.userId());

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
                eq(userId),
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
        UUID userId = UUID.randomUUID();
        Employee employee = buildEmployee(employeeId, companyId, "12345678901");
        Company company = new Company(companyId, "KTS", "12345678000199", "contato@kts.com", true, null, null, 0, 0);
        com.kts.kronos.domain.model.User user = new com.kts.kronos.domain.model.User(
                userId,
                "test@kts.com",
                "hash",
                com.kts.kronos.domain.model.enuns.Role.MANAGER,
                true,
                employeeId,
                1L,
                null,
                null,
                null
        );
        byte[] pdfBytes = "pdf-content".getBytes(StandardCharsets.UTF_8);
        LegalText currentBiometricTerm = currentBiometricTerm();

        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(currentBiometricTerm));
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.of(user));
        when(legalConsentProvider.findValidCurrentConsent(
                employeeId,
                ConsentType.BIOMETRIC_AUTHENTICATION,
                "2026.05.21",
                "current-hash"
        )).thenReturn(Optional.empty());
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(pdfService.generateConsentTerm(employee, company, "10.0.0.1", "JUnit-Agent", currentBiometricTerm))
                .thenReturn(pdfBytes);
        when(documentProvider.findByEmployeeAndType(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM, true))
                .thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class,
                () -> service.acceptBiometricTerms(employeeId, userId, "10.0.0.1", "JUnit-Agent", "2026.05.21", "current-hash"));
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
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = buildEmployee(employeeId, companyId, "12345678901").withFaceS3ObjectKey("faces/employee/image.jpg");
        com.kts.kronos.domain.model.User user = new com.kts.kronos.domain.model.User(
                userId,
                "test@kts.com",
                "hash",
                com.kts.kronos.domain.model.enuns.Role.MANAGER,
                true,
                employeeId,
                5L,
                null,
                null,
                null
        );
        UUID documentId = UUID.randomUUID();

        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.of(user));
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
                userId,
                ConsentType.BIOMETRIC_AUTHENTICATION,
                LegalBasis.CONSENT,
                "Biometric authentication",
                "2026.05.21",
                "abc123sha256content",
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
        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(currentBiometricTerm()));

        service.revokeBiometricTerms(employeeId, "10.0.0.1", "JUnit-Agent");

        verify(faceStorageProvider).deleteFaceImage("faces/employee/image.jpg");
        verify(faceRecognitionProvider).deleteFacesByExternalImageId(employeeId);
        verify(employeeProvider).save(employee.withFaceS3ObjectKey(null));
        verify(documentProvider, never()).delete(any(), any());
        verify(legalConsentProvider).save(any(LegalConsent.class));

        ArgumentCaptor<String> auditDetailsCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditService).register(
                eq(AuditAction.BIOMETRIC_CONSENT_REVOKED),
                eq(userId),
                eq(employeeId),
                eq(companyId),
                eq("LEGAL_CONSENT"),
                eq(employeeId.toString()),
                eq("HIGH"),
                eq("10.0.0.1"),
                eq("JUnit-Agent"),
                auditDetailsCaptor.capture()
        );
        assertTrue(auditDetailsCaptor.getValue().contains("purgados"));
        assertTrue(auditDetailsCaptor.getValue().contains("evidenceDocumentsPreserved=true"));
    }

    @Test
    @DisplayName("revogação: deve incrementar sessionVersion do usuário e retornar resultado de revogação")
    void revokeBiometricTerms_shouldIncrementUserSessionVersionAndReturnRevocationResult() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = buildEmployee(employeeId, companyId, "12345678901")
                .withFaceS3ObjectKey("faces/employee/image.jpg");
        com.kts.kronos.domain.model.User user = new com.kts.kronos.domain.model.User(
                userId,
                "manager@kts.com",
                "hash",
                com.kts.kronos.domain.model.enuns.Role.MANAGER,
                true,
                employeeId,
                5L,
                null,
                null,
                null
        );
        UUID documentId = UUID.randomUUID();

        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.of(user));
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
                userId,
                ConsentType.BIOMETRIC_AUTHENTICATION,
                LegalBasis.CONSENT,
                "Biometric authentication",
                "2026.05.21",
                "abc123sha256content",
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
        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(currentBiometricTerm()));

        var result = service.revokeBiometricTerms(employeeId, "10.0.0.1", "JUnit-Agent");

        ArgumentCaptor<com.kts.kronos.domain.model.User> userCaptor = ArgumentCaptor.forClass(com.kts.kronos.domain.model.User.class);
        verify(userProvider).save(userCaptor.capture());
        assertEquals(6L, userCaptor.getValue().sessionVersion());

        assertEquals(6L, result.newSessionVersion());
        assertEquals(employeeId, result.employeeId());
        assertEquals(userId, result.userId());

        ArgumentCaptor<String> auditDetailsCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditService).register(
                eq(AuditAction.BIOMETRIC_CONSENT_REVOKED),
                eq(userId),
                eq(employeeId),
                eq(companyId),
                eq("LEGAL_CONSENT"),
                eq(employeeId.toString()),
                eq("HIGH"),
                eq("10.0.0.1"),
                eq("JUnit-Agent"),
                auditDetailsCaptor.capture()
        );
        assertTrue(auditDetailsCaptor.getValue().contains("sessions_revoked") ||
                   auditDetailsCaptor.getValue().contains("sessões invalidadas"));

        verify(faceStorageProvider).deleteFaceImage("faces/employee/image.jpg");
        verify(faceRecognitionProvider).deleteFacesByExternalImageId(employeeId);
        verify(employeeProvider).save(employee.withFaceS3ObjectKey(null));
        verify(kronosMetrics).consentRevoked();
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

    private LegalConsent buildConsent(UUID employeeId) {
        return new LegalConsent(
                UUID.randomUUID(),
                employeeId,
                UUID.randomUUID(),
                ConsentType.BIOMETRIC_AUTHENTICATION,
                LegalBasis.CONSENT,
                "Biometric authentication and identity validation in authorized Kronos flows.",
                "2026.05.21",
                "current-hash",
                Instant.parse("2026-05-21T09:00:00Z"),
                null,
                "10.0.0.1",
                "JUnit",
                UUID.randomUUID(),
                "pdf-hash",
                Instant.parse("2026-05-21T09:00:00Z"),
                null
        );
    }

    @Test
    @DisplayName("getBiometricConsentStatus: sem consentimento ativo")
    void getBiometricConsentStatus_WithoutActiveConsent() {
        UUID employeeId = UUID.randomUUID();
        LegalText currentTerm = currentBiometricTerm();

        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(currentTerm));
        when(legalConsentProvider.findActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION))
                .thenReturn(Optional.empty());

        var result = service.getBiometricConsentStatus(employeeId);

        assertFalse(result.accepted());
        assertEquals(null, result.acceptedVersion());
        assertEquals(null, result.acceptedHash());
        assertEquals("2026.05.21", result.currentVersion());
        assertEquals("current-hash", result.currentHash());
        assertTrue(result.requiresNewAcceptance());
    }

    @Test
    @DisplayName("getBiometricConsentStatus: consentimento com versão e hash vigentes")
    void getBiometricConsentStatus_WithCurrentVersionAndHash() {
        UUID employeeId = UUID.randomUUID();
        LegalText currentTerm = currentBiometricTerm();
        LegalConsent activeConsent = new LegalConsent(
                UUID.randomUUID(),
                employeeId,
                UUID.randomUUID(),
                ConsentType.BIOMETRIC_AUTHENTICATION,
                LegalBasis.CONSENT,
                "Biometric authentication and identity validation in authorized Kronos flows.",
                "2026.05.21",
                "current-hash",
                Instant.parse("2026-05-21T09:00:00Z"),
                null,
                "10.0.0.1",
                "JUnit",
                UUID.randomUUID(),
                "pdf-hash",
                Instant.parse("2026-05-21T09:00:00Z"),
                null
        );

        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(currentTerm));
        when(legalConsentProvider.findActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION))
                .thenReturn(Optional.of(activeConsent));

        var result = service.getBiometricConsentStatus(employeeId);

        assertTrue(result.accepted());
        assertEquals("2026.05.21", result.acceptedVersion());
        assertEquals("current-hash", result.acceptedHash());
        assertEquals("2026.05.21", result.currentVersion());
        assertEquals("current-hash", result.currentHash());
        assertFalse(result.requiresNewAcceptance());
    }

    @Test
    @DisplayName("getBiometricConsentStatus: consentimento legado sem hash exige novo aceite")
    void getBiometricConsentStatus_WithNullConsentHashRequiresNewAcceptance() {
        UUID employeeId = UUID.randomUUID();
        LegalText currentTerm = currentBiometricTerm();
        LegalConsent activeConsent = activeBiometricConsent(employeeId, "2026.05.21", null);

        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(currentTerm));
        when(legalConsentProvider.findActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION))
                .thenReturn(Optional.of(activeConsent));

        var result = service.getBiometricConsentStatus(employeeId);

        assertFalse(result.accepted());
        assertEquals("2026.05.21", result.acceptedVersion());
        assertNull(result.acceptedHash());
        assertEquals("2026.05.21", result.currentVersion());
        assertEquals("current-hash", result.currentHash());
        assertTrue(result.requiresNewAcceptance());
    }

    @Test
    @DisplayName("getBiometricConsentStatus: consentimento legado com hash em branco exige novo aceite")
    void getBiometricConsentStatus_WithBlankConsentHashRequiresNewAcceptance() {
        UUID employeeId = UUID.randomUUID();
        LegalText currentTerm = currentBiometricTerm();
        LegalConsent activeConsent = activeBiometricConsent(employeeId, "2026.05.21", "");

        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(currentTerm));
        when(legalConsentProvider.findActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION))
                .thenReturn(Optional.of(activeConsent));

        var result = service.getBiometricConsentStatus(employeeId);

        assertFalse(result.accepted());
        assertEquals("2026.05.21", result.acceptedVersion());
        assertEquals("", result.acceptedHash());
        assertTrue(result.requiresNewAcceptance());
    }

    @Test
    @DisplayName("getBiometricConsentStatus: consentimento com versão desatualizada")
    void getBiometricConsentStatus_WithOldVersion() {
        UUID employeeId = UUID.randomUUID();
        LegalText currentTerm = currentBiometricTerm();
        LegalConsent activeConsent = new LegalConsent(
                UUID.randomUUID(),
                employeeId,
                UUID.randomUUID(),
                ConsentType.BIOMETRIC_AUTHENTICATION,
                LegalBasis.CONSENT,
                "Biometric authentication and identity validation in authorized Kronos flows.",
                "2026.05.20",
                "old-hash",
                Instant.parse("2026-05-20T09:00:00Z"),
                null,
                "10.0.0.1",
                "JUnit",
                UUID.randomUUID(),
                "pdf-hash",
                Instant.parse("2026-05-20T09:00:00Z"),
                null
        );

        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(currentTerm));
        when(legalConsentProvider.findActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION))
                .thenReturn(Optional.of(activeConsent));

        var result = service.getBiometricConsentStatus(employeeId);

        assertFalse(result.accepted());
        assertEquals("2026.05.20", result.acceptedVersion());
        assertEquals("old-hash", result.acceptedHash());
        assertEquals("2026.05.21", result.currentVersion());
        assertEquals("current-hash", result.currentHash());
        assertTrue(result.requiresNewAcceptance());
    }

    @Test
    @DisplayName("getBiometricConsentStatus: consentimento com versão antiga e hash atual exige novo aceite")
    void getBiometricConsentStatus_WithOldVersionAndCurrentHash() {
        UUID employeeId = UUID.randomUUID();
        LegalText currentTerm = currentBiometricTerm();
        LegalConsent activeConsent = activeBiometricConsent(employeeId, "2026.05.20", "current-hash");

        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(currentTerm));
        when(legalConsentProvider.findActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION))
                .thenReturn(Optional.of(activeConsent));

        var result = service.getBiometricConsentStatus(employeeId);

        assertFalse(result.accepted());
        assertEquals("2026.05.20", result.acceptedVersion());
        assertEquals("current-hash", result.acceptedHash());
        assertEquals("2026.05.21", result.currentVersion());
        assertEquals("current-hash", result.currentHash());
        assertTrue(result.requiresNewAcceptance());
    }

    @Test
    @DisplayName("getBiometricConsentStatus: consentimento com hash desatualizado")
    void getBiometricConsentStatus_WithOldHash() {
        UUID employeeId = UUID.randomUUID();
        LegalText currentTerm = currentBiometricTerm();
        LegalConsent activeConsent = new LegalConsent(
                UUID.randomUUID(),
                employeeId,
                UUID.randomUUID(),
                ConsentType.BIOMETRIC_AUTHENTICATION,
                LegalBasis.CONSENT,
                "Biometric authentication and identity validation in authorized Kronos flows.",
                "2026.05.21",
                "old-hash",
                Instant.parse("2026-05-21T09:00:00Z"),
                null,
                "10.0.0.1",
                "JUnit",
                UUID.randomUUID(),
                "pdf-hash",
                Instant.parse("2026-05-21T09:00:00Z"),
                null
        );

        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(currentTerm));
        when(legalConsentProvider.findActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION))
                .thenReturn(Optional.of(activeConsent));

        var result = service.getBiometricConsentStatus(employeeId);

        assertFalse(result.accepted());
        assertEquals("2026.05.21", result.acceptedVersion());
        assertEquals("old-hash", result.acceptedHash());
        assertEquals("2026.05.21", result.currentVersion());
        assertEquals("current-hash", result.currentHash());
        assertTrue(result.requiresNewAcceptance());
    }

    @Test
    @DisplayName("getBiometricConsentStatus: consentimento revogado")
    void getBiometricConsentStatus_WithRevokedConsent() {
        UUID employeeId = UUID.randomUUID();
        LegalText currentTerm = currentBiometricTerm();

        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(currentTerm));
        when(legalConsentProvider.findActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION))
                .thenReturn(Optional.empty());

        var result = service.getBiometricConsentStatus(employeeId);

        assertFalse(result.accepted());
        assertTrue(result.requiresNewAcceptance());
    }

    private LegalText currentBiometricTerm() {
        return currentBiometricTermWithHash("current-hash");
    }

    private LegalText currentBiometricTermWithHash(String contentHashSha256) {
        return new LegalText(
                UUID.randomUUID(),
                DocumentType.BIOMETRIC_CONSENT_TERM,
                "2026.05.21",
                "Termo de Consentimento Biométrico",
                "Parágrafo inicial.\n\n- Item 1\n- Item 2",
                contentHashSha256,
                true,
                Instant.parse("2026-05-21T09:00:00Z"),
                Instant.parse("2026-05-21T09:05:00Z")
        );
    }

    private LegalConsent activeBiometricConsent(UUID employeeId, String version, String contentHashSha256) {
        return new LegalConsent(
                UUID.randomUUID(),
                employeeId,
                UUID.randomUUID(),
                ConsentType.BIOMETRIC_AUTHENTICATION,
                LegalBasis.CONSENT,
                "Biometric authentication and identity validation in authorized Kronos flows.",
                version,
                contentHashSha256,
                Instant.parse("2026-05-21T09:00:00Z"),
                null,
                "10.0.0.1",
                "JUnit",
                UUID.randomUUID(),
                "pdf-hash",
                Instant.parse("2026-05-21T09:00:00Z"),
                null
        );
    }

    @Test
    @DisplayName("getConsentHistory: retorna lista de consentimentos do colaborador")
    void getConsentHistory_ReturnsConsentList() {
        UUID employeeId = UUID.randomUUID();
        var employee = buildEmployee(employeeId, UUID.randomUUID(), "12345678901");
        var consent = activeBiometricConsent(employeeId, "2026.05.21", "current-hash");

        when(employeeProvider.findById(employeeId)).thenReturn(java.util.Optional.of(employee));
        when(legalConsentProvider.findAllByEmployeeId(employeeId)).thenReturn(java.util.List.of(consent));

        var result = service.getConsentHistory(employeeId);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("getConsentHistory: lança exceção quando colaborador não existe")
    void getConsentHistory_ThrowsWhenEmployeeNotFound() {
        UUID employeeId = UUID.randomUUID();
        when(employeeProvider.findById(employeeId)).thenReturn(java.util.Optional.empty());
        assertThrows(com.kts.kronos.application.exceptions.ResourceNotFoundException.class,
                () -> service.getConsentHistory(employeeId));
    }

    @Test
    @DisplayName("getBiometricConsentStatus: consentimento com hash null → not accepted")
    void getBiometricConsentStatus_WithNullConsentHash() {
        UUID employeeId = UUID.randomUUID();
        LegalText currentTerm = currentBiometricTerm();
        // consent with null contentHashSha256
        LegalConsent nullHashConsent = new LegalConsent(
                UUID.randomUUID(), employeeId, UUID.randomUUID(),
                ConsentType.BIOMETRIC_AUTHENTICATION, LegalBasis.CONSENT,
                "Biometric authentication and identity validation in authorized Kronos flows.",
                "2026.05.21", null,
                java.time.Instant.parse("2026-05-21T09:00:00Z"), null,
                "10.0.0.1", "JUnit", UUID.randomUUID(), "pdf-hash",
                java.time.Instant.parse("2026-05-21T09:00:00Z"), null
        );

        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(java.util.Optional.of(currentTerm));
        when(legalConsentProvider.findActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION))
                .thenReturn(java.util.Optional.of(nullHashConsent));

        var result = service.getBiometricConsentStatus(employeeId);
        assertFalse(result.accepted());
        assertTrue(result.requiresNewAcceptance());
    }

    @Test
    @DisplayName("validateCurrentBiometricTerm: lança exceção quando hash é null")
    void validateCurrentBiometricTerm_ThrowsWhenHashIsNull() {
        UUID employeeId = UUID.randomUUID();
        LegalText termWithNullHash = currentBiometricTermWithHash(null);

        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(java.util.Optional.of(termWithNullHash));

        // acceptBiometricTerms triggers validateCurrentBiometricTerm
        assertThrows(IllegalStateException.class,
                () -> service.acceptBiometricTerms(employeeId, UUID.randomUUID(),
                        "127.0.0.1", "JUnit", "2026.05.21", "some-hash"));
    }

    @Test
    @DisplayName("revokeBiometricTerms: funciona quando faceS3ObjectKey é null (sem deleção S3)")
    void revokeBiometricTerms_WhenFaceKeyIsNull_SkipsS3Deletion() {
        UUID employeeId = UUID.randomUUID();
        var employeeNoFace = employeeWithNoFace(employeeId);
        var user = user(employeeId);
        var updatedUser = user.incrementSessionVersion();

        when(employeeProvider.findById(employeeId)).thenReturn(java.util.Optional.of(employeeNoFace));
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(java.util.Optional.of(user));
        when(legalConsentProvider.findActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION))
                .thenReturn(java.util.Optional.empty()); // no active consent
        when(employeeProvider.save(org.mockito.ArgumentMatchers.any())).thenReturn(employeeNoFace);
        when(documentProvider.findByEmployeeAndType(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM, true))
                .thenReturn(java.util.List.of());
        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(java.util.Optional.of(currentBiometricTerm()));
        when(legalConsentProvider.findActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION))
                .thenReturn(java.util.Optional.empty());

        var result = service.revokeBiometricTerms(employeeId, "127.0.0.1", "JUnit");
        assertNotNull(result);
    }

    @Test
    @DisplayName("invalidateConsentCaches: exceção de cache é silenciada")
    void invalidateConsentCaches_ExceptionIsSilenced() {
        UUID employeeId = UUID.randomUUID();
        var employeeNoFace = employeeWithNoFace(employeeId);
        var user = user(employeeId);
        var updatedUser = user.incrementSessionVersion();

        when(employeeProvider.findById(employeeId)).thenReturn(java.util.Optional.of(employeeNoFace));
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(java.util.Optional.of(user));
        when(legalConsentProvider.findActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION))
                .thenReturn(java.util.Optional.empty());
        when(employeeProvider.save(org.mockito.ArgumentMatchers.any())).thenReturn(employeeNoFace);
        when(documentProvider.findByEmployeeAndType(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM, true))
                .thenReturn(java.util.List.of());
        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(java.util.Optional.of(currentBiometricTerm()));
        when(legalConsentProvider.findActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION))
                .thenReturn(java.util.Optional.empty());
        // Make cache eviction throw → should be silenced
        doThrow(new RuntimeException("Redis unavailable"))
                .when(cacheProvider).evictNamespace(org.mockito.ArgumentMatchers.anyString());

        // Should NOT throw despite cache error
        assertDoesNotThrow(() -> service.revokeBiometricTerms(employeeId, "127.0.0.1", "JUnit"));
    }

    private com.kts.kronos.domain.model.Employee employeeWithNoFace(UUID employeeId) {
        return buildEmployee(employeeId, UUID.randomUUID(), "12345678901").withFaceS3ObjectKey(null);
    }

    private com.kts.kronos.domain.model.User user(UUID employeeId) {
        return new com.kts.kronos.domain.model.User(
                UUID.randomUUID(), "user@test.com", "hash",
                com.kts.kronos.domain.model.enuns.Role.PARTNER, true, employeeId);
    }


    // ── L99: employeeProvider.findById returns empty in acceptBiometricTerms ──
    @Test
    @DisplayName("aceite: lança ResourceNotFoundException quando employeeProvider não encontra o employee (user existe)")
    void acceptBiometricTerms_employeeNotFound_throwsAfterUserFound() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(currentBiometricTerm()));
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.of(user(employeeId)));
        when(legalConsentProvider.findValidCurrentConsent(
                eq(employeeId), any(), any(), any())).thenReturn(Optional.empty());
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.acceptBiometricTerms(employeeId, userId, "127.0.0.1", "JUnit",
                        "2026.05.21", "current-hash"));
    }

    // ── L180: userProvider.findByEmployeeId returns empty in revokeBiometricTerms ──
    @Test
    @DisplayName("revogar: lança ResourceNotFoundException quando user não existe após employee encontrado")
    void revokeBiometricTerms_userNotFound_throwsAfterEmployeeFound() {
        UUID employeeId = UUID.randomUUID();
        var employee = employeeWithNoFace(employeeId);

        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.revokeBiometricTerms(employeeId, "127.0.0.1", "JUnit"));
    }

    // ── BR L308 B=true: version matches but hash doesn't ─────────────────────
    @Test
    @DisplayName("validação: lança BadRequestException quando versão confere mas hash não confere")
    void acceptBiometricTerms_versionMatchesButHashDoesNot_throwsBadRequest() {
        UUID employeeId = UUID.randomUUID();

        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(currentBiometricTerm())); // version=2026.05.21, hash=current-hash

        assertThrows(BadRequestException.class,
                () -> service.acceptBiometricTerms(employeeId, UUID.randomUUID(), "10.0.0.1", "JUnit",
                        "2026.05.21", "wrong-hash")); // version matches, hash does NOT
    }

}