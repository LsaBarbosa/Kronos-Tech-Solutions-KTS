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
    Optional<EmployeeEntity> findByCpf(String cpf);

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
}
