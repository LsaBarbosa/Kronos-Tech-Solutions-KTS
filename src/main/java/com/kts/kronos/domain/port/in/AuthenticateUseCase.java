package com.kts.kronos.domain.port.in;

import com.kts.kronos.domain.model.dto.AuthResponse;
import com.kts.kronos.domain.model.dto.LoginRequest;

public interface AuthenticateUseCase {
    AuthResponse authenticate(LoginRequest request);
}
