package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.ScheduleExceptionRepository;
import com.kts.kronos.adapter.out.persistence.entity.ScheduleExceptionEntity;
import com.kts.kronos.application.port.out.provider.ScheduleExceptionProvider;
import com.kts.kronos.domain.model.ScheduleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ScheduleExceptionProviderImpl implements ScheduleExceptionProvider {

    private final ScheduleExceptionRepository repository;

    @Override
    public Optional<ScheduleException> findByEmployeeAndDate(UUID employeeId, LocalDate date) {
        return repository.findByEmployeeIdAndExceptionDate(employeeId, date)
                .map(ScheduleExceptionEntity::toDomain);
    }

    @Override
    public List<ScheduleException> findByEmployeeAndMonth(UUID employeeId, LocalDate monthStart, LocalDate monthEnd) {
        return repository.findByEmployeeIdAndExceptionDateBetween(employeeId, monthStart, monthEnd)
                .stream()
                .map(ScheduleExceptionEntity::toDomain)
                .toList();
    }

    @Override
    public ScheduleException save(ScheduleException exception) {
        var entity = ScheduleExceptionEntity.fromDomain(exception);
        return repository.save(entity).toDomain();
    }

    @Override
    @Transactional
    public void deleteByEmployeeAndDate(UUID employeeId, LocalDate date) {
        repository.deleteByEmployeeIdAndExceptionDate(employeeId, date);
    }

    @Override
    public boolean existsByEmployeeAndDate(UUID employeeId, LocalDate date) {
        return repository.existsByEmployeeIdAndExceptionDate(employeeId, date);
    }
}
