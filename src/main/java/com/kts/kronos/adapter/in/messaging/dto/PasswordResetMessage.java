package com.kts.kronos.adapter.in.messaging.dto;

import java.io.Serializable;

public record PasswordResetMessage(String toEmail,
                                   String userName,
                                   String resetToken
) implements Serializable {
}