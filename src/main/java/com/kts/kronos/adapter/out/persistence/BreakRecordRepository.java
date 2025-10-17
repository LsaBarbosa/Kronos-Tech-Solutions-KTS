package com.kts.kronos.adapter.out.persistence;
import com.kts.kronos.adapter.out.persistence.entity.BreakRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BreakRecordRepository extends JpaRepository<BreakRecordEntity, Long> {
    List<BreakRecordEntity> findByTimeRecordId(Long timeRecordId);
    Optional<BreakRecordEntity> findFirstByTimeRecordIdAndEndBreakIsNullAndActiveTrueOrderByStartBreakDesc(Long timeRecordId);
}