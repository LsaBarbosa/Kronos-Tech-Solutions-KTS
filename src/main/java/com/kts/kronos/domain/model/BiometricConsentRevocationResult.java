package com.kts.kronos.domain.model;

import java.util.UUID;

public record BiometricConsentRevocationResult(
        UUID employeeId,
        UUID userId,
        long newSessionVersion,
        BiometricConsentStatus consentStatus
) {}
