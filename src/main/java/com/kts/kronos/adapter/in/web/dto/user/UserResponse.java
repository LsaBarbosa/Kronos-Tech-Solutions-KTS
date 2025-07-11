package com.kts.kronos.adapter.in.web.dto.user;

import com.kts.kronos.domain.model.User;

import java.util.UUID;

public record UserResponse(
        UUID userId,
        String username,
        String role,
        boolean active,
        UUID employeeId
) {
    public static UserResponse fromDomain(User user) {
        return new UserResponse(
                user.userId(),
                user.username(),
                user.role().name(),
                user.active(),
                user.employeeId()
        );
    }
}
