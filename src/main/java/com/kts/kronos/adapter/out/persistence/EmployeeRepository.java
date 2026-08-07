package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.EmployeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.kts.kronos.application.port.out.projection.CompanyEmployeeCountsProjection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
public interface EmployeeRepository extends JpaRepository<EmployeeEntity, UUID> {
    boolean existsByCpf(String cpf);
    Optional<EmployeeEntity> findByPis(String pis);
    Optional<EmployeeEntity> findByCompanyIdAndPisAndDeletedAtIsNull(UUID companyId, String pis);
    Optional<EmployeeEntity> findByCpf(String cpf);

    boolean existsByCompanyIdAndCpfAndDeletedAtIsNull(UUID companyId, String cpf);
    Optional<EmployeeEntity> findByCompanyIdAndCpfAndDeletedAtIsNull(UUID companyId, String cpf);
    List<EmployeeEntity> findAllByCpf(String cpf);

    void deleteById(UUID id);
    List<EmployeeEntity> findByCompanyId(UUID companyId);
    List<EmployeeEntity> findByCompanyIdAndActive(UUID companyId, boolean active);
    long countByCompanyIdAndActive(UUID companyId, boolean active);
    @Query("""
        SELECT
            e.companyId AS companyId,
            COALESCE(SUM(CASE WHEN e.active = true THEN 1 ELSE 0 END), 0) AS activeCount,
            COALESCE(SUM(CASE WHEN e.active = false THEN 1 ELSE 0 END), 0) AS inactiveCount
        FROM EmployeeEntity e
        WHERE e.companyId IN :companyIds
        GROUP BY e.companyId
    """)
    List<CompanyEmployeeCountsProjection> countByCompanyIds(@Param("companyIds") Collection<UUID> companyIds);

    @Query("SELECT COUNT(e) FROM EmployeeEntity e WHERE e.faceS3ObjectKey IS NOT NULL")
    long countByFaceS3ObjectKeyIsNotNullAndCreatedAtBefore(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query("UPDATE EmployeeEntity e SET e.faceS3ObjectKey = NULL WHERE e.faceS3ObjectKey IS NOT NULL")
    int clearBiometricDataBefore(Instant cutoff);

    @Query("""
            SELECT e FROM EmployeeEntity e
             WHERE e.faceS3ObjectKey IS NOT NULL
               AND NOT EXISTS (
                   SELECT 1 FROM LegalConsentEntity lc
                    WHERE lc.employeeId = e.employeeId
                      AND lc.consentType = 'BIOMETRIC_AUTHENTICATION'
                      AND lc.revokedAt IS NULL
                      AND lc.version = :version
                      AND lc.contentHashSha256 = :contentHashSha256
               )
               AND NOT EXISTS (
                   SELECT 1 FROM LegalConsentEntity lc
                    WHERE lc.employeeId = e.employeeId
                      AND lc.consentType = 'BIOMETRIC_AUTHENTICATION'
                      AND lc.revokedAt IS NOT NULL
               )
            """)
    List<EmployeeEntity> findEligibleBiometricArtifactsWithoutValidCurrentConsent(
            @Param("version") String version,
            @Param("contentHashSha256") String contentHashSha256
    );

    @Query("""
            SELECT e FROM EmployeeEntity e
             WHERE e.faceS3ObjectKey IS NOT NULL
               AND NOT EXISTS (
                   SELECT 1 FROM LegalConsentEntity lc
                    WHERE lc.employeeId = e.employeeId
                      AND lc.consentType = 'BIOMETRIC_AUTHENTICATION'
                      AND lc.revokedAt IS NULL
                      AND lc.version = :version
                      AND lc.contentHashSha256 = :contentHashSha256
               )
               AND (
                   SELECT MAX(lc.revokedAt) FROM LegalConsentEntity lc
                    WHERE lc.employeeId = e.employeeId
                      AND lc.consentType = 'BIOMETRIC_AUTHENTICATION'
                      AND lc.revokedAt IS NOT NULL
               ) <= :cutoff
            """)
    List<EmployeeEntity> findEligibleBiometricArtifactsByRevokedConsent(
            @Param("cutoff") Instant cutoff,
            @Param("version") String version,
            @Param("contentHashSha256") String contentHashSha256
    );

    @Modifying
    @Query("UPDATE EmployeeEntity e SET e.faceS3ObjectKey = NULL WHERE e.employeeId = :employeeId")
    int clearBiometricDataByEmployeeId(@Param("employeeId") UUID employeeId);
}
