package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.domain.model.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserProvider {
    void save(User user);
    Optional<User> findByUsername(String username);
    Optional<User> findById(UUID userId);
    List<User> findAll();
    List<User> findByActive(boolean active);
    Optional<User> findByEmployeeId(UUID employeeId);
    List<User> findByEmployeeIds(Collection<UUID> employeeIds);
    List<User> findByEmployeeIdsAndActive(Collection<UUID> employeeIds, boolean active);
    void deleteById(UUID userId);
    List<User> findAllByIds(Collection<UUID> ids);
}
