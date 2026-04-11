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

        var role = jwtAuthenticatedUser.getRoleFromToken();
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

        var role = jwtAuthenticatedUser.getRoleFromToken();
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

        var authenticatedUserId = jwtAuthenticatedUser.getuserId();
        if (!targetUser.userId().equals(authenticatedUserId)) {
            throw new ForbiddenException(FORBIDDEN_OTHER_USER_RESOURCE);
        }
        return targetUser;
    }

    public Document authorizeDocumentAccess(UUID documentId, UUID requestedEmployeeId) {
        if (requestedEmployeeId != null) {
            var targetEmployee = authorizeEmployeeAccess(requestedEmployeeId);
            return documentProvider.findByIdAndEmployeeId(documentId, targetEmployee.employeeId())
                    .orElseThrow(() -> new ResourceNotFoundException(DOCUMENT_NOT_FOUND));
        }

        var document = documentProvider.findById(documentId);
        authorizeEmployeeAccess(document.employeeId());
        return document;
    }

    public UUID authorizeCompanyAccess(UUID requestedCompanyId) {
        var authenticatedEmployee = getAuthenticatedEmployee();
        var targetCompanyId = requestedCompanyId == null ? authenticatedEmployee.companyId() : requestedCompanyId;
        var role = jwtAuthenticatedUser.getRoleFromToken();

        if (isCto(role)) {
            return targetCompanyId;
        }

        if (!authenticatedEmployee.companyId().equals(targetCompanyId)) {
            throw new ForbiddenException(MANAGER_DIFFERENT_COMPANY);
        }
        return targetCompanyId;
    }

    private Employee getAuthenticatedEmployee() {
        var authenticatedEmployeeId = jwtAuthenticatedUser.getEmployeeId();
        return employeeProvider.findById(authenticatedEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));
    }

    private void validateSameTenant(Employee authenticatedEmployee, Employee targetEmployee) {
        if (!authenticatedEmployee.companyId().equals(targetEmployee.companyId())) {
            throw new ForbiddenException(MANAGER_DIFFERENT_COMPANY);
        }
    }

    private void validateEmployeeOwnership(Employee authenticatedEmployee, Employee targetEmployee) {
        if (!authenticatedEmployee.employeeId().equals(targetEmployee.employeeId())) {
            throw new ForbiddenException(FORBIDDEN_OTHER_EMPLOYEE_RESOURCE);
        }
    }

    private boolean isManager(String role) {
        return "MANAGER".equals(role);
    }

    private boolean isCto(String role) {
        return "CTO".equals(role);
    }
}
