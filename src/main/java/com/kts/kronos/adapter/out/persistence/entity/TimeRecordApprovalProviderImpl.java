package com.kts.kronos.adapter.out.persistence.entity;


import com.kts.kronos.adapter.out.persistence.TimeRecordApprovalRepository;
import com.kts.kronos.adapter.out.persistence.entity.TimeRecordApprovalEntity;
import com.kts.kronos.application.port.out.provider.TimeRecordApprovalProvider;
import com.kts.kronos.domain.model.TimeRecordApprovalRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TimeRecordApprovalProviderImpl  implements TimeRecordApprovalProvider {

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
    public List<TimeRecordApprovalRequest> findAll() {
        return repository.findAll().stream()
                .map(TimeRecordApprovalEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByTimeRecordId(Long timeRecordId) {
        repository.deleteById(timeRecordId);
    }
}