package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.roles.Role;

import java.util.List;
import java.util.UUID;

public record User( UUID userId, String username, String passwordHash, List<Role> roles,
                    boolean enabled,
                    UUID employeeId) {
}
