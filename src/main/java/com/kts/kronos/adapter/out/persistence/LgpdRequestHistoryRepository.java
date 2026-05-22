package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.LgpdRequestHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LgpdRequestHistoryRepository extends JpaRepository<LgpdRequestHistoryEntity, UUID> {
    List<LgpdRequestHistoryEntity> findByRequestIdOrderByCreatedAtAsc(UUID requestId);
}
