package com.kts.kronos.application.service;

import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.DocumentUseCase;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.application.security.PrivacyLogReferenceService;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.LegalConsent;
import com.kts.kronos.domain.model.LegalText;
import com.kts.kronos.domain.model.enuns.ConsentType;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.LegalBasis;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.observability.application.KronosMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AcceptTermsServiceCoverageTest {

    @InjectMocks private AcceptTermsService service;

    @Mock private EmployeeProvider employeeProvider;
    @Mock private CompanyProvider companyProvider;
    @Mock private UserProvider userProvider;
    @Mock private BiometricTermPdfService pdfService;
    @Mock private DocumentUseCase documentUseCase;
    @Mock private DocumentProvider documentProvider;
    @Mock private AuditService auditService;
    @Mock private FaceStorageProvider faceStorageProvider;
    @Mock private FaceRecognitionProvider faceRecognitionProvider;
    @Mock private LegalConsentProvider legalConsentProvider;
    @Mock private LegalTextProvider legalTextProvider;
    @Mock private KronosMetrics kronosMetrics;
    @Mock private PrivacyLogReferenceService privacyLogReferenceService;
    @Mock private com.kts.kronos.application.port.out.provider.CacheProvider cacheProvider;

    // ── revokeBiometricTerms: faceS3ObjectKey non-null but blank → !isBlank()=FALSE ─

    @Test
    void revokeBiometricTerms_withBlankFaceKey_skipsS3Deletion() {
        UUID empId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        // Non-null but blank key → covers `!isBlank()=FALSE` branch
        var employee = buildEmployee(empId, companyId).withFaceS3ObjectKey("   ");
        var user = buildUser(empId);

        when(employeeProvider.findById(empId)).thenReturn(Optional.of(employee));
        when(userProvider.findByEmployeeId(empId)).thenReturn(Optional.of(user));
        when(legalConsentProvider.findActive(empId, ConsentType.BIOMETRIC_AUTHENTICATION))
                .thenReturn(Optional.empty());
        when(employeeProvider.save(any())).thenReturn(employee.withFaceS3ObjectKey(null));
        when(documentProvider.findByEmployeeAndType(empId, DocumentType.BIOMETRIC_CONSENT_TERM, true))
                .thenReturn(List.of());
        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(currentBiometricTerm()));

        assertDoesNotThrow(() -> service.revokeBiometricTerms(empId, "10.0.0.1", "JUnit"));
        // faceStorageProvider.deleteFaceImage should NOT be called (key is blank)
        org.mockito.Mockito.verify(faceStorageProvider, org.mockito.Mockito.never())
                .deleteFaceImage(any());
    }

    // ── revokeBiometricTerms: findActive returns consent → ifPresent lambda covered ─

    @Test
    void revokeBiometricTerms_withActiveConsent_revokesConsent() {
        UUID empId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        var employee = buildEmployee(empId, companyId).withFaceS3ObjectKey(null);
        var user = buildUser(empId);
        var consent = buildConsent(empId); // non-empty → ifPresent lambda body covered

        when(employeeProvider.findById(empId)).thenReturn(Optional.of(employee));
        when(userProvider.findByEmployeeId(empId)).thenReturn(Optional.of(user));
        when(legalConsentProvider.findActive(empId, ConsentType.BIOMETRIC_AUTHENTICATION))
                .thenReturn(Optional.of(consent)); // → lambda body covered
        when(legalConsentProvider.save(any())).thenReturn(null);
        when(employeeProvider.save(any())).thenReturn(employee.withFaceS3ObjectKey(null));
        when(documentProvider.findByEmployeeAndType(empId, DocumentType.BIOMETRIC_CONSENT_TERM, true))
                .thenReturn(List.of());
        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(currentBiometricTerm()));

        assertDoesNotThrow(() -> service.revokeBiometricTerms(empId, "10.0.0.1", "JUnit"));
        // ifPresent lambda body: legalConsentProvider.save(consent.revoke(...)) was called
        org.mockito.Mockito.verify(legalConsentProvider, org.mockito.Mockito.atLeastOnce()).save(any());
    }

    // ── getBiometricConsentStatus: currentTerm hash=null → hasCurrentHash FALSE ──

    @Test
    void getBiometricConsentStatus_withNullCurrentTermHash_notAccepted() {
        UUID empId = UUID.randomUUID();
        LegalText termWithNullHash = buildTerm(null); // contentHashSha256=null → hasCurrentHash=FALSE
        LegalConsent consent = buildConsent(empId);

        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(termWithNullHash));
        when(legalConsentProvider.findActive(empId, ConsentType.BIOMETRIC_AUTHENTICATION))
                .thenReturn(Optional.of(consent));

        var status = service.getBiometricConsentStatus(empId);
        assertFalse(status.accepted()); // hasCurrentHash=FALSE → accepted=FALSE
    }

    // ── getBiometricConsentStatus: currentTerm hash=blank → !isBlank()=FALSE ─────

    @Test
    void getBiometricConsentStatus_withBlankCurrentTermHash_notAccepted() {
        UUID empId = UUID.randomUUID();
        LegalText termWithBlankHash = buildTerm("   "); // blank → !isBlank()=FALSE → hasCurrentHash=FALSE
        LegalConsent consent = buildConsent(empId);

        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(termWithBlankHash));
        when(legalConsentProvider.findActive(empId, ConsentType.BIOMETRIC_AUTHENTICATION))
                .thenReturn(Optional.of(consent));

        var status = service.getBiometricConsentStatus(empId);
        assertFalse(status.accepted()); // hasCurrentHash=FALSE → accepted=FALSE
    }

    // ── validateCurrentBiometricTerm: blank hash → isBlank()=TRUE → throw ────────

    @Test
    void acceptBiometricTerms_withBlankCurrentTermHash_throwsIllegalState() {
        UUID empId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LegalText termWithBlankHash = buildTerm("   "); // blank → isBlank()=TRUE → IllegalStateException

        when(legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(termWithBlankHash));

        assertThrows(IllegalStateException.class, () ->
                service.acceptBiometricTerms(empId, userId, "10.0.0.1", "JUnit", "v1", "hash123"));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Employee buildEmployee(UUID empId, UUID companyId) {
        return new Employee(
                empId, "Test", "12345678901", "12345678901", "Dev",
                "test@kts.com", 1000.0, "11999999999", true, null,
                companyId, null, false, null, null, null, null, null, null, null, null, null, null
        );
    }

    private com.kts.kronos.domain.model.User buildUser(UUID empId) {
        return new com.kts.kronos.domain.model.User(
                UUID.randomUUID(), "user@kts.com", "hash",
                Role.PARTNER, true, empId
        );
    }

    private LegalConsent buildConsent(UUID empId) {
        return new LegalConsent(
                UUID.randomUUID(), empId, UUID.randomUUID(),
                ConsentType.BIOMETRIC_AUTHENTICATION, LegalBasis.CONSENT,
                "Purpose", "2026.05.21", "current-hash",
                Instant.parse("2026-05-21T09:00:00Z"), null,
                "10.0.0.1", "JUnit", UUID.randomUUID(), "pdf-hash",
                Instant.parse("2026-05-21T09:00:00Z"), null
        );
    }

    private LegalText currentBiometricTerm() {
        return buildTerm("current-hash");
    }

    private LegalText buildTerm(String hash) {
        return new LegalText(
                UUID.randomUUID(), DocumentType.BIOMETRIC_CONSENT_TERM,
                "2026.05.21", "Termo de Consentimento Biométrico",
                "Parágrafo inicial.", hash,
                true, Instant.parse("2026-05-21T09:00:00Z"), Instant.parse("2026-05-21T09:05:00Z")
        );
    }
}
