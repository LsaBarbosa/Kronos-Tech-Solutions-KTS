package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.domain.model.LegalConsent;
import com.kts.kronos.domain.model.enuns.ConsentType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LegalConsentProvider {
    LegalConsent save(LegalConsent consent);
    Optional<LegalConsent> findActive(UUID employeeId, ConsentType type);
    boolean existsActive(UUID employeeId, ConsentType type);
    List<LegalConsent> findAllByEmployeeId(UUID employeeId);
}
