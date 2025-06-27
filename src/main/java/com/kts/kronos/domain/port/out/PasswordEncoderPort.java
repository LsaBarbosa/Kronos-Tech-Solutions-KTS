package com.kts.kronos.domain.port.out;

public interface PasswordEncoderPort {
    boolean matches(String rawPassword, String passwordHash) ;
}
