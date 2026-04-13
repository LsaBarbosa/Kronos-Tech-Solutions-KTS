package com.kts.kronos.adapter.out.persistence.impl;


import com.kts.kronos.adapter.out.persistence.TimeRecordApprovalRepository;
import com.kts.kronos.adapter.out.persistence.entity.TimeRecordApprovalEntity;
import com.kts.kronos.application.port.out.provider.TimeRecordApprovalProvider;
import com.kts.kronos.domain.model.TimeRecordApprovalRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TimeRecordApprovalProviderImpl implements TimeRecordApprovalProvider {

    private final TimeRecordApprovalRepository repository;

    @Override
    public void save(TimeRecordApprovalRequest request) {
        repository.save(TimeRecordApprovalEntity.fromDomain(request));
    }

    @Override
    public Optional<TimeRecordApprovalRequest> findByTimeRecordId(Long timeRecordId) {
        return repository.findById(timeRecordId)
                .map(TimeRecordApprovalEntity::toDomain);
    }

    @Override
    public Page<TimeRecordApprovalRequest> findAllByCompanyId(Pageable pageable, String employeeName, UUID companyId) {
        String searchName = null;

        if (employeeName != null && !employeeName.isBlank()) {
            searchName = employeeName.trim().toLowerCase();
        }

        return repository.findAllByCompanyId(pageable, companyId, searchName)
                .map(TimeRecordApprovalEntity::toDomain);
    }

    @Override
    public void deleteByTimeRecordId(Long timeRecordId) {
        repository.deleteById(timeRecordId);
    }
}