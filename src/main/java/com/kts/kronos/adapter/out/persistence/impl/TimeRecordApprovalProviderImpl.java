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
    public Page<TimeRecordApprovalRequest> findAll(Pageable pageable, String employeeName) {
        // Delega para o novo método no Repository
        return repository.findAllPageable(pageable, employeeName)
                .map(TimeRecordApprovalEntity::toDomain);
    }

    @Override
    public void deleteByTimeRecordId(Long timeRecordId) {
        repository.deleteById(timeRecordId);
    }
}