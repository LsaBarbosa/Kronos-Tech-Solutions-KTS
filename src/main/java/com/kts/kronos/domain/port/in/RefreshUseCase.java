package com.kts.kronos.domain.port.in;

import com.kts.kronos.domain.model.dto.AuthResponse;
import com.kts.kronos.domain.model.dto.RefreshToken;

public interface RefreshUseCase {
    AuthResponse refresh(RefreshToken request);
}
