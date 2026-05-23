package com.kts.kronos.adapter.in.web.dto.lgpd;

import java.util.List;
import java.util.UUID;

public record AnonymizationDryRunResponse(
        UUID employeeId,
        AnonymizationDryRunSummary summary,
        List<AnonymizationDomain> domains,
        List<String> warnings
) {
    public static AnonymizationDryRunResponse create(
            UUID employeeId,
            AnonymizationDryRunSummary summary,
            List<AnonymizationDomain> domains,
            List<String> warnings
    ) {
        return new AnonymizationDryRunResponse(employeeId, summary, domains, warnings);
    }

    public static AnonymizationDryRunResponse empty(UUID employeeId) {
        return new AnonymizationDryRunResponse(
                employeeId,
                AnonymizationDryRunSummary.from(0, 0, 0, 0),
                List.of(),
                List.of()
        );
    }
}
