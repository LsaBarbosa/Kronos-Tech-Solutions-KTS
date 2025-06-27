package com.kts.kronos.adapter.out.security.password;

import com.kts.kronos.domain.port.out.PasswordEncoderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BCryptPasswordEncoderAdapter implements PasswordEncoderPort {
    private final BCryptPasswordEncoder encoder;
    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return encoder.matches(rawPassword, passwordHash);
    }
}
