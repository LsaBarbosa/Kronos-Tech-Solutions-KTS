package com.kts.kronos.adapter.in.web.http;


import com.kts.kronos.adapter.in.web.dto.timerecord.*;
import com.kts.kronos.application.port.in.usecase.TimeRecordUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.kts.kronos.constants.ApiPaths.*;
import static com.kts.kronos.constants.Messages.ANY_EMPLOYEE;
import static com.kts.kronos.constants.Messages.MANAGER;

@RestController
@RequestMapping(RECORDS)
@RequiredArgsConstructor
public class TimeRecordController {
    private final TimeRecordUseCase useCase;

    @PreAuthorize(ANY_EMPLOYEE)
    @PostMapping(CHECKIN)
    public ResponseEntity<ActionResponse> registerTime(@Valid @RequestBody GeolocationRequest request) {
        var response = useCase.registerTime(request);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize(ANY_EMPLOYEE)
    @PutMapping(UPDATE_TIME_RECORD)
    public void updateTimeRecord(@PathVariable Long timeRecordId, @Valid @RequestBody UpdateTimeRecordRequest req) {
        useCase.updateTimeRecord(timeRecordId, req);
    }

    @PreAuthorize(MANAGER)
    @PutMapping(UPDATE_STATUS)
    public void updateStatus(@PathVariable UUID employeeId, @PathVariable Long timeRecordId, @Valid @RequestBody UpdateTimeRecordStatusRequest req) {
        useCase.updateStatus(employeeId, timeRecordId, req);
    }

    @PreAuthorize(MANAGER)
    @PutMapping(TOGGLE_ACTIVATE_RECORD)
    public void toggleActivate(@PathVariable UUID employeeId, @PathVariable Long timeRecordId) {
        useCase.toggleActivate(employeeId, timeRecordId);
    }

    @PreAuthorize(MANAGER)
    @DeleteMapping(DELETE_RECORD)
    public void deleteTimeRecord(@PathVariable UUID employeeId, @PathVariable Long timeRecordId) {
        useCase.deleteTimeRecord(employeeId, timeRecordId);
    }

    @PreAuthorize(ANY_EMPLOYEE)
    @PostMapping(REPORT)
    public List<TimeRecordResponse> report(@RequestParam(required = false) UUID employeeId, @Valid @RequestBody ListReportRequest req) {
        return useCase.listReport(employeeId, req);
    }

    @PreAuthorize(ANY_EMPLOYEE)
    @PostMapping(SIMPLE_REPORT)
    public ResponseEntity<SimpleReportResponse> simpleReport(@RequestParam(required = false) UUID employeeId,
                                                             @Valid @RequestBody SimpleReportRequest req) {
        var resp = useCase.simpleReport(employeeId, req);
        return ResponseEntity.ok(resp);
    }

    @PreAuthorize(MANAGER)
    @PatchMapping(APPROVE_UPDATE)
    public void approveChange(@PathVariable Long timeRecordId) {
        useCase.approveTimeRecordChange(timeRecordId);
    }

    @PreAuthorize(MANAGER)
    @PatchMapping(REJECT_UPDATE)
    public void rejectChange(@PathVariable Long timeRecordId) {
        useCase.rejectTimeRecordChange(timeRecordId);
    }

    @PreAuthorize(MANAGER)
    @GetMapping(PENDING_APPROVALS)
    public ResponseEntity<TimeRecordApprovalPageResponse> listPendingApprovals(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "employeeName", required = false) String employeeName // Novo filtro
    ) {
        final int SIZE = 5;
        // O valor de 'employeeName' será passado para o UseCase
        var approvals = useCase.listPendingApprovals(page, SIZE, employeeName);
        return ResponseEntity.ok(approvals);
    }

    @PreAuthorize(ANY_EMPLOYEE) // PARTNER/MANAGER/CTO podem solicitar
    @PostMapping(VACATION_REQUEST)
    public ResponseEntity<List<Long>> requestVacation(@Valid @RequestBody RequestVacationRequest request) {
        var createdIds = useCase.requestVacation(request);
        // Retorna 201 Created com a lista de IDs dos TimeRecords criados
        return ResponseEntity.status(HttpStatus.CREATED).body(createdIds);
    }

    @PreAuthorize(MANAGER)
    @PatchMapping(VACATION_APPROVE)
    public ResponseEntity<Void> approveVacation(@Valid @RequestBody VacationApprovalRequest request) {
        useCase.approveVacation(request);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize(MANAGER)
    @PatchMapping(VACATION_REJECT)
    public ResponseEntity<Void> rejectVacation(@Valid @RequestBody VacationApprovalRequest request) {
        useCase.rejectVacation(request);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize(MANAGER)
    @GetMapping(VACATION_REQUEST)
    public ResponseEntity<List<VacationRequestResponse>> listVacationRequests(
            @RequestParam(value = "status", defaultValue = "PENDING") String statusFilter,
            @RequestParam(value = "employeeName", required = false) String employeeName,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        // A role MANAGER é exigida para listar as solicitações
        var requests = useCase.listVacationRequests(statusFilter, employeeName, page, size);
        return ResponseEntity.ok(requests);
    }
}
