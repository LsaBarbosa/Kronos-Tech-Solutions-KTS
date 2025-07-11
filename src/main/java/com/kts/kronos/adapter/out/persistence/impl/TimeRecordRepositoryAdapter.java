package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.SpringDataTimeRecordRepository;
import com.kts.kronos.adapter.out.persistence.entity.TimeRecordEntity;
import com.kts.kronos.application.port.out.repository.TimeRecordRepository;
import com.kts.kronos.domain.model.TimeRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class TimeRecordRepositoryAdapter implements TimeRecordRepository {
    private final SpringDataTimeRecordRepository jpa;

    @Override
    public TimeRecord save(TimeRecord tr) {
        var entity = TimeRecordEntity.fromDomain(tr);
        var saved = jpa.save(entity);
        return saved.toDomain().withId(saved.getTimeRecordId());
    }

    @Override
    public Optional<TimeRecord> findById(Long id) {
        return jpa.findById(id)
                .map(TimeRecordEntity::toDomain);
    }

    @Override
    public Optional<TimeRecord> findTopByEmployeeIdOrderByStartWorkDesc(UUID employeeId) {
        return jpa.findLatestByEmployeeId(employeeId)
                .map(TimeRecordEntity::toDomain);
    }

    @Override
    public void deleteTimeRecord(TimeRecord timeRecord) {
        var entity = TimeRecordEntity.fromDomain(timeRecord);
        jpa.delete(entity);
    }


    @Override
    public Optional<TimeRecord> findOpenByEmployeeId(UUID employeeId) {
        return jpa
                .findFirstByEmployeeIdAndEndWorkIsNullOrderByStartWorkDesc(employeeId)
                .map(TimeRecordEntity::toDomain);
    }

    @Override
    public List<TimeRecord> findByEmployeeIdAndActive(UUID employeeId, boolean active) {
        return jpa.findByEmployeeIdAndActive(employeeId, active)
                .stream()
                .map(TimeRecordEntity::toDomain)
                .toList();
    }
    @Override
    public List<TimeRecord> findByEmployeeId(UUID employeeId) {
        return jpa.findByEmployeeId(employeeId)
                .stream()
                .map(TimeRecordEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsByEmployeeIdAndDate(UUID employeeId, LocalDate date) {
        var dayStart = date.atStartOfDay();
        var dayEnd   = date.atTime(23, 59, 59);
        return jpa.existsByEmployeeIdAndDate(employeeId, dayStart, dayEnd);
    }
}
