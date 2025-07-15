package com.kts.kronos.application.port.in.usecase;
import com.kts.kronos.adapter.in.web.dto.timerecord.*;
import com.kts.kronos.domain.model.StatusRecord;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TimeRecordUseCase {
    void checkin(CreateTimeRecordRequest request);
    void checkout(CreateTimeRecordRequest request);
    void updateStatus(UpdateTimeRecordStatusRequest request);
    void toggleActivate(ToggleActivate toggleActivate);
    void deleteTimeRecord(DeleteTimeRecordRequest req);
    SimpleReportResponse simpleReport(SimpleReportRequest req);
    byte[] simpleReportPDF(SimpleReportResponse report) throws IOException;
    TimeRecordResponse updateTimeRecord(UpdateTimeRecordRequest req);
    List<TimeRecordResponse> listReport(UUID employeeId,
                                        String reference,
                                        Boolean active,
                                        StatusRecord status,
                                        LocalDate[] dates);
}

