package com.kts.kronos.application.port.in.usecase;

import java.util.UUID;

public interface AcceptTermsUseCase {
    void acceptBiometricTerms(UUID employeeId, String ipAddress, String userAgent);
    boolean hasAcceptedBiometricTerm(UUID employeeId);

}
