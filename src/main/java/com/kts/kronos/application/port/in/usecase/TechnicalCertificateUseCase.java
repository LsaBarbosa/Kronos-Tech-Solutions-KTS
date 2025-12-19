package com.kts.kronos.application.port.in.usecase;

import java.util.UUID;

public interface TechnicalCertificateUseCase {
    byte[] generateCertificate(UUID companyId);
}
