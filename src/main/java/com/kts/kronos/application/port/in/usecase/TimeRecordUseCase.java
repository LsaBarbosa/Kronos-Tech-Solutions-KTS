package com.kts.kronos.application.port.in.usecase;

import com.kts.kronos.adapter.in.web.dto.timerecord.*;

import java.util.List;
import java.util.UUID;

public interface TimeRecordUseCase {
    void checkin(UUID employeeId);

    void checkout(UUID employeeId);

    void updateStatus(UUID employeeId, Long recordId,UpdateTimeRecordStatusRequest request);

    void deleteTimeRecord(UUID employeeId, Long recordId);

    void toggleActivate(UUID employeeId, Long timeRecordId);

    SimpleReportResponse simpleReport(UUID employeeId, SimpleReportRequest req);

    byte[] simpleReportPDF(UUID employeeId, SimpleReportResponse report);

    TimeRecordResponse updateTimeRecord(UUID employeeId, Long timeRecordId,UpdateTimeRecordRequest req);

    List<TimeRecordResponse> listReport(UUID employeeId, ListReportRequest req);

    byte[] listReportPDF(List<TimeRecordResponse> records);
}

