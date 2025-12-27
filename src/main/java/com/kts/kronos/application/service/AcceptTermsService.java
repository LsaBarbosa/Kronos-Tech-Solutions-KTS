package com.kts.kronos.application.service;

import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.port.in.usecase.DocumentUseCase;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.domain.model.AuditLog;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.enuns.DocumentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final S3StorageProvider s3StorageProvider;
    private final AuditLogProvider auditLogProvider;
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

        var timestamp = LocalDateTime.now().format(DATE_TIME);
        var s3Key = String.format("legal/%s/%s/%s_termo_biometria.pdf",
                company.companyId(),
                employee.employeeId(),
                timestamp
        );

        var storagePath = s3StorageProvider.uploadFile(s3Key, pdfBytes);

        documentUseCase.uploadGeneratedDocument(
                DocumentType.BIOMETRIC_CONSENT_TERM,
                employee.employeeId(),
                null, // Não vinculado a um TimeRecord específico
                pdfBytes,
                filename
        );

        var audit = AuditLog.create(
                employeeId,
                "ACEITE_TERMOS_BIOMETRIA",
                ipAddress,
                userAgent,
                "Documento gerado e armazenado em: " + storagePath
        );

        auditLogProvider.registerLog(audit);
        // ------------------------------------------

        log.info("Fluxo de aceite e auditoria concluído com sucesso.");
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