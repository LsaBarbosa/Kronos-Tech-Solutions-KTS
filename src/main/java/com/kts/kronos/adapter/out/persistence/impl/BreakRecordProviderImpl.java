package com.kts.kronos.adapter.out.persistence.impl;
import com.kts.kronos.adapter.out.persistence.BreakRecordRepository;
import com.kts.kronos.adapter.out.persistence.entity.BreakRecordEntity;
import com.kts.kronos.application.port.out.provider.BreakRecordProvider;
import com.kts.kronos.domain.model.BreakRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class BreakRecordProviderImpl implements BreakRecordProvider {
    private final BreakRecordRepository repository;

    @Override
    public BreakRecord save(BreakRecord breakRecord) {
        var entity = BreakRecordEntity.fromDomain(breakRecord);
        var saved = repository.save(entity);
        return saved.toDomain().withId(saved.getBreakRecordId());
    }

    @Override
    public Optional<BreakRecord> findOpenBreakByTimeRecordId(Long timeRecordId) {
        return repository.findFirstByTimeRecordIdAndEndBreakIsNullAndActiveTrueOrderByStartBreakDesc(timeRecordId)
                .map(BreakRecordEntity::toDomain);
    }

    @Override
    public List<BreakRecord> findByTimeRecordId(Long timeRecordId) {
        return repository.findByTimeRecordId(timeRecordId)
                .stream()
                .map(BreakRecordEntity::toDomain)
                .toList();
    }
}