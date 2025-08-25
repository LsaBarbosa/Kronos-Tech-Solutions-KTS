package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.security.ChangePasswordRequest;
import com.kts.kronos.adapter.in.web.dto.user.CreateUserRequest;
import com.kts.kronos.adapter.in.web.dto.user.UpdateUserRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.UserUseCase;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.Role;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.*;

@Component
@RequiredArgsConstructor
@Transactional
public class UserService implements UserUseCase {

    private final UserProvider userProvider;
    private final EmployeeProvider employeeProvider;
    private final PasswordEncoder passwordEncoder;
    private final JwtAuthenticatedUser jwtAuthenticatedUser;

    @Override
    public void createUser(CreateUserRequest req) {

        if (userProvider.findByUsername(req.username()).isPresent()) {
            throw new BadRequestException(USERNAME_ALREADY_EXIST);
        }
        // valida employee
        findById(req.employeeId());

        validatePasswordPolicy(req.password()); // política (abaixo)
        var hashed = passwordEncoder.encode(req.password());

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
        return userProvider.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
    }

    @Override
    public User getUserById(UUID userId) {
        var targetUserId = jwtAuthenticatedUser.isWithEmployeeId(userId);
        return userProvider.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + userId));
    }

    @Override
    public List<User> listUsers(Boolean active) {
        return active == null
                ? userProvider.findAll()
                : userProvider.findByActive(active);
    }

    @Override
    public void updateUser(UUID userId, UpdateUserRequest req) {
        var existing = getUserId(userId);

        var username = req.username() != null ? req.username() : existing.username();
        var password = existing.password();

        if (req.password() != null && !req.password().isBlank()) {
            validatePasswordPolicy(req.password());
            password = passwordEncoder.encode(req.password());
        }

        Role role = Role.valueOf(req.role() != null ? req.role() : existing.role().name());
        boolean active = req.enabled() != null ? req.enabled() : existing.active();
        var updated = new User(userId, username, password, role, active, existing.employeeId());

        userProvider.save(updated);
    }

    @Override
    public void deleteUser(UUID userId) {
        var existing = getUserId(userId);
        userProvider.deleteById(userId);
        employeeProvider.findById(existing.employeeId())
                .ifPresent(emp -> employeeProvider.deleteById(emp.employeeId()));
    }


    @Override
    public void toggleActivate(UUID userId) {
        var existing = getUserId(userId);
        var active = existing.withActive(!existing.active());
        userProvider.save(active);
        employeeProvider.findById(existing.employeeId())
                .ifPresent(emp -> employeeProvider.save(emp.withActive(!existing.active())));
    }

    @Override
    public void changeOwnPassword(ChangePasswordRequest req) {
        var userId = jwtAuthenticatedUser.getuserId();
        User user = getUserId(userId);

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

    private void validatePasswordPolicy(String raw) {
        // exemplo simples: 8+ chars, 1 maiúscula, 1 minúscula, 1 dígito
        if (raw == null || !raw.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")) {
            throw new BadRequestException(INVALID_PASSWORD_POLICY);
        }
    }
    private void findById(UUID userId) {
        employeeProvider.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));
    }
    private User getUserId(UUID userId) {
        return userProvider.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
    }
}
