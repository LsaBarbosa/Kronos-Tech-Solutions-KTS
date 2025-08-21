package com.kts.kronos.adapter.in.web.http;


import com.kts.kronos.adapter.in.web.dto.timerecord.TimeRecordResponse;
import com.kts.kronos.adapter.in.web.dto.timerecord.UpdateTimeRecordRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.UpdateTimeRecordStatusRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.ListReportRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.SimpleReportRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.SimpleReportResponse;
import com.kts.kronos.application.port.in.usecase.TimeRecordUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ContentDisposition;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.kts.kronos.constants.ApiPaths.RECORDS;
import static com.kts.kronos.constants.ApiPaths.CHECKIN;
import static com.kts.kronos.constants.ApiPaths.CHECKOUT;
import static com.kts.kronos.constants.ApiPaths.UPDATE_TIME_RECORD;
import static com.kts.kronos.constants.ApiPaths.UPDATE_STATUS;
import static com.kts.kronos.constants.ApiPaths.TOGGLE_ACTIVATE_RECORD;
import static com.kts.kronos.constants.ApiPaths.DELETE_RECORD;
import static com.kts.kronos.constants.ApiPaths.REPORT;
import static com.kts.kronos.constants.ApiPaths.SIMPLE_REPORT;
import static com.kts.kronos.constants.ApiPaths.REPORT_PDF;
import static com.kts.kronos.constants.ApiPaths.REPORT_SIMPLE_PDF;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(RECORDS)
@RequiredArgsConstructor
public class TimeRecordController {
    private final TimeRecordUseCase useCase;

    @PreAuthorize("hasAnyRole('MANAGER', 'PARTNER')")
    @PostMapping(CHECKIN)
    @ResponseStatus(HttpStatus.CREATED)
    public void checkin() {
        useCase.checkin();
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'PARTNER')")
    @PostMapping(CHECKOUT)
    @ResponseStatus(HttpStatus.CREATED)
    public void checkout() {
        useCase.checkout();
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'PARTNER')")
    @PutMapping(UPDATE_TIME_RECORD)
    @ResponseStatus(HttpStatus.OK)
    public void updateTimeRecord(@PathVariable Long timeRecordId, @Valid @RequestBody UpdateTimeRecordRequest req) {
        useCase.updateTimeRecord(timeRecordId, req);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping(UPDATE_STATUS)
    @ResponseStatus(HttpStatus.OK)
    public void updateStatus(@PathVariable UUID employeeId, @PathVariable Long timeRecordId, @Valid @RequestBody UpdateTimeRecordStatusRequest req) {
        useCase.updateStatus(employeeId, timeRecordId, req);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping(TOGGLE_ACTIVATE_RECORD)
    @ResponseStatus(HttpStatus.OK)
    public void toggleActivate(@PathVariable UUID employeeId, @PathVariable Long timeRecordId) {
        useCase.toggleActivate(employeeId, timeRecordId);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @DeleteMapping(DELETE_RECORD)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTimeRecord(@PathVariable UUID employeeId, @PathVariable Long timeRecordId) {
        useCase.deleteTimeRecord(employeeId, timeRecordId);
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'PARTNER')")
    @GetMapping(REPORT)
    public List<TimeRecordResponse> report(@RequestParam(required = false) UUID employeeId, @Valid @RequestBody ListReportRequest req) {
        return useCase.listReport(employeeId, req);
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'PARTNER')")
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

    @PreAuthorize("hasAnyRole('MANAGER', 'PARTNER')")
    @GetMapping(SIMPLE_REPORT)
    public ResponseEntity<SimpleReportResponse> simpleReport(@RequestParam(required = false) UUID employeeId,
                                                             @Valid @RequestBody SimpleReportRequest req) {
        var resp = useCase.simpleReport(employeeId, req);
        return ResponseEntity.ok(resp);
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'PARTNER')")
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

    @PreAuthorize("hasRole('MANAGER')")
    @PatchMapping("/approve/{timeRecordId}")
    @ResponseStatus(HttpStatus.OK)
    public void approveChange(@PathVariable Long timeRecordId) {
        useCase.approveTimeRecordChange(timeRecordId);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PatchMapping("/reject/{timeRecordId}")
    @ResponseStatus(HttpStatus.OK)
    public void rejectChange(@PathVariable Long timeRecordId) {
        useCase.rejectTimeRecordChange(timeRecordId);
    }

}
