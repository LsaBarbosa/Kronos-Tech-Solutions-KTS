package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.LgpdRequestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface LgpdRequestRepository extends JpaRepository<LgpdRequestEntity, UUID> {
    List<LgpdRequestEntity> findByEmployeeIdOrderByCreatedAtDesc(UUID employeeId);

    List<LgpdRequestEntity> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    List<LgpdRequestEntity> findAllByOrderByCreatedAtDesc();

    Page<LgpdRequestEntity> findByCompanyIdOrderByCreatedAtDesc(UUID companyId, Pageable pageable);

    Page<LgpdRequestEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT COUNT(r) FROM LgpdRequestEntity r WHERE r.createdAt < :cutoff")
    long countCreatedBefore(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query("DELETE FROM LgpdRequestEntity r WHERE r.createdAt < :cutoff")
    int deleteCreatedBefore(@Param("cutoff") Instant cutoff);
}
