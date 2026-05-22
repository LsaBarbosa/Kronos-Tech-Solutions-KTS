package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.RetentionPolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RetentionPolicyRepository extends JpaRepository<RetentionPolicyEntity, UUID> {
    List<RetentionPolicyEntity> findByEnabledTrueOrderByPolicyCodeAsc();
}
