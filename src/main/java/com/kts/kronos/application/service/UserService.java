package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.security.ChangePasswordRequest;
import com.kts.kronos.adapter.in.web.dto.user.CreateUserRequest;
import com.kts.kronos.adapter.in.web.dto.user.UpdateUserRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ConflictException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.port.in.usecase.EmployeeUseCase;
import com.kts.kronos.application.port.in.usecase.UserUseCase;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.application.security.AuthenticationRateLimitService;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.AuditAction;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.observability.application.KronosMetrics;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.kts.kronos.constants.Messages.*;

@Slf4j
@Component
@RequiredArgsConstructor
@Transactional
public class UserService implements UserUseCase {

    private final UserProvider userProvider;
    private final DocumentProvider documentProvider;
    private final TimeRecordProvider timeRecordProvider;
    private final EmployeeProvider employeeProvider;
    private final PasswordEncoder passwordEncoder;
    private final JwtAuthenticatedUser jwtAuthenticatedUser;
    private final EmployeeUseCase employeeUseCase;
    private final DomainAuthorizationService domainAuthorizationService;
    private final AcceptTermsUseCase acceptTermsUseCase;
    private final AuthenticationRateLimitService authenticationRateLimitService;
    private final KronosMetrics kronosMetrics;
    private final AuditService auditService;

    @Override
    public void createUser(CreateUserRequest req) {

        if (userProvider.existsByUsername(req.username().toLowerCase())) {
            throw new BadRequestException(USERNAME_ALREADY_EXIST);
        }

        findById(req.employeeId());

        if (userProvider.existsByEmployeeId(req.employeeId())) {
            throw new BadRequestException(USER_ALREADY_LINKED_TO_EMPLOYEE);
        }

        var randomSystemPassword = UUID.randomUUID().toString();
        var hashed = passwordEncoder.encode(randomSystemPassword);

        var user = new User(
                req.username(),
                hashed,
                Role.valueOf(req.role()),
                req.employeeId()
        );
        try {
            userProvider.save(user);
            kronosMetrics.userCreated();

            // Auditoria de criação de usuário
            try {
                String[] ipAndUA = extractIpAndUserAgent();
                auditService.registerSecurity(
                        AuditAction.USER_CREATED,
                        currentUserIdOrNull(),
                        user.employeeId(),
                        "MEDIUM",
                        "USER",
                        user.userId().toString(),
                        "role=" + req.role(),
                        ipAndUA[0],
                        ipAndUA[1]
                );
            } catch (Exception auditEx) {
                log.debug("Falha ao registrar auditoria de criação de usuário", auditEx);
            }
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException(USERNAME_ALREADY_EXIST);
        }
    }

    @Override
    public User getUserByUsername(String username) {
        return domainAuthorizationService.authorizeUserAccessByUsername(username);
    }

    @Override
    public User getUserById(UUID userId) {
        return getUserId(userId);
    }

    @Override
    public List<User> listUsers(Boolean active) {
        var currentRole = jwtAuthenticatedUser.getCurrentRole();

        if (currentRole == Role.CTO) {
            return active == null
                    ? userProvider.findAll()
                    : userProvider.findByActive(active);
        }

        var companyId = domainAuthorizationService.authorizeCompanyAccess(null);
        var employeeIdsFromCompany = employeeProvider.findByCompanyId(companyId).stream()
                .map(Employee::employeeId)
                .collect(Collectors.toSet());

        if (employeeIdsFromCompany.isEmpty()) {
            return List.of();
        }

        var usersFromTenant = active == null
                ? userProvider.findByEmployeeIds(employeeIdsFromCompany)
                : userProvider.findByEmployeeIdsAndActive(employeeIdsFromCompany, active);

        if (currentRole == Role.PARTNER) {
            return usersFromTenant.stream()
                    .filter(user -> user.role() == Role.MANAGER)
                    .toList();
        }

        return usersFromTenant;
    }

    @Override
    public void updateUser(UUID userId, UpdateUserRequest req) {
        var existing = getUserId(userId);

        var username = req.username() != null ? req.username().toLowerCase() : existing.username();
        var password = existing.password();
        boolean passwordChanged = false;

        if (req.password() != null && !req.password().isBlank()) {
            validatePasswordPolicy(req.password());
            password = passwordEncoder.encode(req.password());
            passwordChanged = true;
        }

        var role = Role.valueOf(req.role() != null ? req.role() : existing.role().name());
        boolean active = req.enabled() != null ? req.enabled() : existing.active();
        var updated = new User(userId, username, password, role, active, existing.employeeId());
        updated = new User(
                updated.userId(),
                updated.username(),
                updated.password(),
                updated.role(),
                updated.active(),
                updated.employeeId(),
                existing.sessionVersion(),
                existing.deletedAt(),
                existing.deletedBy(),
                existing.deactivationReason()
        );

        try {
            userProvider.save(updated);
            kronosMetrics.userUpdated();

            // Auditoria de atualização de usuário
            try {
                java.util.List<String> changedFieldsList = new java.util.ArrayList<>();
                if (!username.equals(existing.username())) changedFieldsList.add("username");
                if (passwordChanged) changedFieldsList.add("password");
                if (!role.equals(existing.role())) changedFieldsList.add("role");
                if (active != existing.active()) changedFieldsList.add("active");

                String[] ipAndUA = extractIpAndUserAgent();
                auditService.registerSecurity(
                        AuditAction.USER_UPDATED,
                        currentUserIdOrNull(),
                        existing.employeeId(),
                        "MEDIUM",
                        "USER",
                        userId.toString(),
                        "changedFields=" + String.join(",", changedFieldsList),
                        ipAndUA[0],
                        ipAndUA[1]
                );
            } catch (Exception auditEx) {
                log.debug("Falha ao registrar auditoria de atualização de usuário", auditEx);
            }
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException(USERNAME_ALREADY_EXIST);
        }
    }

