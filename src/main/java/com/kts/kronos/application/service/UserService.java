package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.user.CreateUserRequest;
import com.kts.kronos.adapter.in.web.dto.user.UpdateUserRequest;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.UserUseCase;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.Role;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional
public class UserService implements UserUseCase {
    private final UserProvider userProvider;
    private final EmployeeProvider employeeProvider;

    @Override
    public void createUser(CreateUserRequest req) {

        if (userProvider.findByUsername(req.username()).isPresent()) {
            throw new BadRequestException("Username já existe");
        }
        // valida employee
        employeeProvider.findById(req.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee não encontrado"));

        User u = new User(
                req.username(),
                req.password(),
                Role.valueOf(req.role()),
                req.employeeId()
        );
        userProvider.save(u);
    }

    @Override
    public User getUserByUsername(String username) {
        return userProvider.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User não encontrado"));
    }

    @Override
    public User getUserById(UUID userId) {
        return userProvider.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Colborador não encontrado" + userId));
    }
    @Override
    public User getUserByEmployee(UUID employeeId) {
        return userProvider.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Colborador não encontrado" + employeeId));
    }

    @Override
    public List<User> listUsers(Boolean active) {
        return active == null
                ? userProvider.findAll()
                : userProvider.findByActive(active);
    }

    @Override
    public void updateUser(UUID userId, UpdateUserRequest req) {
        User existing = userProvider.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User não encontrado"));
        User updated = new User(
                userId,
                req.username(),
                req.password() != null ? req.password() : existing.password(),
                Role.valueOf(req.role() != null ? req.role() : existing.role().name()),
                req.enabled() != null ? req.enabled() : existing.active(),
                existing.employeeId()
        );
        userProvider.save(updated);
    }

    @Override
    public void deleteUser(UUID userId) {
        var existing = userProvider.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User não encontrado"));
        userProvider.deleteById(userId);
        employeeProvider.findById(existing.employeeId())
                .ifPresent(emp -> employeeProvider.deleteById(emp.employeeId()));
    }

    @Override
    public void toggleActivate(UUID userId) {
        var existing = userProvider.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User não encontrado"));
        var active = existing.withActive(!existing.active());
        userProvider.save(active);
        employeeProvider.findById(existing.employeeId())
                .ifPresent(emp -> employeeProvider.save(emp.withActive(!existing.active())));
    }
}
