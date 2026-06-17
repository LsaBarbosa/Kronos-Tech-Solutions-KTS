package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.ServiceContractSignatureEntity;
import com.kts.kronos.domain.model.enuns.ContractSignatureStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ServiceContractSignatureRepository extends JpaRepository<ServiceContractSignatureEntity, UUID> {

    Optional<ServiceContractSignatureEntity> findByAssignmentIdAndStatus(
            UUID assignmentId,
            ContractSignatureStatus status
    );

    @Query("""
            SELECT s FROM ServiceContractSignatureEntity s
            WHERE s.companyId = :companyId
              AND (:contractId IS NULL OR s.contractId = :contractId)
              AND (:status IS NULL OR s.status = :status)
            """)
    Page<ServiceContractSignatureEntity> findAdminFiltered(
            Pageable pageable,
            @Param("companyId") UUID companyId,
            @Param("contractId") UUID contractId,
            @Param("status") ContractSignatureStatus status
    );
}
