package com.kts.kronos.domain.port.out;

import org.springframework.security.core.Authentication;

public interface TokenPort {
    String generateToken(Authentication authentication);
    String generateRefreshToken(Authentication authentication);
    Authentication parseToken(String token);
}
