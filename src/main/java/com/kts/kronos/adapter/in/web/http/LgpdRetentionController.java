package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.retention.RetentionExecutionSummaryResponse;
import com.kts.kronos.application.port.out.provider.RetentionExecutionLogProvider;
import com.kts.kronos.domain.model.RetentionExecutionLog;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.kts.kronos.constants.ApiPaths.LGPD;
import static com.kts.kronos.constants.ApiPaths.LGPD_RETENTION_EXECUTIONS;
import static com.kts.kronos.constants.ApiPaths.LGPD_RETENTION_EXECUTION_ID;

@RestController
@RequestMapping(LGPD)
@RequiredArgsConstructor
public class LgpdRetentionController {
    private final RetentionExecutionLogProvider retentionExecutionLogProvider;

    @PreAuthorize("hasAnyRole('CTO', 'MANAGER')")
    @GetMapping(LGPD_RETENTION_EXECUTIONS)
    public ResponseEntity<Page<RetentionExecutionSummaryResponse>> listRetentionExecutions(Pageable pageable) {
        Page<RetentionExecutionLog> executionLogs = retentionExecutionLogProvider.findAll(pageable);
        List<RetentionExecutionSummaryResponse> responses = executionLogs.getContent().stream()
                .map(RetentionExecutionSummaryResponse::fromDomain)
                .toList();

        Page<RetentionExecutionSummaryResponse> responsePage = new PageImpl<>(
                responses,
                pageable,
                executionLogs.getTotalElements()
        );

        return ResponseEntity.ok(responsePage);
    }

    @PreAuthorize("hasAnyRole('CTO', 'MANAGER')")
    @GetMapping(LGPD_RETENTION_EXECUTION_ID)
    public ResponseEntity<RetentionExecutionSummaryResponse> getRetentionExecution(
            @PathVariable UUID executionId
    ) {
        RetentionExecutionLog log = retentionExecutionLogProvider.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + executionId));
        return ResponseEntity.ok(RetentionExecutionSummaryResponse.fromDomain(log));
    }
}
