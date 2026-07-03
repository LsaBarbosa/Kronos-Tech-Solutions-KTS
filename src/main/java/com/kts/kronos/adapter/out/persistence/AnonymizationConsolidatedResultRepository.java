package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.AnonymizationConsolidatedResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AnonymizationConsolidatedResultRepository extends JpaRepository<AnonymizationConsolidatedResultEntity, UUID> {
    @Query("""
            SELECT acr FROM AnonymizationConsolidatedResultEntity acr
             WHERE acr.requestId = :requestId
            """)
    Optional<AnonymizationConsolidatedResultEntity> findByRequestId(@Param("requestId") UUID requestId);

    @Query("""
            SELECT acr FROM AnonymizationConsolidatedResultEntity acr
             WHERE acr.employeeId = :employeeId AND acr.companyId = :companyId
             ORDER BY acr.finishedAt DESC
             LIMIT 1
            """)
    Optional<AnonymizationConsolidatedResultEntity> findLatestByEmployeeAndCompany(
            @Param("employeeId") UUID employeeId,
            @Param("companyId") UUID companyId
    );

    @Modifying
    @Query("DELETE FROM AnonymizationConsolidatedResultEntity acr WHERE acr.employeeId = :employeeId")
    int deleteByEmployeeId(@Param("employeeId") UUID employeeId);
}
