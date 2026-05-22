package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.LgpdRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LgpdRequestRepository extends JpaRepository<LgpdRequestEntity, UUID> {
    List<LgpdRequestEntity> findByEmployeeIdOrderByCreatedAtDesc(UUID employeeId);

    List<LgpdRequestEntity> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    List<LgpdRequestEntity> findAllByOrderByCreatedAtDesc();
}
