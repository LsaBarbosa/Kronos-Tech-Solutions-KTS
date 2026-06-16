package com.kts.kronos.adapter.in.web.dto.user;

import com.kts.kronos.domain.model.User;

import java.util.UUID;

public record UserSearchItemResponse(
        UUID userId,
        UUID employeeId,
        String username,
        String role,
        boolean active,
        boolean biometricConsentAccepted
) {
    public static UserSearchItemResponse fromDomain(User user, boolean biometricConsentAccepted) {
        return new UserSearchItemResponse(
                user.userId(),
                user.employeeId(),
                user.username(),
                user.role().name(),
                user.active(),
                biometricConsentAccepted
        );
    }
}
