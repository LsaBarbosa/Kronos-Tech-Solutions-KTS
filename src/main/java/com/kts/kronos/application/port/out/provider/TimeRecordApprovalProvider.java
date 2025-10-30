package com.kts.kronos.application.port.out.provider;
import com.kts.kronos.domain.model.TimeRecordApprovalRequest;

 import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TimeRecordApprovalProvider {
    void save(TimeRecordApprovalRequest request);
    Optional<TimeRecordApprovalRequest> findByTimeRecordId(Long timeRecordId);
    Page<TimeRecordApprovalRequest> findAll(Pageable pageable, String employeeName);
    void deleteByTimeRecordId(Long timeRecordId);
}
