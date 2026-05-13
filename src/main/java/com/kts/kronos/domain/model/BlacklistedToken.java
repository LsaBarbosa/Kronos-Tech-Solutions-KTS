package com.kts.kronos.domain.model;

import java.time.LocalDateTime;

public record BlacklistedToken(String tokenHash, LocalDateTime expiresAt) {}
