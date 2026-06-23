package com.kts.kronos.application.service.demo;

import com.kts.kronos.adapter.in.web.dto.demo.DemoValidationIssue;
import com.kts.kronos.adapter.in.web.dto.demo.DemoValidationResult;
import com.kts.kronos.adapter.out.persistence.*;
import com.kts.kronos.config.demo.DemoSandboxProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoSandboxValidationService {

    private final CompanyRepository   companyRepo;
    private final UserRepository      userRepo;
    private final EmployeeRepository  employeeRepo;
    private final DocumentRepository  documentRepo;
    private final TimeRecordRepository timeRecordRepo;
    private final UserCompanyAccessRepository accessRepo;
    private final LegalConsentRepository      consentRepo;
    private final DemoSandboxProperties       props;

    /**
     * Checks whether there is a sandbox environment ready to use (exists, not corrupted).
     */
    @Transactional(readOnly = true)
    public boolean sandboxExists() {
        return companyRepo.findBySandboxKey(props.getSandboxKey()).isPresent();
    }

    /**
     * Validates that the sandbox was fully purged — no residue remains.
     * Called after a purge; issues are logged and returned.
     */
    @Transactional(readOnly = true)
    public DemoValidationResult validateAfterPurge() {
        List<DemoValidationIssue> issues = new ArrayList<>();

        companyRepo.findBySandboxKey(props.getSandboxKey()).ifPresent(c ->
                issues.add(new DemoValidationIssue("COMPANY_RESIDUE",
                        "Company with sandbox_key still exists: " + c.getId())));

        userRepo.findByUsernameIgnoreCase(props.getUsername()).ifPresent(u ->
                issues.add(new DemoValidationIssue("USER_RESIDUE",
                        "User '" + props.getUsername() + "' still exists")));

        Path sandboxDir = Path.of(props.getLocalStorageRoot());
        if (Files.exists(sandboxDir)) {
            issues.add(new DemoValidationIssue("FILES_RESIDUE",
                    "Sandbox directory still exists: " + sandboxDir));
        }

        if (!issues.isEmpty()) {
            log.warn("[DemoSandbox] Post-purge validation found {} issues", issues.size());
        }
        return new DemoValidationResult(issues.isEmpty(), issues);
    }

    /**
     * Validates sandbox health when it exists (for status endpoint).
     */
    @Transactional(readOnly = true)
    public DemoValidationResult validateSandboxHealth() {
        List<DemoValidationIssue> issues = new ArrayList<>();

        var companyOpt = companyRepo.findBySandboxKey(props.getSandboxKey());
        if (companyOpt.isEmpty()) {
            return new DemoValidationResult(true, List.of());
        }

        var company = companyOpt.get();
        var employees = employeeRepo.findByCompanyId(company.getId());
        if (employees.isEmpty()) {
            issues.add(new DemoValidationIssue("NO_EMPLOYEES", "Sandbox company has no employees"));
        }

        if (userRepo.findByUsernameIgnoreCase(props.getUsername()).isEmpty()) {
            issues.add(new DemoValidationIssue("NO_USER", "Sandbox user '" + props.getUsername() + "' not found"));
        }

        return new DemoValidationResult(issues.isEmpty(), issues);
    }
}
