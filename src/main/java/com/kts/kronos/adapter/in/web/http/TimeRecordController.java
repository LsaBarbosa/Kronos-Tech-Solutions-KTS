package com.kts.kronos.adapter.in.web.http;


import com.kts.kronos.adapter.in.web.dto.timerecord.*;
import com.kts.kronos.adapter.in.web.dto.timerecord.vacation.RequestVacationRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.vacation.VacationApprovalRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.vacation.VacationRequestResponse;
import com.kts.kronos.application.port.in.usecase.TimeRecordUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

import static com.kts.kronos.constants.ApiPaths.*;
import static com.kts.kronos.constants.ApiPaths.CHECKIN;
import static com.kts.kronos.constants.ApiPaths.TIME_OFF_REQUEST;
import static com.kts.kronos.constants.Messages.*;
import static com.kts.kronos.constants.Swagger.*;


@RestController
@RequestMapping(RECORDS)
@RequiredArgsConstructor
@Tag(name = SWAGGER_TR_TAG, description = SWAGGER_TR_DESC)
public class TimeRecordController {
    private final TimeRecordUseCase useCase;

    @PreAuthorize(ANY_EMPLOYEE)
    @PostMapping(CHECKIN)
    @Operation(summary = REG_TIME_SUMMARY, description = REG_TIME_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = REG_TIME_200),
            @ApiResponse(responseCode = "400", description = REG_TIME_400),
            @ApiResponse(responseCode = "404", description = COMPANY_OR_EMPLOYEE_NOT_FOUND),
            @ApiResponse(responseCode = "500", description = REG_TIME_500)
    })
    public ResponseEntity<ActionResponse> registerTime(@Valid @RequestBody GeolocationRequest request) {
        var response = useCase.registerTime(request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize(ANY_EMPLOYEE)
    @PutMapping(UPDATE_TIME_RECORD)
    @Operation(summary = UPDATE_TR_SUMMARY, description = UPDATE_TR_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = UPDATE_TR_200),
            @ApiResponse(responseCode = "400", description = UPDATE_TR_400),
            @ApiResponse(responseCode = "403", description = ACCESS_DENIED),
            @ApiResponse(responseCode = "404", description = RECORD_NOT_FOUND)
    })
    public void updateTimeRecord(@PathVariable Long timeRecordId, @Valid @RequestBody UpdateTimeRecordRequest req) {
        useCase.updateTimeRecord(timeRecordId, req);
    }

    @PreAuthorize(MANAGER)
    @PutMapping(UPDATE_STATUS)
    @Operation(summary = UPDATE_STATUS_SUMMARY, description = UPDATE_STATUS_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = UPDATE_STATUS_200),
            @ApiResponse(responseCode = "400", description = UPDATE_STATUS_400),
            @ApiResponse(responseCode = "404", description = RECORD_NOT_FOUND)
    })
    public void updateStatus(@PathVariable UUID employeeId, @PathVariable Long timeRecordId, @Valid @RequestBody UpdateTimeRecordStatusRequest req) {
        useCase.updateStatus(employeeId, timeRecordId, req);
    }

    @PreAuthorize(MANAGER)
    @PutMapping(TOGGLE_ACTIVATE_RECORD)
    @Operation(summary = TOGGLE_TR_SUMMARY, description = TOGGLE_TR_DESC)
    public void toggleActivate(@PathVariable UUID employeeId, @PathVariable Long timeRecordId) {
        useCase.toggleActivate(employeeId, timeRecordId);
    }

    @PreAuthorize(MANAGER)
    @DeleteMapping(DELETE_RECORD)
    @Operation(summary = DEL_TR_SUMMARY, description = DEL_TR_DESC)
    public void deleteTimeRecord(@PathVariable UUID employeeId, @PathVariable Long timeRecordId) {
        useCase.deleteTimeRecord(employeeId, timeRecordId);
    }

    @PreAuthorize(ANY_EMPLOYEE)
    @PostMapping(REPORT)
    @Operation(summary = REPORT_SUMMARY, description = REPORT_DESC)
    public List<TimeRecordResponse> report(@RequestParam(required = false) UUID employeeId,
                                           @Valid @RequestBody ListReportRequest req) {
        return useCase.listReport(employeeId, req);
    }

    @PreAuthorize(ANY_EMPLOYEE)
    @PostMapping(SIMPLE_REPORT)
    @Operation(summary = SIMPLE_REPORT_SUMMARY, description = SIMPLE_REPORT_DESC)
    public ResponseEntity<SimpleReportResponse> simpleReport(@RequestParam(required = false) UUID employeeId,
                                                             @Valid @RequestBody SimpleReportRequest req) {
        var resp = useCase.simpleReport(employeeId, req);
        return ResponseEntity.ok(resp);
    }

    @PreAuthorize(MANAGER)
    @PatchMapping(APPROVE_UPDATE)
    @Operation(summary = APPROVE_CHANGE_SUMMARY, description = APPROVE_CHANGE_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = APPROVE_CHANGE_200),
            @ApiResponse(responseCode = "400", description = APPROVE_CHANGE_400),
            @ApiResponse(responseCode = "404", description = APPROVE_CHANGE_404)
    })
    public void approveChange(@PathVariable Long timeRecordId) {
        useCase.approveTimeRecordChange(timeRecordId);
    }

    @PreAuthorize(MANAGER)
    @PatchMapping(REJECT_UPDATE)
    @Operation(summary = REJECT_CHANGE_SUMMARY, description = REJECT_CHANGE_DESC)
    public void rejectChange(@PathVariable Long timeRecordId) {
        useCase.rejectTimeRecordChange(timeRecordId);
    }

    @PreAuthorize(MANAGER)
    @GetMapping(PENDING_APPROVALS)
    @Operation(summary = LIST_PENDING_SUMMARY, description = LIST_PENDING_DESC)
    public ResponseEntity<TimeRecordApprovalPageResponse> listPendingApprovals(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "employeeName", required = false) String employeeName) {

        final int SIZE = 5;
        var approvals = useCase.listPendingApprovals(page, SIZE, employeeName);
        return ResponseEntity.ok(approvals);
    }

    @PreAuthorize(ANY_EMPLOYEE)
    @PostMapping(VACATION_REQUEST)
    @Operation(summary = REQ_VACATION_SUMMARY, description = REQ_VACATION_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = REQ_VACATION_201),
            @ApiResponse(responseCode = "400", description = REQ_VACATION_400),
            @ApiResponse(responseCode = "403", description = REQ_VACATION_403)
    })
    public ResponseEntity<List<Long>> requestVacation(@Valid @RequestBody RequestVacationRequest request) {
        var createdIds = useCase.requestVacation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdIds);
    }

    @PreAuthorize(MANAGER)
    @PatchMapping(VACATION_APPROVE)
    @Operation(summary = APPROVE_VACATION_SUMMARY, description = APPROVE_VACATION_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = APPROVE_VACATION_204),
            @ApiResponse(responseCode = "403", description = APPROVE_VACATION_403)
    })
    public ResponseEntity<Void> approveVacation(@Valid @RequestBody VacationApprovalRequest request) {
        useCase.approveVacation(request);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize(MANAGER)
    @PatchMapping(VACATION_REJECT)
    @Operation(summary = REJECT_VACATION_SUMMARY)
    public ResponseEntity<Void> rejectVacation(@Valid @RequestBody VacationApprovalRequest request) {
        useCase.rejectVacation(request);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize(MANAGER)
    @GetMapping(VACATION_REQUEST)
    @Operation(summary = LIST_VACATION_SUMMARY, description = LIST_VACATION_DESC)
    public ResponseEntity<List<VacationRequestResponse>> listVacationRequests(
            @RequestParam(value = "status", defaultValue = "PENDING") String statusFilter,
            @RequestParam(value = "employeeName", required = false) String employeeName,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        var requests = useCase.listVacationRequests(statusFilter, employeeName, page, size);
        return ResponseEntity.ok(requests);
    }

    @PreAuthorize(ANY_EMPLOYEE)
    @PostMapping(path = TIME_OFF_REQUEST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = REQ_TIMEOFF_SUMMARY, description = REQ_TIMEOFF_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = REQ_TIMEOFF_201),
            @ApiResponse(responseCode = "400", description = REQ_TIMEOFF_400),
            @ApiResponse(responseCode = "404", description = MANAGER_NOT_FOUND)
    })
    public ResponseEntity<Long> requestTimeOff(
            @RequestPart("request") @Valid RequestTimeOffRequest request,
            @RequestPart(value = "document", required = false) MultipartFile document) {
        
        var createdId = useCase.requestTimeOff(request, document);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdId);
    }

    @PreAuthorize(MANAGER)
    @PatchMapping(TIME_OFF_APPROVE)
    @Operation(summary = APPROVE_TIMEOFF_SUMMARY, description = APPROVE_TIMEOFF_DESC)
    public ResponseEntity<Void> approveTimeOff(@PathVariable Long timeRecordId) {
        useCase.approveTimeOff(timeRecordId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize(MANAGER)
    @PatchMapping(TIME_OFF_REJECT)
    @Operation(summary = REJECT_TIMEOFF_SUMMARY)
    public ResponseEntity<Void> rejectTimeOff(@PathVariable Long timeRecordId) {
        useCase.rejectTimeOff(timeRecordId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize(MANAGER)
    @GetMapping(TIME_OFF_REQUESTS)
    @Operation(summary = LIST_TIMEOFF_SUMMARY)
    public ResponseEntity<TimeRecordPageResponse> listTimeOffRequests(
            @RequestParam(value = "status", defaultValue = "PENDING") String statusFilter,
            @RequestParam(value = "employeeName", required = false) String employeeName,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size) {
        var requests = useCase.listTimeOffRequests(statusFilter, employeeName, page, size);
        return ResponseEntity.ok(requests);
    }
}