    @Override
    public void deleteUser(UUID userId) {
        var existing = getUserId(userId);
        var employeeId = existing.employeeId();
        var deletedBy = currentUserIdOrNull();

        userProvider.save(existing.deactivate(deletedBy, "USER_DELETE"));
        employeeProvider.findById(employeeId)
                .map(employee -> employee.deactivate(deletedBy, "USER_DELETE"))
                .ifPresent(employeeProvider::save);

        // Auditoria de desativação de usuário
        try {
            String[] ipAndUA = extractIpAndUserAgent();
            auditService.registerSecurity(
                    AuditAction.USER_DEACTIVATED,
                    deletedBy,
                    employeeId,
                    "HIGH",
                    "USER",
                    userId.toString(),
                    "reason=USER_DELETE",
                    ipAndUA[0],
                    ipAndUA[1]
            );
        } catch (Exception auditEx) {
            log.debug("Falha ao registrar auditoria de desativação de usuário", auditEx);
        }
    }

    @Override
    public void toggleActivate(UUID userId) {
        var existing = getUserId(userId);
        boolean newActiveStatus = !existing.active();
        var active = existing.withActive(newActiveStatus);
        userProvider.save(active);
        employeeUseCase.toggleActivate(existing.employeeId());

        // Auditoria de alteração de ativação de usuário
        try {
            String[] ipAndUA = extractIpAndUserAgent();
            auditService.registerSecurity(
                    AuditAction.USER_ACTIVATION_TOGGLED,
                    currentUserIdOrNull(),
                    existing.employeeId(),
                    "MEDIUM",
                    "USER",
                    userId.toString(),
                    "active=" + newActiveStatus,
                    ipAndUA[0],
                    ipAndUA[1]
            );
        } catch (Exception auditEx) {
            log.debug("Falha ao registrar auditoria de alteração de ativação de usuário", auditEx);
        }
    }

    @Override
    public void changeOwnPassword(ChangePasswordRequest req) {
        var userId = jwtAuthenticatedUser.getuserId();
        var user = getUserId(userId);

        if (!passwordEncoder.matches(req.currentPassword(), user.password())) {
            throw new BadRequestException(INVALID_PASSWORD);
        }
        if (req.newPassword() == null || !req.newPassword().equals(req.confirmPassword())) {
            throw new BadRequestException(INVALID_CONFIRM_PASSWORD);
        }
        validatePasswordPolicy(req.newPassword());

        String hashed = passwordEncoder.encode(req.newPassword());
        userProvider.save(user.withPassword(hashed).incrementSessionVersion());

        // Auditoria de troca de senha
        try {
            String[] ipAndUA = extractIpAndUserAgent();
            auditService.registerSecurity(
                    AuditAction.AUTH_PASSWORD_CHANGED,
                    userId,
                    user.employeeId(),
                    "HIGH",
                    "USER",
                    userId.toString(),
                    "password_changed=true,sessions_revoked=true",
                    ipAndUA[0],
                    ipAndUA[1]
            );
        } catch (Exception auditEx) {
            log.debug("Falha ao registrar auditoria de troca de senha", auditEx);
        }
    }

    @Override
    public User getOwnProfile() {
        var userId = jwtAuthenticatedUser.getuserId();
        return getUserId(userId);
    }

    @Override
    public boolean usernameExists(String username) {
        authenticationRateLimitService.checkAdminSearchRateLimit();
        return userProvider.existsByUsername(username.toLowerCase());
    }

    private void validatePasswordPolicy(String raw) {
        if (raw == null || !raw.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")) {
            throw new BadRequestException(INVALID_PASSWORD_POLICY);
        }
    }

    private void findById(UUID userId) {
        employeeProvider.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));
    }

    private User getUserId(UUID userId) {
        return domainAuthorizationService.authorizeUserAccess(userId);
    }

    private UUID currentUserIdOrNull() {
        try {
            return jwtAuthenticatedUser.getuserId();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String[] extractIpAndUserAgent() {
        String ipAddress = "unknown";
        String userAgent = "unknown";
        try {
            var requestAttrs = RequestContextHolder.getRequestAttributes();
            if (requestAttrs instanceof ServletRequestAttributes servletAttrs) {
                var request = servletAttrs.getRequest();
                ipAddress = request.getHeader("X-Forwarded-For");
                if (ipAddress == null || ipAddress.isBlank()) {
                    ipAddress = request.getRemoteAddr();
                }
                userAgent = request.getHeader("User-Agent");
                if (userAgent == null) {
                    userAgent = "unknown";
                }
            }
        } catch (Exception e) {
            log.debug("Falha ao obter IP/User-Agent para auditoria", e);
        }
        return new String[]{ipAddress, userAgent};
    }
}
