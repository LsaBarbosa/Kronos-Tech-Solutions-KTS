package com.kts.kronos.application.port.in.usecase;

import java.util.List;
import java.util.UUID;

import com.kts.kronos.adapter.in.web.dto.timerecord.*;
import com.kts.kronos.adapter.in.web.dto.timerecord.vacation.RequestVacationRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.vacation.VacationApprovalRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.vacation.VacationRequestResponse;
import org.springframework.web.multipart.MultipartFile;

public interface TimeRecordUseCase {
    ActionResponse registerTime(GeolocationRequest request, MultipartFile faceImage);
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
    List<VacationRequestResponse> listVacationRequests(String statusFilter, String employeeName, int page, int size);
    Long requestTimeOff(RequestTimeOffRequest request, MultipartFile document);
    void approveTimeOff(Long timeRecordId);
    void rejectTimeOff(Long timeRecordId);
    TimeRecordPageResponse listTimeOffRequests(String statusFilter, String employeeName, int page, int size);
}

