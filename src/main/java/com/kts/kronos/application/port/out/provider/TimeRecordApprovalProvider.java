package com.kts.kronos.application.port.out.provider;
import com.kts.kronos.domain.model.TimeRecordApprovalRequest;

import java.util.List;
import java.util.Optional;
public interface TimeRecordApprovalProvider {
    void save(TimeRecordApprovalRequest request);
    Optional<TimeRecordApprovalRequest> findByTimeRecordId(Long timeRecordId);
    List<TimeRecordApprovalRequest> findAll();
    void deleteByTimeRecordId(Long timeRecordId);
}
