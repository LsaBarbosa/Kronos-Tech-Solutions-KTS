package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.ServiceContractEntity;
import com.kts.kronos.domain.model.enuns.ServiceContractStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ServiceContractRepository extends JpaRepository<ServiceContractEntity, UUID> {

    @Query("""
            SELECT c FROM ServiceContractEntity c
            WHERE c.companyId = :companyId
              AND (:status IS NULL OR c.status = :status)
            """)
    Page<ServiceContractEntity> findByCompanyFiltered(
            Pageable pageable,
            @Param("companyId") UUID companyId,
            @Param("status") ServiceContractStatus status
    );
}
