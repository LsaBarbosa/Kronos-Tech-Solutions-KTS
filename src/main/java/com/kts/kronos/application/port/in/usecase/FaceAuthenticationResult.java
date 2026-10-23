package com.kts.kronos.application.port.in.usecase;

import com.kts.kronos.domain.model.BiometricConsentStatus;
import com.kts.kronos.domain.model.User;

import java.util.UUID;

public record FaceAuthenticationResult(
        User user,
        BiometricConsentStatus biometricConsentStatus,
        UUID activeCompanyId
) {
}
