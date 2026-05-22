package com.kts.kronos.application.service;

import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.port.in.usecase.DocumentUseCase;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.domain.model.LegalConsent;
import com.kts.kronos.domain.model.LegalText;
import com.kts.kronos.domain.model.enuns.AuditAction;
import com.kts.kronos.domain.model.enuns.ConsentType;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.LegalBasis;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AcceptTermsService implements AcceptTermsUseCase {

    private final EmployeeProvider employeeProvider;
    private final CompanyProvider companyProvider;
    private final BiometricTermPdfService pdfService;
    private final DocumentUseCase documentUseCase;
    private final DocumentProvider documentProvider;
    private final AuditService auditService;
    private final FaceStorageProvider faceStorageProvider;
    private final FaceRecognitionProvider faceRecognitionProvider;
    private final LegalConsentProvider legalConsentProvider;
    private final LegalTextProvider legalTextProvider;

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
    public void acceptBiometricTerms(
            UUID employeeId,
            UUID userId,
            String ipAddress,
            String userAgent,
            String version,
            String contentHashSha256
    ) {

        boolean exists = legalConsentProvider.existsActive(
                employeeId,
                ConsentType.BIOMETRIC_AUTHENTICATION
        );

        if (exists) {
            log.warn("Usuário {} tentou aceitar o termo novamente, mas já possui registro.", employeeId);
            return;
        }

        log.info("Iniciando processo de aceite de termos para Employee ID: {}", employeeId);

        var employee = employeeProvider.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));

        var currentBiometricTerm = getCurrentBiometricTerm();
        validateCurrentBiometricTerm(currentBiometricTerm, version, contentHashSha256);

        var company = companyProvider.findById(employee.companyId())
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND));

        // 1. Gera o PDF assinado eletronicamente
        byte[] pdfBytes = pdfService.generateConsentTerm(employee, company, ipAddress, userAgent, currentBiometricTerm);

        var filename = String.format("Termo_Aceite_Biometria_%s.pdf", employee.employeeId());

        // 3. Fonte única de verdade: persiste o documento apenas pelo fluxo canônico
        documentUseCase.uploadGeneratedDocument(
                DocumentType.BIOMETRIC_CONSENT_TERM,
                employee.employeeId(),
                null,
                pdfBytes,
                filename
        );

        // 4. Busca o metadado recém-persistido para usar o mesmo artefato na auditoria
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
                employeeId,
                employee.companyId(),
                "LEGAL_CONSENT",
                persistedDocument.documentId().toString(),
                "MEDIUM",
                ipAddress,
                userAgent,
                String.format(
                        "Consentimento biometrico registrado. documentId=%s, documentType=%s",
                        persistedDocument.documentId(),
                        persistedDocument.type()
                )
        );

        log.info("Fluxo de aceite e auditoria concluído com sucesso.");
    }

    @Override
    @Transactional
    public void revokeBiometricTerms(UUID employeeId, String ipAddress, String userAgent) {
        var employee = employeeProvider.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));

        if (employee.faceS3ObjectKey() != null && !employee.faceS3ObjectKey().isBlank()) {
            faceStorageProvider.deleteFaceImage(employee.faceS3ObjectKey());
        }

        legalConsentProvider.findActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION)
                .ifPresent(consent -> legalConsentProvider.save(consent.revoke(Instant.now())));

        faceRecognitionProvider.deleteFacesByExternalImageId(employeeId);
        employeeProvider.save(employee.withFaceS3ObjectKey(null));

        var consentDocuments = documentProvider.findByEmployeeAndType(
                employeeId,
                DocumentType.BIOMETRIC_CONSENT_TERM,
                true
        );

        auditService.register(
                AuditAction.BIOMETRIC_CONSENT_REVOKED,
                employeeId,
                employee.companyId(),
                "LEGAL_CONSENT",
                employeeId.toString(),
                "MEDIUM",
                ipAddress,
                userAgent,
                String.format(
                        "Consentimento biométrico revogado e artefatos biométricos purgados. evidenceDocumentsPreserved=%s",
                        !consentDocuments.isEmpty()
                )
        );
        log.info("Revogação biométrica concluída com sucesso para o colaborador {}", employeeId);
    }

    @Override
    public boolean hasAcceptedBiometricTerm(UUID employeeId) {
        return legalConsentProvider.existsActive(
                employeeId,
                ConsentType.BIOMETRIC_AUTHENTICATION
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
        if (!currentBiometricTerm.version().equals(version)
                || !currentBiometricTerm.contentHashSha256().equals(contentHashSha256)) {
            throw new com.kts.kronos.application.exceptions.BadRequestException(INVALID_BIOMETRIC_TERM_VERSION_OR_HASH);
        }
    }
}
