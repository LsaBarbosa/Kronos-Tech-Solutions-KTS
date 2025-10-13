package com.kts.kronos.application.port.in.usecase;

import com.kts.kronos.adapter.in.web.dto.timerecord.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimeRecordUseCase {
    void registerTime(GeolocationRequest request);
    void registerBreak(GeolocationRequest request);
    void updateStatus(UUID employeeId, Long recordId,UpdateTimeRecordStatusRequest request);

    void deleteTimeRecord(UUID employeeId, Long recordId);

    void toggleActivate(UUID employeeId, Long timeRecordId);

    SimpleReportResponse simpleReport(UUID employeeId, SimpleReportRequest req);

    byte[] simpleReportPDF(UUID employeeId, SimpleReportResponse report);

    void updateTimeRecord(Long timeRecordId,UpdateTimeRecordRequest req);

    List<TimeRecordResponse> listReport(UUID employeeId, ListReportRequest req);

    byte[] listReportPDF(List<TimeRecordResponse> records);
    List<TimeRecordApprovalResponse> listPendingApprovals();
    void approveTimeRecordChange(Long timeRecordId);
    void rejectTimeRecordChange(Long timeRecordId);
}

