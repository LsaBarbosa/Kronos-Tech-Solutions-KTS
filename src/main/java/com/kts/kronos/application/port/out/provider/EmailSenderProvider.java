package com.kts.kronos.application.port.out.provider;

public interface EmailSenderProvider {
    void sendResetEmail(String toEmail, String token, String username);
}
