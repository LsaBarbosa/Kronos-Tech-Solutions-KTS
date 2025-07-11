package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.user.CreateUserRequest;
import com.kts.kronos.adapter.in.web.dto.user.UpdateUserRequest;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.UserUseCase;
import com.kts.kronos.application.port.out.repository.EmployeeRepository;
import com.kts.kronos.application.port.out.repository.UserRepository;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserService implements UserUseCase {
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    public void createUser(CreateUserRequest req) {

        if (userRepository.findByUsername(req.username()).isPresent()) {
            throw new BadRequestException("Username já existe");
        }
        // valida employee
        employeeRepository.findById(req.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee não encontrado"));

        User u = new User(
                req.username(),
                req.password(),
                Role.valueOf(req.role()),
                req.employeeId()
        );
        userRepository.save(u);
    }

    @Override
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User não encontrado"));
    }

    @Override
    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Colborador não encontrado" + userId));
    }
    @Override
    public User getUserByEmployee(UUID employeeId) {
        return userRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Colborador não encontrado" + employeeId));
    }

    @Override
    public List<User> listUsers(Boolean active) {
        return active == null
                ? userRepository.findAll()
                : userRepository.findByActive(active);
    }

    @Override
    public void updateUser(UUID userId, UpdateUserRequest req) {
        User existing = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User não encontrado"));
        User updated = new User(
                userId,
                req.username(),
                req.password() != null ? req.password() : existing.password(),
                Role.valueOf(req.role() != null ? req.role() : existing.role().name()),
                req.enabled() != null ? req.enabled() : existing.active(),
                existing.employeeId()
        );
        userRepository.save(updated);
    }

    @Override
    public void deleteUser(UUID userId) {
        var existing = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User não encontrado"));
        userRepository.deleteById(userId);
        employeeRepository.findById(existing.employeeId())
                .ifPresent(emp -> employeeRepository.deleteById(emp.employeeId()));
    }

    @Override
    public void activateUser(UUID userId) {
        var existing = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User não encontrado"));
        var active = existing.withActive(true);
        userRepository.save(active);
        employeeRepository.findById(existing.employeeId())
                .ifPresent(emp -> employeeRepository.save(emp.withActive(true)));
    }

    @Override
    public void deactivateUser(UUID userId) {
        var existing = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User não encontrado"));
        var deactiveUser = existing.withActive(false);
        userRepository.save(deactiveUser);

      employeeRepository.findById(existing.employeeId())
                .ifPresent(emp -> employeeRepository.save(emp.withActive(false)));
    }
}
