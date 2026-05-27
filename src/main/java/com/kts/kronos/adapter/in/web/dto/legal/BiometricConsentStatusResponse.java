package com.kts.kronos.adapter.in.web.dto.legal;

import com.kts.kronos.domain.model.BiometricConsentStatus;

public record BiometricConsentStatusResponse(
        boolean biometricConsentAccepted,
        String acceptedVersion,
        String acceptedHash,
        String currentVersion,
        String currentHash,
        boolean requiresNewAcceptance
) {
    public static BiometricConsentStatusResponse fromDomain(BiometricConsentStatus status) {
        return new BiometricConsentStatusResponse(
                status.accepted(),
                status.acceptedVersion(),
                status.acceptedHash(),
                status.currentVersion(),
                status.currentHash(),
                status.requiresNewAcceptance()
        );
    }
}
