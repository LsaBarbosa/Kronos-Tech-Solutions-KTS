package com.kts.kronos.adapter.in.web.http;


import com.kts.kronos.adapter.in.web.dto.timerecord.CreateTimeRecordRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.TimeRecordResponse;
import com.kts.kronos.adapter.in.web.dto.timerecord.UpdateTimeRecordRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.UpdateTimeRecordStatusRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.ToggleActivate;
import com.kts.kronos.adapter.in.web.dto.timerecord.DeleteTimeRecordRequest;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping(CHECKIN)
    @ResponseStatus(HttpStatus.CREATED)
    public void checkin(@Valid @RequestBody CreateTimeRecordRequest req) {
        useCase.checkin(req);
    }

    @PostMapping(CHECKOUT)
    @ResponseStatus(HttpStatus.CREATED)
    public void checkout(@Valid @RequestBody CreateTimeRecordRequest req) {
        useCase.checkout(req);
    }

    @PutMapping(UPDATE_TIME_RECORD)
    public ResponseEntity<TimeRecordResponse> updateTimeRecord(@Valid @RequestBody UpdateTimeRecordRequest req) {
        var updated = useCase.updateTimeRecord(req);
        return ResponseEntity.ok(updated);
    }

    @PutMapping(UPDATE_STATUS)
    public void updateStatus(@Valid @RequestBody UpdateTimeRecordStatusRequest req) {
        useCase.updateStatus(req);
    }

    @PutMapping(TOGGLE_ACTIVATE_RECORD)
    public void toggleActivate(@PathVariable Long timeRecordId, @Valid @RequestBody ToggleActivate toggleActivate) {
        useCase.toggleActivate(toggleActivate,timeRecordId );
    }

    @DeleteMapping(DELETE_RECORD)
    public ResponseEntity<Void> deleteTimeRecord(@PathVariable UUID employeeId, @PathVariable Long timeRecordId) {
        var req = new DeleteTimeRecordRequest(employeeId, timeRecordId);
        useCase.deleteTimeRecord(req);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(REPORT)
    public List<TimeRecordResponse> report(@PathVariable UUID employeeId, @Valid @RequestBody ListReportRequest req) {
        return useCase.listReport(employeeId, req);
    }

    @GetMapping(REPORT_PDF)
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

    @GetMapping(SIMPLE_REPORT)
    public ResponseEntity<SimpleReportResponse> simpleReport(@PathVariable UUID employeeId,
                                                             @Valid @RequestBody SimpleReportRequest req) {
        var resp = useCase.simpleReport(employeeId, req);
        return ResponseEntity.ok(resp);
    }

    @GetMapping(REPORT_SIMPLE_PDF)
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
