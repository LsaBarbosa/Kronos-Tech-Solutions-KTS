package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.LegalConsentEntity;
import com.kts.kronos.domain.model.enuns.ConsentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
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

    Optional<LegalConsentEntity> findFirstByEmployeeIdAndConsentTypeAndVersionAndContentHashSha256AndRevokedAtIsNull(
            UUID employeeId,
            ConsentType consentType,
            String version,
            String contentHashSha256
    );

    @Query("""
            SELECT COUNT(c) FROM LegalConsentEntity c
             WHERE c.createdAt < :cutoff
               AND c.revokedAt IS NOT NULL
               AND c.retentionAppliedAt IS NULL
            """)
    long countRevokedConsentsBefore(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query("""
            UPDATE LegalConsentEntity c
               SET c.ipAddress = 'ANONYMIZED',
                   c.userAgent = 'ANONYMIZED',
                   c.retentionAppliedAt = :appliedAt,
                   c.retentionPolicyCode = :policyCode
             WHERE c.createdAt < :cutoff
               AND c.revokedAt IS NOT NULL
               AND c.retentionAppliedAt IS NULL
            """)
    int minimizeRevokedConsentsBefore(
            @Param("cutoff") Instant cutoff,
            @Param("appliedAt") Instant appliedAt,
            @Param("policyCode") String policyCode
    );

    @Deprecated(forRemoval = true)
    @Query("SELECT COUNT(c) FROM LegalConsentEntity c WHERE c.createdAt < :cutoff")
    long countCreatedBefore(@Param("cutoff") Instant cutoff);

    @Deprecated(forRemoval = true)
    @Modifying
    @Query("DELETE FROM LegalConsentEntity c WHERE c.createdAt < :cutoff")
    int deleteCreatedBefore(@Param("cutoff") Instant cutoff);

    List<LegalConsentEntity> findByEmployeeId(UUID employeeId);

    @Modifying
    @Query("DELETE FROM LegalConsentEntity c WHERE c.employeeId = :employeeId")
    int deleteByEmployeeId(@Param("employeeId") UUID employeeId);
}
