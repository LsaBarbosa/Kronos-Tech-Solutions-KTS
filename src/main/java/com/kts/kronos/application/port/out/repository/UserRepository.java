package com.kts.kronos.application.port.out.repository;

import com.kts.kronos.domain.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User save(User user);
    Optional<User> findByUsername(String username);
    Optional<User> findById(UUID userId);
    List<User> findAll();
    List<User> findByActive(boolean active);
    Optional<User> findByEmployeeId(UUID employeeId);
    void deleteById(UUID userId);
}
