package com.kts.kronos.adapter.in.web.dto.lgpd;

import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.Role;

import java.util.UUID;

public record UserSummaryResponse(
        UUID userId,
        String username,
        Role role
) {
    public static UserSummaryResponse fromDomain(User user) {
        return new UserSummaryResponse(
                user.userId(),
                user.username(),
                user.role()
        );
    }
}
