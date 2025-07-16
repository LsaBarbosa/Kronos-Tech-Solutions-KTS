package com.kts.kronos.adapter.in.web.http;


import com.kts.kronos.adapter.in.web.dto.timerecord.*;
import com.kts.kronos.application.port.in.usecase.TimeRecordUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/records")
@RequiredArgsConstructor
public class TimeRecordController {
    private final TimeRecordUseCase useCase;

    @PostMapping("/checkin")
    @ResponseStatus(HttpStatus.CREATED)
    public void checkin(@Valid @RequestBody CreateTimeRecordRequest req) {
        useCase.checkin(req);
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public void checkout(@Valid @RequestBody CreateTimeRecordRequest req) {
        useCase.checkout(req);
    }

    @PutMapping("/update/time-record")
    public ResponseEntity<TimeRecordResponse> updateTimeRecord(@Valid @RequestBody UpdateTimeRecordRequest req) {
        var updated = useCase.updateTimeRecord(req);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/update/status")
    public void updateStatus(@Valid @RequestBody UpdateTimeRecordStatusRequest req) {
        useCase.updateStatus(req);
    }

    @PutMapping("/update/toggle-activate")
    public void toggleActivate(@Valid @RequestBody ToggleActivate toggleActivate) {
        useCase.toggleActivate(toggleActivate);
    }

    @DeleteMapping("records/{employeeId}/{timeRecordId}")
    public ResponseEntity<Void> deleteTimeRecord(@PathVariable UUID employeeId, @PathVariable Long timeRecordId) {
        var req = new DeleteTimeRecordRequest(employeeId, timeRecordId);
        useCase.deleteTimeRecord(req);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/report/{employeeId}")
    public List<TimeRecordResponse> report(@PathVariable UUID employeeId, @Valid @RequestBody ListReportRequest req) {
        return useCase.listReport(employeeId, req);
    }

    @GetMapping("report/{employeeId}/pdf")
    public ResponseEntity<byte[]> reportPdf(@PathVariable UUID employeeId, @Valid @RequestBody ListReportRequest req){

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

    @GetMapping("/report/simple/{employeeId}")
    public ResponseEntity<SimpleReportResponse> simpleReport(@PathVariable UUID employeeId,
                                                             @Valid @RequestBody SimpleReportRequest req) {
        var resp = useCase.simpleReport(employeeId, req);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("report/simple/{employeeId}/pdf")
    public ResponseEntity<byte[]> simpleReportPdf(@PathVariable UUID employeeId,
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

}
