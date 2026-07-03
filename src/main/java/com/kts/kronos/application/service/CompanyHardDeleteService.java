package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.company.CompanyHardDeleteResultDTO;
import com.kts.kronos.adapter.out.persistence.*;
import com.kts.kronos.adapter.out.persistence.entity.EmployeeEntity;
import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.application.port.out.provider.FaceStorageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyHardDeleteService {

    private final EmployeeRepository employeeRepository;
    private final LegalConsentRepository legalConsentRepository;
    private final LgpdRequestRepository lgpdRequestRepository;
    private final AnonymizationConsolidatedResultRepository anonymizationRepository;
    private final UserCompanyAccessRepository userCompanyAccessRepository;
    private final CompanyRepository companyRepository;
    private final FaceStorageProvider faceStorageProvider;
    private final FaceRecognitionProvider faceRecognitionProvider;

    /**
     * Two-phase hard delete:
     * Phase 1 — external systems (S3, Rekognition), outside any DB transaction.
     * Phase 2 — database cascade, inside @Transactional.
     * If phase 1 fails before touching the DB, state is fully recoverable.
     */
    public CompanyHardDeleteResultDTO hardDelete(UUID companyId, String cnpj, String companyName) {
        List<EmployeeEntity> employees = employeeRepository.findByCompanyId(companyId);

        // Phase 1: external systems — must complete before any DB delete
        List<String> failures = cleanExternalSystems(employees);

        // Phase 2: database — runs even if there were external partial failures
        int employeesDeleted = deleteFromDatabase(companyId, employees);

        log.info("event=company_hard_delete_completed companyRef={} employees={} externalFailures={}",
                companyId, employeesDeleted, failures.size());

        return new CompanyHardDeleteResultDTO(cnpj, companyName, employeesDeleted, failures.size(), failures);
    }

    /** Phase 1 — clean S3 and Rekognition. Accumulates failures, never aborts. */
    private List<String> cleanExternalSystems(List<EmployeeEntity> employees) {
        List<String> failures = new ArrayList<>();

        for (EmployeeEntity employee : employees) {
            UUID employeeId = employee.getEmployeeId();

            try {
                faceRecognitionProvider.deleteFacesByExternalImageId(employeeId);
            } catch (Exception e) {
                String msg = "Rekognition cleanup failed for employee " + employeeId + ": " + e.getClass().getSimpleName();
                failures.add(msg);
                log.warn("event=company_hard_delete_rekognition_failure employeeRef={} reason={}", employeeId, e.getClass().getSimpleName());
            }

            String faceKey = employee.getFaceS3ObjectKey();
            if (faceKey != null && !faceKey.isBlank()) {
                try {
                    faceStorageProvider.deleteFaceImage(faceKey);
                } catch (Exception e) {
                    String msg = "S3 cleanup failed for employee " + employeeId + ": " + e.getClass().getSimpleName();
                    failures.add(msg);
                    log.warn("event=company_hard_delete_s3_failure employeeRef={} reason={}", employeeId, e.getClass().getSimpleName());
                }
            }
        }

        return failures;
    }

    /** Phase 2 — delete from DB in FK-safe order inside a single transaction. */
    @Transactional
    public int deleteFromDatabase(UUID companyId, List<EmployeeEntity> employees) {
        for (EmployeeEntity employee : employees) {
            UUID employeeId = employee.getEmployeeId();
            // RESTRICT FK order: anonymization → lgpd_request → legal_consent
            anonymizationRepository.deleteByEmployeeId(employeeId);
            lgpdRequestRepository.deleteByEmployeeId(employeeId);
            legalConsentRepository.deleteByEmployeeId(employeeId);
        }

        // RESTRICT FK on company: user_company_access must go before employees and company
        userCompanyAccessRepository.deleteByCompanyId(companyId);

        // Employee delete cascades: users, time_records, documents, messages, approvals
        employeeRepository.deleteAll(employees);

        // Company delete cascades: company_nsr, messages by company
        companyRepository.deleteById(companyId);

        return employees.size();
    }
}
