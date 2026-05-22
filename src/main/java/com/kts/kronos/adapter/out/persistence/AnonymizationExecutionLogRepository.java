package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.AnonymizationExecutionLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AnonymizationExecutionLogRepository extends JpaRepository<AnonymizationExecutionLogEntity, UUID> {
}
