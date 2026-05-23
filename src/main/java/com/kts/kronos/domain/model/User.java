package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.enuns.Role;

import java.time.LocalDateTime;
import java.util.UUID;

public record User(
        UUID userId,
        String username,
        String password,
        Role role,
        boolean active,
        UUID employeeId,
        Long sessionVersion,
        LocalDateTime deletedAt,
        UUID deletedBy,
        String deactivationReason
) {
    public User {
        sessionVersion = sessionVersion == null ? 0L : sessionVersion;
    }

    public User(UUID userId, String username, String password, Role role, boolean active, UUID employeeId) {
        this(userId, username, password, role, active, employeeId, 0L, null, null, null);
    }

    public User(
            UUID userId,
            String username,
            String password,
            Role role,
            boolean active,
            UUID employeeId,
            LocalDateTime deletedAt,
            UUID deletedBy,
            String deactivationReason
    ) {
        this(userId, username, password, role, active, employeeId, 0L, deletedAt, deletedBy, deactivationReason);
    }

    public User(String username, String password, Role role, UUID employeeId) {
        this(UUID.randomUUID(), username, password, role, true, employeeId, 0L, null, null, null);
    }

    public User withActive(boolean active) {
        return new User(
                userId,
                username,
                password,
                role,
                active,
                employeeId,
                sessionVersion,
                active ? null : deletedAt,
                active ? null : deletedBy,
                active ? null : deactivationReason
        );
    }

    public User deactivate(UUID deletedBy, String reason) {
        return new User(
                userId,
                username,
                password,
                role,
                false,
                employeeId,
                sessionVersion,
                LocalDateTime.now(),
                deletedBy,
                reason
        );
    }

    public User withPassword(String password) {
        return new User(
                userId, username, password, role, active, employeeId, sessionVersion, deletedAt, deletedBy, deactivationReason
        );
    }

    public User incrementSessionVersion() {
        return new User(
                userId,
                username,
                password,
                role,
                active,
                employeeId,
                sessionVersion + 1,
                deletedAt,
                deletedBy,
                deactivationReason
        );
    }
}
