package com.kts.kronos.application.service;

import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.domain.model.AnonymizationPlan;
import com.kts.kronos.domain.model.enuns.AuditAction;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.application.service.anonymization.AnonymizationPlanExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.kts.kronos.constants.Messages.UNAUTHORIZED_ROLE_OPERATION;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeAnonymizationService {
    private static final String ANONYMIZATION_REASON = "LGPD_ANONYMIZATION";

    private final JwtAuthenticatedUser jwtAuthenticatedUser;
    private final DomainAuthorizationService domainAuthorizationService;
    private final EmployeeProvider employeeProvider;
    private final UserProvider userProvider;
    private final AuditService auditService;
    private final AnonymizationPlanExecutor anonymizationPlanExecutor;

    public void anonymize(UUID employeeId, String ipAddress, String userAgent, UUID actorUserId) {
        requireAdministrativeRole();
        var employee = domainAuthorizationService.authorizeEmployeeAccess(employeeId);

        var plan = new AnonymizationPlan(
                employeeId,
                employee.companyId(),
                actorUserId,
                ANONYMIZATION_REASON,
                false,
                false,
                true,
                true,
                true,
                true
        );

        try {
            anonymizationPlanExecutor.executePlan(plan, "APPLY");

            auditService.registerLgpd(
                    AuditAction.LGPD_DATA_ANONYMIZED,
                    actorUserId,
                    employee.employeeId(),
                    employee.companyId(),
                    "EMPLOYEE",
                    employee.employeeId().toString(),
                    "HIGH",
                    String.format("Anonimização LGPD concluída via AnonymizationPlanExecutor. employeeId=%s", employee.employeeId()),
                    ipAddress,
                    userAgent
            );

            log.info("event=employee_anonymization_complete employeeId={} companyId={}", employeeId, employee.companyId());
        } catch (Exception e) {
            log.error("event=employee_anonymization_failed employeeId={} error={}", employeeId, e.getMessage(), e);
            throw new RuntimeException("Falha ao anonimizar colaborador: " + e.getMessage(), e);
        }
    }

    private void requireAdministrativeRole() {
        var currentRole = jwtAuthenticatedUser.getCurrentRole();
        if (currentRole != Role.MANAGER && currentRole != Role.CTO) {
            throw new ForbiddenException(UNAUTHORIZED_ROLE_OPERATION);
        }
    }
}
