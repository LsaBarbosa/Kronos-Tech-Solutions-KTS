package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<DocumentEntity, UUID> {
    List<DocumentEntity> findByEmployeeId(UUID employeeId);
    List<DocumentEntity> findByEmployeeIdAndUploadedAtBetween(
            UUID employeeId,
            Instant  start,
            Instant end
    );
}
