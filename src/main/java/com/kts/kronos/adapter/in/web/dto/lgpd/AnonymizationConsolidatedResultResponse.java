package com.kts.kronos.adapter.in.web.dto.lgpd;

import com.kts.kronos.domain.model.AnonymizationConsolidatedResult;
import com.kts.kronos.domain.model.AnonymizationExecutionResult;
import com.kts.kronos.domain.model.enuns.AnonymizationConsolidatedStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record AnonymizationConsolidatedResultResponse(
        UUID consolidatedExecutionId,
        UUID employeeId,
        UUID companyId,
        String consolidatedStatus,
        String executionMode,
        Instant startedAt,
        Instant finishedAt,
        long durationMs,
        AnonymizationSummaryResponse summary,
        List<AnonymizationDomainResultResponse> domainResults,
        List<String> failedDomains,
        List<String> warnings
) {
    public static AnonymizationConsolidatedResultResponse from(AnonymizationConsolidatedResult result) {
        long durationMs = result.finishedAt().toEpochMilli() - result.startedAt().toEpochMilli();

        AnonymizationSummaryResponse summary = new AnonymizationSummaryResponse(
                result.totalScanned(),
                result.totalAffected(),
                result.totalSkipped(),
                result.totalErrors()
        );

        List<AnonymizationDomainResultResponse> domainResults = result.domainResults().stream()
                .filter(r -> r != null)
                .map(AnonymizationDomainResultResponse::from)
                .collect(Collectors.toList());

        return new AnonymizationConsolidatedResultResponse(
                result.consolidatedExecutionId(),
                result.employeeId(),
                result.companyId(),
                result.consolidatedStatus().name(),
                result.executionMode(),
                result.startedAt(),
                result.finishedAt(),
                durationMs,
                summary,
                domainResults,
                result.failedDomains(),
                result.warnings()
        );
    }
}
