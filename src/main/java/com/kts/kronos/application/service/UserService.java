package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.security.ChangePasswordRequest;
import com.kts.kronos.adapter.in.web.dto.user.AddCompanyAccessRequest;
import com.kts.kronos.adapter.in.web.dto.user.CreateUserRequest;
import com.kts.kronos.adapter.in.web.dto.user.UserListResponse;
import com.kts.kronos.adapter.in.web.dto.user.UserResponse;
import com.kts.kronos.adapter.in.web.dto.user.UserSearchItemResponse;
import com.kts.kronos.adapter.in.web.dto.user.UpdateUserRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.cache.ApplicationCacheNames;
import com.kts.kronos.application.cache.CacheScopes;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ConflictException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.port.in.usecase.EmployeeUseCase;
import com.kts.kronos.application.port.in.usecase.UserUseCase;
import com.kts.kronos.application.port.out.provider.CacheProvider;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.application.port.out.provider.UserCompanyAccessProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.application.security.AuthenticationRateLimitService;
import com.kts.kronos.application.security.ClientIpResolver;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.UserCompanyAccess;
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
    private final CacheProvider cacheProvider;
    private final ClientIpResolver clientIpResolver;
    private final UserCompanyAccessProvider userCompanyAccessProvider;
    private final CompanyProvider companyProvider;

    @Override
    public void createUser(CreateUserRequest req) {

        if (userProvider.existsByUsername(req.username().toLowerCase())) {
            throw new BadRequestException(USERNAME_ALREADY_EXIST);
        }

        findById(req.employeeId());

        if (userProvider.existsByEmployeeId(req.employeeId())) {
            throw new BadRequestException(USER_ALREADY_LINKED_TO_EMPLOYEE);
        }

        var requestedRole = Role.valueOf(req.role());

        // Prevent privilege escalation: only CTO can create a CTO user
        var callerRole = jwtAuthenticatedUser.getCurrentRole();
        if (requestedRole == Role.CTO && callerRole != Role.CTO) {
            throw new com.kts.kronos.application.exceptions.ForbiddenException(
                    "Apenas o CTO pode criar um usuário com o papel CTO.");
        }

        var randomSystemPassword = UUID.randomUUID().toString();
        var hashed = passwordEncoder.encode(randomSystemPassword);

        var user = new User(
                req.username(),
                hashed,
                requestedRole,
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
            invalidateUserCaches();
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
            var directManagers = usersFromTenant.stream()
                    .filter(user -> user.role() == Role.MANAGER)
                    .collect(Collectors.toList());

            // Gestores de múltiplas empresas: tb_user.employee_id aponta para a empresa
            // original, então não aparecem via findByEmployeeIds. Buscamos via UCA.
            var directManagerIds = directManagers.stream()
                    .map(User::userId)
                    .collect(Collectors.toSet());

            var ucaManagerIds = userCompanyAccessProvider.findActiveByCompanyId(companyId).stream()
                    .filter(uca -> Role.MANAGER.name().equals(uca.role()))
                    .map(UserCompanyAccess::userId)
                    .filter(uid -> !directManagerIds.contains(uid))
                    .collect(Collectors.toSet());

            if (!ucaManagerIds.isEmpty()) {
                var extraManagers = userProvider.findAllByIds(ucaManagerIds).stream()
                        .filter(u -> active == null || u.active() == active)
                        .toList();
                directManagers.addAll(extraManagers);
            }

            return directManagers;
        }

        return usersFromTenant;
    }

    @Override
    public UserListResponse listUsersResponse(Boolean active) {
        return cache(
                ApplicationCacheNames.USER_LIST,
                listUsersScope(active),
                UserListResponse.class,
                () -> buildUserListResponse(active)
        );
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

        // Prevent privilege escalation: only CTO can assign the CTO role
        var callerRole = jwtAuthenticatedUser.getCurrentRole();
        if (role == Role.CTO && callerRole != Role.CTO) {
            throw new com.kts.kronos.application.exceptions.ForbiddenException(
                    "Apenas o CTO pode atribuir o papel CTO a outro usuário.");
        }

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
            invalidateUserCaches();
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
        invalidateUserCaches();
    }

    @Override
    public void toggleActivate(UUID userId) {
        var existing = getUserId(userId);
        boolean newActiveStatus = !existing.active();
        var updated = newActiveStatus
                ? existing.withActive(true)
                : existing.withActive(false).incrementSessionVersion();
        userProvider.save(updated);
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
        invalidateUserCaches();
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
        invalidateUserCaches();
    }

    @Override
    public User getOwnProfile() {
        var userId = jwtAuthenticatedUser.getuserId();
        return getUserId(userId);
    }

    @Override
    public UserResponse getOwnProfileResponse() {
        var userId = jwtAuthenticatedUser.getuserId();
        return cache(
                ApplicationCacheNames.USER_OWN_PROFILE,
                CacheScopes.authenticatedScope("userId=" + userId),
                UserResponse.class,
                () -> UserResponse.fromDomain(getOwnProfile())
        );
    }

    @Override
    public void addCompanyAccess(UUID userId, AddCompanyAccessRequest req) {
        var user = userProvider.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));

        var company = companyProvider.findById(req.companyId())
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND + req.companyId()));

        if (!company.active()) {
            throw new com.kts.kronos.application.exceptions.BadRequestException("Empresa inativa.");
        }

        var employee = employeeProvider.findById(req.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));

        if (!employee.companyId().equals(req.companyId())) {
            throw new com.kts.kronos.application.exceptions.BadRequestException(
                    "Colaborador não pertence à empresa informada.");
        }

        if (userCompanyAccessProvider.existsActiveByUserIdAndCompanyId(userId, req.companyId())) {
            throw new ConflictException("Usuário já possui acesso ativo a esta empresa.");
        }

        var access = new UserCompanyAccess(
                java.util.UUID.randomUUID(),
                userId,
                req.companyId(),
                req.employeeId(),
                req.role(),
                true,
                req.defaultCompany(),
                java.time.LocalDateTime.now(),
                null
        );
        userCompanyAccessProvider.save(access);

        try {
            String[] ipAndUA = extractIpAndUserAgent();
            auditService.registerSecurity(
                    AuditAction.USER_COMPANY_ACCESS_ADDED,
                    currentUserIdOrNull(),
                    user.employeeId(),
                    "MEDIUM",
                    "USER",
                    userId.toString(),
                    "companyId=" + req.companyId() + ",role=" + req.role(),
                    ipAndUA[0],
                    ipAndUA[1]
            );
        } catch (Exception auditEx) {
            log.debug("Falha ao registrar auditoria de vínculo empresa-usuário", auditEx);
        }
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
                ipAddress = clientIpResolver.resolve(request);
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

    private void invalidateUserCaches() {
        try {
            cacheProvider.evictNamespace(ApplicationCacheNames.USER_LIST);
            cacheProvider.evictNamespace(ApplicationCacheNames.USER_OWN_PROFILE);
            cacheProvider.evictNamespace(ApplicationCacheNames.EMPLOYEE_OWN_PROFILE);
            cacheProvider.evictNamespace(ApplicationCacheNames.DASHBOARD_SUMMARY);
        } catch (RuntimeException ex) {
            log.warn("event=redis_cache_invalidation_failed scope=user reason={}", ex.getClass().getSimpleName());
        }
    }

    private UserListResponse buildUserListResponse(Boolean active) {
        var users = listUsers(active);
        var items = users.stream()
                .map(user -> UserSearchItemResponse.fromDomain(
                        user,
                        acceptTermsUseCase.hasAcceptedBiometricTerm(user.employeeId())
                ))
                .toList();
        return new UserListResponse(items);
    }

    private String listUsersScope(Boolean active) {
        var currentRole = jwtAuthenticatedUser.getCurrentRole();
        String tenantScope = currentRole == Role.CTO
                ? "tenant=all"
                : "companyId=" + domainAuthorizationService.authorizeCompanyAccess(null);
        return CacheScopes.authenticatedScope("role=" + currentRole, tenantScope, "active=" + active);
    }

    private <T> T cache(String cacheName, String scope, Class<T> type, java.util.function.Supplier<T> loader) {
        return cacheProvider.getOrLoad(cacheName, scope, type, loader);
    }
}
