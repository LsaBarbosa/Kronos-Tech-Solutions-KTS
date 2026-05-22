package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {
    List<AuditLogEntity> findByUserIdOrderByTimestampDesc(UUID userId);
}
