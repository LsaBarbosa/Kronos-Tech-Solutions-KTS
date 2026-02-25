package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.security.ChangePasswordRequest;
import com.kts.kronos.adapter.in.web.dto.user.CreateUserRequest;
import com.kts.kronos.adapter.in.web.dto.user.UpdateUserRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.adapter.out.security.PasswordPolicyValidator;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.EmployeeUseCase;
import com.kts.kronos.application.port.in.usecase.UserUseCase;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.kts.kronos.constants.Logs.*;
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
    private final PasswordPolicyValidator passwordPolicyValidator;

    @Override
    public void createUser(CreateUserRequest req) {
        log.info(LOG_USER_CREATE_INIT, req.username(), req.role());

        employeeProvider.findById(req.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));

        if (userProvider.findByUsername(req.username().toLowerCase()).isPresent()) {
            throw new BadRequestException(USERNAME_ALREADY_EXIST);
        }

        var randomSystemPassword = UUID.randomUUID().toString();
        var hashed = passwordEncoder.encode(randomSystemPassword);

        var user = new User(
                req.username(),
                hashed,
                Role.valueOf(req.role()),
                req.employeeId()
        );
        userProvider.save(user);
        log.info(LOG_USER_CREATE_SUCCESS, user.username(), user.employeeId());
    }

    @Override
    public User getUserByUsername(String username) {
        var targetUser = userProvider.findByUsername(username.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));

        validateUserCompanyAccess(targetUser.employeeId());
        return targetUser;
    }

    @Override
    public User getUserById(UUID userId) {
        var targetUserId = jwtAuthenticatedUser.isWithEmployeeId(userId);
        var user = userProvider.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + userId));

        validateUserCompanyAccess(user.employeeId());
        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> listUsers(Boolean active) {
        var role = jwtAuthenticatedUser.getRoleFromToken();
        var employeeId = jwtAuthenticatedUser.getEmployeeId();

        var authenticatedEmployee = employeeProvider.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));

        log.debug(LOG_USER_LIST, active, authenticatedEmployee.companyId());

        if ("CTO".equals(role)) {
            return active == null ? userProvider.findAll() : userProvider.findByActive(active);
        }

        return userProvider.findByCompanyIdAndActive(authenticatedEmployee.companyId(), active);
    }

    @Override
    public void updateUser(UUID userId, UpdateUserRequest req) {
        log.info(LOG_USER_UPDATE, userId);
        var existing = getExistingUser(userId);

        var username = req.username() != null ? req.username().toLowerCase() : existing.username();
        var password = existing.password();

        if (req.password() != null && !req.password().isBlank()) {
            passwordPolicyValidator.validate(req.password());
            password = passwordEncoder.encode(req.password());
        }

        var role = Role.valueOf(req.role() != null ? req.role() : existing.role().name());
        boolean active = req.enabled() != null ? req.enabled() : existing.active();
        var updated = new User(userId, username, password, role, active, existing.employeeId());

        userProvider.save(updated);
    }

    @Override
    public void deleteUser(UUID userId) {
        log.warn(LOG_USER_DELETE, userId);
        var existing = getExistingUser(userId);
        var employeeId = existing.employeeId();

        documentProvider.deleteByEmployeeId(employeeId);
        timeRecordProvider.deleteByEmployeeId(employeeId);
        userProvider.deleteById(userId);
        employeeProvider.deleteById(employeeId);
    }


    @Override
    public void toggleActivate(UUID userId) {
        var existing = getExistingUser(userId);
        var active = existing.withActive(!existing.active());
        userProvider.save(active);
        employeeUseCase.toggleActivate(existing.employeeId());
    }

    @Override
    public void changeOwnPassword(ChangePasswordRequest req) {
        var userId = jwtAuthenticatedUser.getuserId();
        log.info(LOG_USER_PASSWORD_CHANGE, userId);
        var user = getExistingUser(userId);

        if (!passwordEncoder.matches(req.currentPassword(), user.password())) {
            throw new BadRequestException(INVALID_PASSWORD);
        }
        if (req.newPassword() == null || !req.newPassword().equals(req.confirmPassword())) {
            throw new BadRequestException(INVALID_CONFIRM_PASSWORD);
        }
        passwordPolicyValidator.validate(req.newPassword());
        var hashed = passwordEncoder.encode(req.newPassword());
        userProvider.save(user.withPassword(hashed));
    }

    @Override
    @Transactional(readOnly = true)
    public User getOwnProfile() {
        return getExistingUser(jwtAuthenticatedUser.getuserId());
    }

    @Override
    public boolean usernameExists(String username) {
        return userProvider.findByUsername(username.toLowerCase()).isPresent();
    }

    private User getExistingUser(UUID userId) {
        return userProvider.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
    }

    private void validateUserCompanyAccess(UUID targetEmployeeId) {
        if (jwtAuthenticatedUser.getRoleFromToken().equals("CTO")) return;

        var authEmployee = employeeProvider.findById(jwtAuthenticatedUser.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));

        var targetEmployee = employeeProvider.findById(targetEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));

        if (!targetEmployee.companyId().equals(authEmployee.companyId())) {
            log.error(LOG_USER_ACCESS_DENIED);
            throw new ResourceNotFoundException(USER_NOT_FOUND);
        }
    }
}
