package com.kts.kronos.adapter.in.web.http;


import com.kts.kronos.adapter.in.web.dto.timerecord.TimeRecordResponse;
import com.kts.kronos.adapter.in.web.dto.timerecord.UpdateTimeRecordRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.UpdateTimeRecordStatusRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.ListReportRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.SimpleReportRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.SimpleReportResponse;
import com.kts.kronos.application.port.in.usecase.TimeRecordUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import static com.kts.kronos.constants.Swagger.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(RECORDS)
@RequiredArgsConstructor
@Tag(name = TIMERECORD_API, description = TIMERECORD_DESCRIPTION_API)
public class TimeRecordController {
    private final TimeRecordUseCase useCase;

    @PostMapping(CHECKIN)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = CREATE, description = CHECKIN_DESCRIPTION)
    public void checkin(@PathVariable UUID employeeId) {
        useCase.checkin(employeeId);
    }

    @PostMapping(CHECKOUT)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = CREATE, description = CHECKOUT_DESCRIPTION)
    public void checkout(@PathVariable UUID employeeId) {
        useCase.checkout(employeeId);
    }

    @PutMapping(UPDATE_TIME_RECORD)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = UPDATE, description = UPDATE_RECORD_DESCRIPTION)
    public void updateTimeRecord(@PathVariable UUID employeeId,
                                 @PathVariable Long timeRecordId,
                                 @Valid @RequestBody UpdateTimeRecordRequest req) {
        useCase.updateTimeRecord(employeeId, timeRecordId, req);
    }

    @PutMapping(UPDATE_STATUS)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = UPDATE, description = UPDATE_STATUS_DESCRIPTION)
    public void updateStatus(@PathVariable UUID employeeId, @PathVariable Long timeRecordId,
                             @Valid @RequestBody UpdateTimeRecordStatusRequest req) {
        useCase.updateStatus(employeeId, timeRecordId, req);
    }

    @PutMapping(TOGGLE_ACTIVATE_RECORD)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = TOGGLE, description = TOGGLE_DESCRIPTION)
    public void toggleActivate(@PathVariable UUID employeeId, @PathVariable Long timeRecordId) {
        useCase.toggleActivate(employeeId, timeRecordId);
    }

    @DeleteMapping(DELETE_RECORD)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = DELETE, description = DELETEE_TIMERECORD_DESCRIPTION)
    public void deleteTimeRecord(@PathVariable UUID employeeId, @PathVariable Long timeRecordId) {
        useCase.deleteTimeRecord(employeeId, timeRecordId);
    }

    @GetMapping(REPORT)
    @Operation(summary = GET_ALL, description = ALL_OBJECTS_DESCRIPTION)
    public List<TimeRecordResponse> report(@PathVariable UUID employeeId, @Valid @RequestBody ListReportRequest req) {
        return useCase.listReport(employeeId, req);
    }

    @GetMapping(REPORT_PDF)
    @Operation(summary = GET_ALL, description = TIMERECORD_DOWNLOAD_DESCRIPTION)
    public ResponseEntity<byte[]> reportPdf(@PathVariable UUID employeeId, @Valid @RequestBody ListReportRequest req) {

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
    @Operation(summary = GET_ALL, description = ALL_OBJECTS_DESCRIPTION)
    public ResponseEntity<SimpleReportResponse> simpleReport(@PathVariable UUID employeeId,
                                                             @Valid @RequestBody SimpleReportRequest req) {
        var resp = useCase.simpleReport(employeeId, req);
        return ResponseEntity.ok(resp);
    }

    @GetMapping(REPORT_SIMPLE_PDF)
    @Operation(summary = GET_ALL, description = TIMERECORD_DOWNLOAD_DESCRIPTION)
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
