package com.kts.kronos.application.port.out.provider;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenProvider {
    String generateAndSaveToken(UUID userId);
    Optional<UUID> validateToken(String token);
    void deleteToken(String token);


}
