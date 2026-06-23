package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.DemoJobAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DemoJobAuditRepository extends JpaRepository<DemoJobAuditEntity, UUID> {
    List<DemoJobAuditEntity> findBySandboxKeyOrderByStartedAtDesc(String sandboxKey);
    Optional<DemoJobAuditEntity> findTopBySandboxKeyAndStatusOrderByStartedAtDesc(String sandboxKey, String status);
}
