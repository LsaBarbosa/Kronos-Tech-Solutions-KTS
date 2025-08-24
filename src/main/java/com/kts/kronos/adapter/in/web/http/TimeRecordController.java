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
    @ResponseStatus(HttpStatus.CREATED)
    public void checkin() {
        useCase.checkin();
    }

    @PreAuthorize(ANY_EMPLOYEE)
    @PostMapping(CHECKOUT)
    @ResponseStatus(HttpStatus.CREATED)
    public void checkout() {
        useCase.checkout();
    }

    @PreAuthorize(ANY_EMPLOYEE)
    @PutMapping(UPDATE_TIME_RECORD)
    @ResponseStatus(HttpStatus.OK)
    public void updateTimeRecord(@PathVariable Long timeRecordId, @Valid @RequestBody UpdateTimeRecordRequest req) {
        useCase.updateTimeRecord(timeRecordId, req);
    }

    @PreAuthorize(MANAGER)
    @PutMapping(UPDATE_STATUS)
    @ResponseStatus(HttpStatus.OK)
    public void updateStatus(@PathVariable UUID employeeId, @PathVariable Long timeRecordId, @Valid @RequestBody UpdateTimeRecordStatusRequest req) {
        useCase.updateStatus(employeeId, timeRecordId, req);
    }

    @PreAuthorize(MANAGER)
    @PutMapping(TOGGLE_ACTIVATE_RECORD)
    @ResponseStatus(HttpStatus.OK)
    public void toggleActivate(@PathVariable UUID employeeId, @PathVariable Long timeRecordId) {
        useCase.toggleActivate(employeeId, timeRecordId);
    }

    @PreAuthorize(MANAGER)
    @DeleteMapping(DELETE_RECORD)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTimeRecord(@PathVariable UUID employeeId, @PathVariable Long timeRecordId) {
        useCase.deleteTimeRecord(employeeId, timeRecordId);
    }

    @PreAuthorize(ANY_EMPLOYEE)
    @GetMapping(REPORT)
    public List<TimeRecordResponse> report(@RequestParam(required = false) UUID employeeId, @Valid @RequestBody ListReportRequest req) {
        return useCase.listReport(employeeId, req);
    }

    @PreAuthorize(ANY_EMPLOYEE)
    @GetMapping(REPORT_PDF)
    public ResponseEntity<byte[]> reportPdf(@RequestParam(required = false) UUID employeeId, @Valid @RequestBody ListReportRequest req) {

        var records = useCase.listReport(employeeId, req);
        byte[] pdf = useCase.listReportPDF(records);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("detailed-report.pdf")
                        .build()
        );

        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    @PreAuthorize(ANY_EMPLOYEE)
    @GetMapping(SIMPLE_REPORT)
    public ResponseEntity<SimpleReportResponse> simpleReport(@RequestParam(required = false) UUID employeeId,
                                                             @Valid @RequestBody SimpleReportRequest req) {
        var resp = useCase.simpleReport(employeeId, req);
        return ResponseEntity.ok(resp);
    }

    @PreAuthorize(ANY_EMPLOYEE)
    @GetMapping(REPORT_SIMPLE_PDF)
    public ResponseEntity<byte[]> simpleReportPdf(@RequestParam(required = false) UUID employeeId,
                                                  @Valid @RequestBody SimpleReportRequest req) {

        var resp = useCase.simpleReport(employeeId, req);
        byte[] pdfBytes = useCase.simpleReportPDF(employeeId, resp);

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("simple-report.pdf")
                        .build()
        );

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @PreAuthorize(MANAGER)
    @PatchMapping(APPROVE_UPDATE)
    @ResponseStatus(HttpStatus.OK)
    public void approveChange(@PathVariable Long timeRecordId) {
        useCase.approveTimeRecordChange(timeRecordId);
    }

    @PreAuthorize(MANAGER)
    @PatchMapping(REJECT_UPDATE)
    @ResponseStatus(HttpStatus.OK)
    public void rejectChange(@PathVariable Long timeRecordId) {
        useCase.rejectTimeRecordChange(timeRecordId);
    }

}
