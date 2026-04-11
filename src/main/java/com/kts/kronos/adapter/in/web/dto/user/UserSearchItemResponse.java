package com.kts.kronos.adapter.in.web.dto.user;

import com.kts.kronos.domain.model.User;

import java.util.UUID;

public record UserSearchItemResponse(
        UUID userId,
        String username,
        String role,
        boolean active
) {
    public static UserSearchItemResponse fromDomain(User user) {
        return new UserSearchItemResponse(
                user.userId(),
                user.username(),
                user.role().name(),
                user.active()
        );
    }
}
