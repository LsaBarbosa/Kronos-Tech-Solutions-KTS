package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.LegalConsentEntity;
import com.kts.kronos.domain.model.enuns.ConsentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LegalConsentRepository extends JpaRepository<LegalConsentEntity, UUID> {
    Optional<LegalConsentEntity> findByEmployeeIdAndConsentTypeAndRevokedAtIsNull(
            UUID employeeId,
            ConsentType consentType
    );

    List<LegalConsentEntity> findByEmployeeIdOrderByGrantedAtDesc(UUID employeeId);

    List<LegalConsentEntity> findByEmployeeIdAndConsentTypeOrderByGrantedAtDesc(
            UUID employeeId,
            ConsentType consentType
    );

    boolean existsByEmployeeIdAndConsentTypeAndRevokedAtIsNull(UUID employeeId, ConsentType consentType);
}
