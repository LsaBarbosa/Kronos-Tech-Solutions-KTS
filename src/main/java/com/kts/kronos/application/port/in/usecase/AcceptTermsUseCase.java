package com.kts.kronos.application.port.in.usecase;

import com.kts.kronos.domain.model.LegalConsent;
import com.kts.kronos.domain.model.LegalText;

import java.util.List;
import java.util.UUID;

public interface AcceptTermsUseCase {
    LegalText getCurrentBiometricTerm();
    void acceptBiometricTerms(UUID employeeId, UUID userId, String ipAddress, String userAgent, String version, String contentHashSha256);
    void revokeBiometricTerms(UUID employeeId, String ipAddress, String userAgent);
    boolean hasAcceptedBiometricTerm(UUID employeeId);
    List<LegalConsent> getConsentHistory(UUID employeeId);
}
