package com.kts.kronos.application.port.in.usecase;

import java.util.List;
import java.util.UUID;

import com.kts.kronos.adapter.in.web.dto.timerecord.ActionResponse;
import com.kts.kronos.adapter.in.web.dto.timerecord.GeolocationRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.ListReportRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.RequestVacationRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.SimpleReportRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.SimpleReportResponse;
import com.kts.kronos.adapter.in.web.dto.timerecord.TimeRecordApprovalPageResponse;
import com.kts.kronos.adapter.in.web.dto.timerecord.TimeRecordResponse;
import com.kts.kronos.adapter.in.web.dto.timerecord.UpdateTimeRecordRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.UpdateTimeRecordStatusRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.VacationApprovalRequest;

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
    List<Long> requestVacation(RequestVacationRequest request);
    void approveVacation(VacationApprovalRequest request);
    void rejectVacation(VacationApprovalRequest request);
}

