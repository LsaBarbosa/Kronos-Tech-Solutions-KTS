package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.security.ChangePasswordRequest;
import com.kts.kronos.adapter.in.web.dto.user.CreateUserRequest;
import com.kts.kronos.adapter.in.web.dto.user.UpdateUserRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.EmployeeUseCase;
import com.kts.kronos.application.port.in.usecase.UserUseCase;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.Role;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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

    @Override
    public void createUser(CreateUserRequest req) {

        if (userProvider.findByUsername(req.username().toLowerCase()).isPresent()) {
            throw new BadRequestException(USERNAME_ALREADY_EXIST);
        }

        findById(req.employeeId());

        var randomSystemPassword = UUID.randomUUID().toString();
        var hashed = passwordEncoder.encode(randomSystemPassword);

        var user = new User(
                req.username(),
                hashed,
                Role.valueOf(req.role()),
                req.employeeId()
        );
        userProvider.save(user);
    }

    @Override
    public User getUserByUsername(String username) {
        var authenticatedUserEmployeeId = jwtAuthenticatedUser.getEmployeeId();
        var authenticatedUserEmployee = employeeProvider.findById(authenticatedUserEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));
        var companyId = authenticatedUserEmployee.companyId();

        var targetUser = userProvider.findByUsername(username.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));

        if (jwtAuthenticatedUser.getCurrentRole() == Role.CTO) {
            return targetUser;
        }

        var targetEmployee = employeeProvider.findById(targetUser.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));

        if (!targetEmployee.companyId().equals(companyId)) {
            throw new ResourceNotFoundException(USER_NOT_FOUND);
        }

        return targetUser;
    }

    @Override
    public User getUserById(UUID userId) {
        return getUserId(userId);
    }

    @Override
    public List<User> listUsers(Boolean active) {
        var authenticatedUserEmployeeId = jwtAuthenticatedUser.getEmployeeId();
        var authenticatedUserEmployee = employeeProvider.findById(authenticatedUserEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));
        var companyId = authenticatedUserEmployee.companyId();

        List<User> allUsers = active == null
                ? userProvider.findAll()
                : userProvider.findByActive(active);

        if (jwtAuthenticatedUser.getCurrentRole() == Role.CTO) {
            return allUsers;
        }

        return allUsers.stream()
                .filter(user -> {
                    var employee = employeeProvider.findById(user.employeeId());
                    return employee.isPresent() && employee.get().companyId().equals(companyId);
                })
                .collect(Collectors.toList());
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

        userProvider.save(updated);
    }

    @Override
    public void deleteUser(UUID userId) {
        var existing = getUserId(userId);
        var employeeId = existing.employeeId();
        documentProvider.deleteByEmployeeId(employeeId);
        timeRecordProvider.deleteByEmployeeId(employeeId);
        userProvider.deleteById(userId);
        employeeProvider.deleteById(employeeId);
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
        return userProvider.findByUsername(username.toLowerCase()).isPresent();
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
}
