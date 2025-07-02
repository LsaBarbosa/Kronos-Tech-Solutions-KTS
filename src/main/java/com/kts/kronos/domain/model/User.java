package com.kts.kronos.domain.model;

import java.util.UUID;

public record User(
        UUID userId,
        String username,
        String password,
        Role role,
        boolean active,
        UUID employeeId
) {
    public User(String username, String password, Role role, UUID employeeId) {
        this(UUID.randomUUID(), username, password, role, true, employeeId);
    }

    public User withActive(boolean active) {
        return new User(
                userId, username, password, role, active, employeeId
        );
    }

}
