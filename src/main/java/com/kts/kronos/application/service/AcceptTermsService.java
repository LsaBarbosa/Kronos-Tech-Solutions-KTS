package com.kts.kronos.application.service;

import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.port.in.usecase.DocumentUseCase;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.application.security.PrivacyLogReferenceService;
import com.kts.kronos.domain.model.BiometricConsentStatus;
import com.kts.kronos.domain.model.BiometricConsentAcceptanceResult;
import com.kts.kronos.domain.model.BiometricConsentRevocationResult;
import com.kts.kronos.domain.model.LegalConsent;
import com.kts.kronos.domain.model.LegalText;
import com.kts.kronos.domain.model.enuns.AuditAction;
import com.kts.kronos.domain.model.enuns.ConsentType;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.LegalBasis;
import com.kts.kronos.observability.application.KronosMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AcceptTermsService implements AcceptTermsUseCase {

    private final EmployeeProvider employeeProvider;
    private final CompanyProvider companyProvider;
    private final UserProvider userProvider;
    private final BiometricTermPdfService pdfService;
    private final DocumentUseCase documentUseCase;
    private final DocumentProvider documentProvider;
    private final AuditService auditService;
    private final FaceStorageProvider faceStorageProvider;
    private final FaceRecognitionProvider faceRecognitionProvider;
    private final LegalConsentProvider legalConsentProvider;
    private final LegalTextProvider legalTextProvider;
    private final KronosMetrics kronosMetrics;
    private final PrivacyLogReferenceService privacyLogReferenceService;

    private static final HexFormat HEX = HexFormat.of();
    private static final String BIOMETRIC_CONSENT_PURPOSE =
            "Biometric authentication and identity validation in authorized Kronos flows.";

    @Override
    public LegalText getCurrentBiometricTerm() {
        return legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM)
                .orElseThrow(() -> new ResourceNotFoundException(CURRENT_BIOMETRIC_TERM_NOT_FOUND));
    }

    @Override
    @Transactional
    public BiometricConsentAcceptanceResult acceptBiometricTerms(
            UUID employeeId,
            UUID userId,
            String ipAddress,
            String userAgent,
            String version,
            String contentHashSha256
    ) {
        var currentBiometricTerm = getCurrentBiometricTerm();
        validateCurrentBiometricTerm(currentBiometricTerm, version, contentHashSha256);

        var user = userProvider.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));

        var existsValidConsent = legalConsentProvider.findValidCurrentConsent(
                employeeId,
                ConsentType.BIOMETRIC_AUTHENTICATION,
                version,
                contentHashSha256
        ).isPresent();

        if (existsValidConsent) {
            log.info("event=biometric_consent_acceptance_idempotent employeeRef={}",
                    privacyLogReferenceService.employeeRef(employeeId));
            var consentStatus = getBiometricConsentStatus(employeeId);
            return new BiometricConsentAcceptanceResult(employeeId, userId, user.sessionVersion(), consentStatus);
        }

        log.info("event=biometric_consent_acceptance_started employeeRef={}",
                privacyLogReferenceService.employeeRef(employeeId));

        var employee = employeeProvider.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));

        var company = companyProvider.findById(employee.companyId())
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND));

        byte[] pdfBytes = pdfService.generateConsentTerm(employee, company, ipAddress, userAgent, currentBiometricTerm);

        var filename = String.format("Termo_Aceite_Biometria_%s.pdf", employee.employeeId());

        documentUseCase.uploadGeneratedDocument(
                DocumentType.BIOMETRIC_CONSENT_TERM,
                employee.employeeId(),
                null,
                pdfBytes,
                filename
        );

        var persistedDocument = documentProvider.findByEmployeeAndType(
                        employee.employeeId(),
                        DocumentType.BIOMETRIC_CONSENT_TERM,
                        true
                ).stream()
                .filter(doc -> filename.equals(doc.fileName()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(DOCUMENT_NOT_FOUND));

        var grantedAt = Instant.now();
        var legalConsent = new LegalConsent(
                UUID.randomUUID(),
                employeeId,
                userId,
                ConsentType.BIOMETRIC_AUTHENTICATION,
                LegalBasis.CONSENT,
                BIOMETRIC_CONSENT_PURPOSE,
                currentBiometricTerm.version(),
                currentBiometricTerm.contentHashSha256(),
                grantedAt,
                null,
                ipAddress,
                userAgent,
                persistedDocument.documentId(),
                calculateSha256(pdfBytes),
                grantedAt,
                null
        );
        legalConsentProvider.save(legalConsent);

        auditService.register(
                AuditAction.BIOMETRIC_CONSENT_ACCEPTED,
                userId,
                employeeId,
                employee.companyId(),
                "LEGAL_CONSENT",
                persistedDocument.documentId().toString(),
                "MEDIUM",
                ipAddress,
                userAgent,
                String.format(
                        "Consentimento biometrico registrado. documentId=%s, documentType=%s, version=%s, contentHash=%s",
                        persistedDocument.documentId(),
                        persistedDocument.type(),
                        currentBiometricTerm.version(),
                        currentBiometricTerm.contentHashSha256()
                )
        );

        log.info("Fluxo de aceite e auditoria concluído com sucesso.");
        kronosMetrics.consentAccepted();

        var consentStatus = getBiometricConsentStatus(employeeId);
        return new BiometricConsentAcceptanceResult(employeeId, userId, user.sessionVersion(), consentStatus);
    }

    @Override
    @Transactional
    public BiometricConsentRevocationResult revokeBiometricTerms(UUID employeeId, String ipAddress, String userAgent) {
        var employee = employeeProvider.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));

        var user = userProvider.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));

        if (employee.faceS3ObjectKey() != null && !employee.faceS3ObjectKey().isBlank()) {
            faceStorageProvider.deleteFaceImage(employee.faceS3ObjectKey());
        }

        legalConsentProvider.findActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION)
                .ifPresent(consent -> legalConsentProvider.save(consent.revoke(Instant.now())));

        faceRecognitionProvider.deleteFacesByExternalImageId(employeeId);
        employeeProvider.save(employee.withFaceS3ObjectKey(null));

        var updatedUser = user.incrementSessionVersion();
        userProvider.save(updatedUser);

        var consentDocuments = documentProvider.findByEmployeeAndType(
                employeeId,
                DocumentType.BIOMETRIC_CONSENT_TERM,
                true
        );

        auditService.register(
                AuditAction.BIOMETRIC_CONSENT_REVOKED,
                user.userId(),
                employeeId,
                employee.companyId(),
                "LEGAL_CONSENT",
                employeeId.toString(),
                "HIGH",
                ipAddress,
                userAgent,
                String.format(
                        "Consentimento biométrico revogado, artefatos biométricos purgados e sessões invalidadas. evidenceDocumentsPreserved=%s, newSessionVersion=%s",
                        !consentDocuments.isEmpty(),
                        updatedUser.sessionVersion()
                )
        );

        log.info(
                "event=biometric_consent_revocation_completed employeeRef={} userRef={} newSessionVersion={}",
                privacyLogReferenceService.employeeRef(employeeId),
                privacyLogReferenceService.userRef(user.userId()),
                updatedUser.sessionVersion()
        );

        kronosMetrics.consentRevoked();

        var consentStatus = getBiometricConsentStatus(employeeId);

        return new BiometricConsentRevocationResult(
                employeeId,
                user.userId(),
                updatedUser.sessionVersion(),
                consentStatus
        );
    }

    @Override
    public boolean hasAcceptedBiometricTerm(UUID employeeId) {
        return legalConsentProvider.existsActive(
                employeeId,
                ConsentType.BIOMETRIC_AUTHENTICATION
        );
    }

    @Override
    public java.util.List<LegalConsent> getConsentHistory(UUID employeeId) {
        log.info("event=biometric_consent_history_requested employeeRef={}",
                privacyLogReferenceService.employeeRef(employeeId));
        employeeProvider.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));
        return legalConsentProvider.findAllByEmployeeId(employeeId);
    }

    @Override
    public BiometricConsentStatus getBiometricConsentStatus(UUID employeeId) {
        var currentTerm = getCurrentBiometricTerm();
        var activeConsent = legalConsentProvider.findActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION);

        if (activeConsent.isEmpty()) {
            return new BiometricConsentStatus(
                    false,
                    null,
                    null,
                    currentTerm.version(),
                    currentTerm.contentHashSha256(),
                    true
            );
        }

        var consent = activeConsent.get();
        boolean hasConsentHash = consent.contentHashSha256() != null
                && !consent.contentHashSha256().isBlank();
        boolean hasCurrentHash = currentTerm.contentHashSha256() != null
                && !currentTerm.contentHashSha256().isBlank();

        boolean accepted = hasConsentHash
                && hasCurrentHash
                && Objects.equals(consent.version(), currentTerm.version())
                && Objects.equals(consent.contentHashSha256(), currentTerm.contentHashSha256());

        return new BiometricConsentStatus(
                accepted,
                consent.version(),
                consent.contentHashSha256(),
                currentTerm.version(),
                currentTerm.contentHashSha256(),
                !accepted
        );
    }

    private String calculateSha256(byte[] payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HEX.formatHex(digest.digest(payload));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(ERROR_TO_GENERATE_HASH, e);
        }
    }

    private void validateCurrentBiometricTerm(LegalText currentBiometricTerm, String version, String contentHashSha256) {
        if (currentBiometricTerm.contentHashSha256() == null
                || currentBiometricTerm.contentHashSha256().isBlank()) {
            throw new IllegalStateException("Current biometric term content hash is not configured");
        }

        if (!Objects.equals(currentBiometricTerm.version(), version)
                || !Objects.equals(currentBiometricTerm.contentHashSha256(), contentHashSha256)) {
            throw new com.kts.kronos.application.exceptions.BadRequestException(INVALID_BIOMETRIC_TERM_VERSION_OR_HASH);
        }
    }
}
