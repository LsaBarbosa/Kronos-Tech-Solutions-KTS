package com.kts.kronos.domain.model;

import java.util.UUID;

public record BiometricConsentAcceptanceResult(
        UUID employeeId,
        UUID userId,
        long sessionVersion,
        BiometricConsentStatus consentStatus
) {}
