package com.kts.kronos.application.port.out.provider;
import com.kts.kronos.domain.model.TimeRecordApprovalRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TimeRecordApprovalProvider {
    void save(TimeRecordApprovalRequest request);
    Optional<TimeRecordApprovalRequest> findByTimeRecordId(Long timeRecordId);
     void deleteByTimeRecordId(Long timeRecordId);
    Page<TimeRecordApprovalRequest> findAllByCompanyId(Pageable pageable, String employeeName, UUID companyId);
    List<TimeRecordApprovalRequest> findByRequestingEmployeeId(UUID employeeId, int limit);
}
