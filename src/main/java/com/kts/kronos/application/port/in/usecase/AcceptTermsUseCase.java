package com.kts.kronos.application.port.in.usecase;

import java.io.IOException;
import java.util.UUID;

public interface AcceptTermsUseCase {
    void acceptBiometricTerms(UUID employeeId, String ipAddress, String userAgent) throws IOException;
    boolean hasAcceptedBiometricTerm(UUID employeeId);

}
