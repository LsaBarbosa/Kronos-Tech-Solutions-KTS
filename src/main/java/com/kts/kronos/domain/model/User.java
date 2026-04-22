package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.enuns.Role;

import java.util.UUID;

public record User(
        UUID userId,
        String username,
        String password,
        Role role,
        boolean active,
        UUID employeeId,
        int tokenVersion
) {
    public User {
        if (tokenVersion < 0) {
            throw new IllegalArgumentException("tokenVersion nao pode ser negativo.");
        }
    }

    public User(UUID userId, String username, String password, Role role, boolean active, UUID employeeId) {
        this(userId, username, password, role, active, employeeId, 0);
    }

    public User(String username, String password, Role role, UUID employeeId) {
        this(UUID.randomUUID(), username, password, role, true, employeeId, 0);
    }

    public User withActive(boolean active) {
        return new User(
                userId, username, password, role, active, employeeId, tokenVersion
        );
    }

    public User withPassword(String password) {
        return new User(
                userId, username, password, role, active, employeeId, tokenVersion
        );
    }

    public User withTokenVersion(int tokenVersion) {
        return new User(
                userId, username, password, role, active, employeeId, tokenVersion
        );
    }

    public User withIncrementedTokenVersion() {
        return withTokenVersion(tokenVersion + 1);
    }
}
