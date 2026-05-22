package com.kts.kronos.application.service;

import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.port.in.usecase.DocumentUseCase;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.domain.model.AuditLog;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.observability.application.KronosMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final AuditLogProvider auditLogProvider;
    private final FaceStorageProvider faceStorageProvider;
    private final FaceRecognitionProvider faceRecognitionProvider;
    private final KronosMetrics kronosMetrics;

    @Override
    @Transactional
    public void acceptBiometricTerms(UUID employeeId, String ipAddress, String userAgent) {

        boolean exists = documentProvider.existsByEmployeeIdAndType(
                employeeId,
                DocumentType.BIOMETRIC_CONSENT_TERM
        );

        if (exists) {
            log.warn("Usuário {} tentou aceitar o termo novamente, mas já possui registro.", employeeId);
            return;
        }

        log.info("Iniciando processo de aceite de termos para Employee ID: {}", employeeId);

        var employee = employeeProvider.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));

        var company = companyProvider.findById(employee.companyId())
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND));

        // 1. Gera o PDF assinado eletronicamente
        byte[] pdfBytes = pdfService.generateConsentTerm(employee, company, ipAddress, userAgent);

// 2. Define o nome do arquivo
        var filename = String.format("Termo_Aceite_Biometria_%s.pdf", employee.cpf());

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

        var audit = AuditLog.create(
                employeeId,
                "ACEITE_TERMOS_BIOMETRIA",
                ipAddress,
                userAgent,
                "Documento gerado e armazenado em: " + persistedDocument.storagePath()
        );

        auditLogProvider.registerLog(audit);

        log.info("Fluxo de aceite e auditoria concluído com sucesso.");
        kronosMetrics.consentAccepted();
    }

    @Override
    @Transactional
    public void revokeBiometricTerms(UUID employeeId, String ipAddress, String userAgent) {
        var employee = employeeProvider.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));

        if (employee.faceS3ObjectKey() != null && !employee.faceS3ObjectKey().isBlank()) {
            faceStorageProvider.deleteFaceImage(employee.faceS3ObjectKey());
        }

        faceRecognitionProvider.deleteFacesByExternalImageId(employeeId);
        employeeProvider.save(employee.withFaceS3ObjectKey(null));

        var consentDocuments = documentProvider.findByEmployeeAndType(
                employeeId,
                DocumentType.BIOMETRIC_CONSENT_TERM,
                true
        );

        for (var document : consentDocuments) {
            documentProvider.delete(employeeId, document.documentId());
        }

        var audit = AuditLog.create(
                employeeId,
                "REVOGACAO_TERMOS_BIOMETRIA",
                ipAddress,
                userAgent,
                "Consentimento biométrico revogado e artefatos biométricos purgados."
        );

        auditLogProvider.registerLog(audit);
        log.info("Revogação biométrica concluída com sucesso para o colaborador {}", employeeId);
        kronosMetrics.consentRevoked();
    }

    @Override
    public boolean hasAcceptedBiometricTerm(UUID employeeId) {
        // Regra de Negócio: O usuário aceitou se existir um documento do tipo BIOMETRIC_CONSENT_TERM vinculado a ele.
        return documentProvider.existsByEmployeeIdAndType(
                employeeId,
                DocumentType.BIOMETRIC_CONSENT_TERM
        );
    }
}