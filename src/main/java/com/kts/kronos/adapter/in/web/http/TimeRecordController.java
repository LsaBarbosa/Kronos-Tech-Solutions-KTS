package com.kts.kronos.adapter.in.web.http;


import com.kts.kronos.adapter.in.web.dto.timerecord.*;
import com.kts.kronos.application.port.in.usecase.TimeRecordUseCase;
import com.kts.kronos.domain.model.StatusRecord;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/records")
@RequiredArgsConstructor
public class TimeRecordController {
    private final TimeRecordUseCase useCase;

    @PostMapping("/checkin")
    @ResponseStatus(HttpStatus.CREATED)
    public void checkin(@Valid @RequestBody CreateTimeRecordRequest request) {
        useCase.checkin(request);
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public void checkout(@Valid @RequestBody CreateTimeRecordRequest request) {
        useCase.checkout(request);
    }

    @PutMapping("/update/time-record")
    public ResponseEntity<TimeRecordResponse> updateTimeRecord(
            @Valid @RequestBody UpdateTimeRecordRequest req
    ) {
        var updated = useCase.updateTimeRecord(req);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/update/status")
    public void updateStatus(
            @Valid @RequestBody UpdateTimeRecordStatusRequest req
    ) {
        useCase.updateStatus(req);
    }

    @PutMapping("/update/toggle-activate")
    public void deactivate(
            @Valid @RequestBody ToggleActivate toggleActivate
    ) {
        useCase.toggleActivate(toggleActivate);
    }


    @GetMapping("/report/total/{employeeId}")
    public List<TimeRecordResponse> report(
            @PathVariable UUID employeeId,
            @RequestParam String reference,
            @RequestParam(value = "active", required = false) Boolean active,
            @RequestParam(required = false) StatusRecord status,
            @RequestParam(name = "dates", required = false)
            @DateTimeFormat(pattern = "dd-MM-yyyy")
            LocalDate[] dates
    ) {
        return useCase.listReport(employeeId, reference, active, status, dates);
    }

    @GetMapping("/report/simple")
    public ResponseEntity<SimpleReportResponse> simpleReport(
            @Valid @RequestBody SimpleReportRequest request
    ) {
        var resp = useCase.simpleReport(request);
        return ResponseEntity.ok(resp);
    }
    @GetMapping("report/simple/pdf")
    public ResponseEntity<byte[]> downloadSimpleReportPdf(
            @Valid @RequestBody SimpleReportRequest req) throws IOException {

        var resp = useCase.simpleReport(req);
        byte[] pdfBytes = useCase.simpleReportPDF(resp);

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("simple-report.pdf")
                        .build()
        );

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }


    @DeleteMapping("records/{employeeId}/{timeRecordId}")
    public ResponseEntity<Void> deleteTimeRecord(
            @PathVariable UUID employeeId,
            @PathVariable Long timeRecordId
    ) {
        var req = new DeleteTimeRecordRequest(employeeId, timeRecordId);
        useCase.deleteTimeRecord(req);
        return ResponseEntity.noContent().build();
    }


}
