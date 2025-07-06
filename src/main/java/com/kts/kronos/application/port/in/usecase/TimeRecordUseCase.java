package com.kts.kronos.application.port.in.usecase;

import com.kts.kronos.adapter.in.web.dto.timerecord.CreateTimeRecordRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.TimeRecordResponse;
import com.kts.kronos.adapter.in.web.dto.timerecord.UpdateTimeRecordRequest;
import com.kts.kronos.domain.model.StatusRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TimeRecordUseCase {
    void checkin(CreateTimeRecordRequest request);
    void checkout(CreateTimeRecordRequest request);
    TimeRecordResponse updateTimeRecord(UpdateTimeRecordRequest req);
    List<TimeRecordResponse> listReport(UUID employeeId,
                                        String reference,
                                        Boolean active,
                                        StatusRecord status,
                                        LocalDate[] dates);
}

