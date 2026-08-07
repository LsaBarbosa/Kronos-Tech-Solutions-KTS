package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.employee.CreateScheduleExceptionRequest;
import com.kts.kronos.adapter.in.web.dto.employee.ScheduleExceptionResponse;
import com.kts.kronos.application.service.ScheduleExceptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/employee")
@RequiredArgsConstructor
public class ScheduleExceptionController {

    private final ScheduleExceptionService exceptionService;

    @PreAuthorize("hasAnyRole('MANAGER','CTO')")
    @PostMapping("/{employeeId}/schedule-exceptions")
    public ResponseEntity<ScheduleExceptionResponse> create(
            @PathVariable UUID employeeId,
            @Valid @RequestBody CreateScheduleExceptionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(exceptionService.create(employeeId, req));
    }

    @PreAuthorize("hasAnyRole('MANAGER','CTO')")
    @GetMapping("/{employeeId}/schedule-exceptions")
    public ResponseEntity<List<ScheduleExceptionResponse>> list(
            @PathVariable UUID employeeId,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(exceptionService.listByMonth(employeeId, year, month));
    }

    @PreAuthorize("hasAnyRole('MANAGER','CTO')")
    @DeleteMapping("/{employeeId}/schedule-exceptions/{date}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID employeeId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        exceptionService.delete(employeeId, date);
        return ResponseEntity.noContent().build();
    }
}
