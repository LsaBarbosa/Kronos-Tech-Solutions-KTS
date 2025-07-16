package com.kts.kronos.application.port.in.usecase;
import com.kts.kronos.adapter.in.web.dto.timerecord.*;

import java.util.List;
import java.util.UUID;

public interface TimeRecordUseCase {
    void checkin(CreateTimeRecordRequest request);
    void checkout(CreateTimeRecordRequest request);
    void updateStatus(UpdateTimeRecordStatusRequest request);
    void deleteTimeRecord(DeleteTimeRecordRequest req);
    void toggleActivate(ToggleActivate toggleActivate);
    SimpleReportResponse simpleReport(UUID employeeId,SimpleReportRequest req);
    byte[] simpleReportPDF(UUID employeeId,SimpleReportResponse report);
    TimeRecordResponse updateTimeRecord(UpdateTimeRecordRequest req);
    List<TimeRecordResponse> listReport(UUID employeeId,ListReportRequest req);
    byte[] listReportPDF(List<TimeRecordResponse> records);
}

