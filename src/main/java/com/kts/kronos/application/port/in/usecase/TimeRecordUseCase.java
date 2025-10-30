package com.kts.kronos.application.port.in.usecase;

import com.kts.kronos.adapter.in.web.dto.timerecord.*;

import java.util.List;
import java.util.UUID;

public interface TimeRecordUseCase {
    ActionResponse registerTime(GeolocationRequest request);
    void updateStatus(UUID employeeId, Long recordId, UpdateTimeRecordStatusRequest request);
    void deleteTimeRecord(UUID employeeId, Long recordId);
    void toggleActivate(UUID employeeId, Long timeRecordId);
    SimpleReportResponse simpleReport(UUID employeeId, SimpleReportRequest req);
    void updateTimeRecord(Long timeRecordId, UpdateTimeRecordRequest req);
    List<TimeRecordResponse> listReport(UUID employeeId, ListReportRequest req);
    TimeRecordApprovalPageResponse listPendingApprovals(int page, int size, String employeeName);
    void approveTimeRecordChange(Long timeRecordId);
    void rejectTimeRecordChange(Long timeRecordId);
}

