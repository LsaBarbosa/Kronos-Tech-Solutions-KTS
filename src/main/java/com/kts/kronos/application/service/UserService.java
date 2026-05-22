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
import com.kts.kronos.domain.model.enuns.Role;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.kts.kronos.constants.Messages.*;

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

        if (req.password() != null && !req.password().isBlank()) {
            validatePasswordPolicy(req.password());
            password = passwordEncoder.encode(req.password());
        }

        var role = Role.valueOf(req.role() != null ? req.role() : existing.role().name());
        boolean active = req.enabled() != null ? req.enabled() : existing.active();
        var updated = new User(userId, username, password, role, active, existing.employeeId());

        try {
            userProvider.save(updated);
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
    }

    @Override
    public void toggleActivate(UUID userId) {
        var existing = getUserId(userId);
        var active = existing.withActive(!existing.active());
        userProvider.save(active);
        employeeUseCase.toggleActivate(existing.employeeId());
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
        userProvider.save(new User(
                user.userId(),
                user.username(),
                hashed,
                user.role(),
                user.active(),
                user.employeeId()
        ));
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
}
