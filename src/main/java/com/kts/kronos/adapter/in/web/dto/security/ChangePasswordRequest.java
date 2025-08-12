package com.kts.kronos.adapter.in.web.dto.security;

public record ChangePasswordRequest(String currentPassword,
                                    String newPassword,
                                    String confirmPassword) {
}
