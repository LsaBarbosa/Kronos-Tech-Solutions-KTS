package com.kts.kronos.domain.model;

public record BiometricConsentStatus(
        boolean accepted,
        String acceptedVersion,
        String acceptedHash,
        String currentVersion,
        String currentHash,
        boolean requiresNewAcceptance
) {}
