package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.TimeRecordApprovalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
public interface TimeRecordApprovalRepository extends JpaRepository<TimeRecordApprovalEntity, Long> {
    @Modifying
    @Transactional
    @Query("DELETE FROM TimeRecordApprovalEntity t WHERE t.createdAt <= :threshold")
    void deleteByCreatedAtBefore(LocalDateTime threshold);
}
