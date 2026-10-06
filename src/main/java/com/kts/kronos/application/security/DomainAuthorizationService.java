package com.kts.kronos.application.security;

import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.domain.model.Document;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static com.kts.kronos.constants.Messages.*;

@Component
@RequiredArgsConstructor
public class DomainAuthorizationService {

    private static final String FORBIDDEN_OTHER_EMPLOYEE_RESOURCE = "Você não pode acessar recursos de outro colaborador.";
    private static final String FORBIDDEN_OTHER_USER_RESOURCE = "Você não pode acessar recursos de outro usuário.";

    private final JwtAuthenticatedUser jwtAuthenticatedUser;
    private final EmployeeProvider employeeProvider;
    private final UserProvider userProvider;
    private final DocumentProvider documentProvider;

    public Employee authorizeEmployeeAccess(UUID requestedEmployeeId) {
        var authenticatedEmployee = getAuthenticatedEmployee();
        var targetEmployee = requestedEmployeeId == null
                ? authenticatedEmployee
                : employeeProvider.findById(requestedEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));

        var role = jwtAuthenticatedUser.getCurrentRole();
        if (isCto(role)) {
            return targetEmployee;
        }
        if (isManager(role)) {
            validateSameTenant(authenticatedEmployee, targetEmployee);
            return targetEmployee;
        }

        validateEmployeeOwnership(authenticatedEmployee, targetEmployee);
        return targetEmployee;
    }

    public User authorizeUserAccess(UUID targetUserId) {
        var targetUser = userProvider.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
        return authorizeResolvedUserAccess(targetUser);
    }

    public User authorizeUserAccessByUsername(String username) {
        var targetUser = userProvider.findByUsername(username.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
        return authorizeResolvedUserAccess(targetUser);
    }

    public Document authorizeDocumentAccess(UUID documentId, UUID requestedEmployeeId) {
        if (requestedEmployeeId != null) {
            var targetEmployee = authorizeEmployeeAccess(requestedEmployeeId);
            var document = documentProvider.findByIdAndEmployeeId(documentId, targetEmployee.employeeId())
                    .orElseThrow(() -> new ResourceNotFoundException(DOCUMENT_NOT_FOUND));
            validateDocumentVisibility(document);
            return document;
        }

        var document = documentProvider.findById(documentId);
        authorizeEmployeeAccess(document.employeeId());
        validateDocumentVisibility(document);
        return document;
    }

    private void validateDocumentVisibility(Document document) {
        var role = jwtAuthenticatedUser.getCurrentRole();

        if (isCto(role) || isManager(role)) {
            if (document.deletedByManager()) {
                throw new ResourceNotFoundException(DOCUMENT_NOT_FOUND);
            }
        } else {
            if (document.deletedByEmployee()) {
                throw new ResourceNotFoundException(DOCUMENT_NOT_FOUND);
            }
        }
    }

    public UUID authorizeCompanyAccess(UUID requestedCompanyId) {
        var role = jwtAuthenticatedUser.getCurrentRole();

        if (isCto(role)) {
            var targetCompanyId = requestedCompanyId == null
                    ? getAuthenticatedEmployee().companyId()
                    : requestedCompanyId;
            return targetCompanyId;
        }

        // Para MANAGER/PARTNER: usa activeCompanyId do JWT
        UUID activeCompanyId = jwtAuthenticatedUser.getActiveCompanyId();
        if (activeCompanyId == null) {
            // Fallback para tokens sem activeCompanyId
            activeCompanyId = getAuthenticatedEmployee().companyId();
        }

        var targetCompanyId = requestedCompanyId == null ? activeCompanyId : requestedCompanyId;

        if (!activeCompanyId.equals(targetCompanyId)) {
            throw new ForbiddenException(MANAGER_DIFFERENT_COMPANY);
        }
        return targetCompanyId;
    }

    public Employee requireEmployeeFromCompany(UUID targetEmployeeId, UUID companyId, String notFoundMessage, String forbiddenMessage) {
        var targetEmployee = employeeProvider.findById(targetEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException(notFoundMessage));

        if (!targetEmployee.companyId().equals(companyId)) {
            throw new ForbiddenException(forbiddenMessage);
        }
        return targetEmployee;
    }

    private Employee getAuthenticatedEmployee() {
        var authenticatedEmployeeId = jwtAuthenticatedUser.getEmployeeId();
        return employeeProvider.findById(authenticatedEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));
    }

    private User authorizeResolvedUserAccess(User targetUser) {
        var authenticatedUserId = jwtAuthenticatedUser.getuserId();

        // Self-access is always permitted regardless of role or active company
        if (targetUser.userId().equals(authenticatedUserId)) {
            return targetUser;
        }

        var role = jwtAuthenticatedUser.getCurrentRole();
        if (isCto(role)) {
            return targetUser;
        }

        if (isManager(role)) {
            var authenticatedEmployee = getAuthenticatedEmployee();
            var targetEmployee = employeeProvider.findById(targetUser.employeeId())
                    .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));
            validateSameTenant(authenticatedEmployee, targetEmployee);
            return targetUser;
        }

        throw new ForbiddenException(FORBIDDEN_OTHER_USER_RESOURCE);
    }

    private void validateSameTenant(Employee authenticatedEmployee, Employee targetEmployee) {
        UUID activeCompanyId = jwtAuthenticatedUser.getActiveCompanyId();
        UUID effectiveCompanyId = activeCompanyId != null ? activeCompanyId : authenticatedEmployee.companyId();

        if (!effectiveCompanyId.equals(targetEmployee.companyId())) {
            throw new ForbiddenException(MANAGER_DIFFERENT_COMPANY);
        }
    }

    private void validateEmployeeOwnership(Employee authenticatedEmployee, Employee targetEmployee) {
        if (!authenticatedEmployee.employeeId().equals(targetEmployee.employeeId())) {
            throw new ForbiddenException(FORBIDDEN_OTHER_EMPLOYEE_RESOURCE);
        }
    }

    private boolean isManager(Role role) {
        return Role.MANAGER == role;
    }

    private boolean isCto(Role role) {
        return Role.CTO == role;
    }
}
