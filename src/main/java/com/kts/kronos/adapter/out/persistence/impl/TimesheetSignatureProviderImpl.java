package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.TimesheetSignatureRepository;
import com.kts.kronos.adapter.out.persistence.entity.TimesheetSignatureEntity;
import com.kts.kronos.adapter.out.persistence.mapper.TimesheetSignatureMapper;
import com.kts.kronos.application.port.out.provider.TimesheetSignatureProvider;
import com.kts.kronos.domain.model.TimesheetSignature;
import com.kts.kronos.domain.model.enuns.TimesheetSignatureStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TimesheetSignatureProviderImpl implements TimesheetSignatureProvider {

    private final TimesheetSignatureRepository repository;

    @Override
    public TimesheetSignature save(TimesheetSignature signature) {
        TimesheetSignatureEntity entity = TimesheetSignatureMapper.toEntity(signature);
        TimesheetSignatureEntity saved = repository.save(entity);
        return TimesheetSignatureMapper.toDomain(saved);
    }

    @Override
    public Optional<TimesheetSignature> findById(UUID signatureId) {
        return repository.findById(signatureId).map(TimesheetSignatureMapper::toDomain);
    }

    @Override
    public Optional<TimesheetSignature> findActiveByEmployeeAndPeriod(
            UUID employeeId,
            int referenceYear,
            int referenceMonth
    ) {
        return repository.findByEmployeeIdAndReferenceYearAndReferenceMonthAndStatus(
                employeeId, referenceYear, referenceMonth, TimesheetSignatureStatus.ACTIVE
        ).map(TimesheetSignatureMapper::toDomain);
    }

    @Override
    public Page<TimesheetSignature> findAdminFiltered(
            Pageable pageable,
            UUID companyId,
            Integer year,
            Integer month,
            TimesheetSignatureStatus status,
            Collection<UUID> employeeIds
    ) {
        Collection<UUID> filterEmployeeIds = (employeeIds == null || employeeIds.isEmpty()) ? null : employeeIds;
        return repository.findAdminFiltered(pageable, companyId, year, month, status, filterEmployeeIds)
                .map(TimesheetSignatureMapper::toDomain);
    }
}
