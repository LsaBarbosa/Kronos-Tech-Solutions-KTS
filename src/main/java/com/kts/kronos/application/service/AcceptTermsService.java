package com.kts.kronos.application.service;

import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.domain.model.AuditLog;
import com.kts.kronos.domain.model.Document;
import com.kts.kronos.domain.model.enuns.DocumentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.kts.kronos.constants.Logs.*;
import static com.kts.kronos.constants.Messages.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AcceptTermsService implements AcceptTermsUseCase {

    private final EmployeeProvider employeeProvider;
    private final CompanyProvider companyProvider;
    private final BiometricTermPdfService pdfService;
    private final DocumentProvider documentProvider;
    private final S3StorageProvider s3StorageProvider;
    private final AuditLogProvider auditLogProvider;
    @Override
    @Transactional
    public void acceptBiometricTerms(UUID employeeId, String ipAddress, String userAgent) {

        log.debug(LOG_CHECK_EXISTING, employeeId);

        boolean exists = documentProvider.existsByEmployeeIdAndType(
                employeeId,
                DocumentType.BIOMETRIC_CONSENT_TERM
        );

        if (exists) {
            log.warn(LOG_ALREADY_ACCEPTED, employeeId);
            return;
        }

        log.info(LOG_INIT_ACCEPTANCE, employeeId);

        var employee = employeeProvider.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));

        var company = companyProvider.findById(employee.companyId())
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND));

        // 1. Gera o PDF assinado eletronicamente
        byte[] pdfBytes = pdfService.generateConsentTerm(employee, company, ipAddress, userAgent);

        var filename = String.format(FILE_NAME_TEMPLATE, employee.cpf());
        var timestamp = LocalDateTime.now().format(S3_TIMESTAMP_FORMATTER);

        var s3Key = String.format(S3_KEY_TEMPLATE,
                company.companyId(),
                employee.employeeId(),
                timestamp
        );

        var storagePath = s3StorageProvider.uploadFile(s3Key, pdfBytes);

        var document = new Document(
                employee.employeeId(),
                DocumentType.BIOMETRIC_CONSENT_TERM,
                filename,
                CONTENT_TYPE_PDF,
                storagePath,
                TIME_ZONE_BRAZIL,
                null,
                false,
                false
        );
        documentProvider.save(document);

        var auditDetails = String.format(AUDIT_DETAIL_TEMPLATE, storagePath);
        var audit = AuditLog.create(
                employeeId,
                AUDIT_ACTION_BIOMETRIC_TERM,
                ipAddress,
                userAgent,
                auditDetails
        );
        auditLogProvider.registerLog(audit);

        log.info(LOG_SUCCESS_ACCEPTANCE, employeeId);
    }

    @Override
    public boolean hasAcceptedBiometricTerm(UUID employeeId) {

            return documentProvider.existsByEmployeeIdAndType(
                    employeeId,
                    DocumentType.BIOMETRIC_CONSENT_TERM
            );
        }
}