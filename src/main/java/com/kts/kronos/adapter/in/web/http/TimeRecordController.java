package com.kts.kronos.adapter.in.web.http;


import com.kts.kronos.adapter.in.web.dto.timerecord.TimeRecordResponse;
import com.kts.kronos.application.port.in.usecase.TimeRecordUseCase;
import com.kts.kronos.adapter.in.web.dto.timerecord.CreateTimeRecordRequest;
import com.kts.kronos.domain.model.StatusRecord;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/time-records")
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

    @GetMapping("/report/{employeeId}")
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
}
