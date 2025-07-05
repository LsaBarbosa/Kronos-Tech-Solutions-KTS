package com.kts.kronos.adapter.in.web.http;


import com.kts.kronos.adapter.in.web.dto.timerecord.TimeRecordResponse;
import com.kts.kronos.application.port.in.usecase.TimeRecordUseCase;
import com.kts.kronos.adapter.in.web.dto.timerecord.CreateTimeRecordRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

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
            @RequestParam(value = "active", required = false) Boolean active
    ) {
        return useCase.listReport(employeeId, reference,active );
    }
}
