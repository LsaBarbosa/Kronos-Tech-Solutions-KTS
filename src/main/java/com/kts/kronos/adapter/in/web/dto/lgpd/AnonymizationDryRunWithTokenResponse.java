package com.kts.kronos.adapter.in.web.dto.lgpd;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AnonymizationDryRunWithTokenResponse(
        @JsonProperty("dryRunToken")
        String dryRunToken,

        @JsonProperty("tokenExpiresAtSeconds")
        long tokenExpiresAtSeconds,

        @JsonProperty("summary")
        AnonymizationSummary summary,

        @JsonProperty("domains")
        List<AnonymizationDomain> domains,

        @JsonProperty("warnings")
        List<String> warnings
) {
    public record AnonymizationSummary(
            @JsonProperty("totalScanned")
            long totalScanned,

            @JsonProperty("totalAffected")
            long totalAffected,

            @JsonProperty("totalSkipped")
            long totalSkipped,

            @JsonProperty("totalErrors")
            long totalErrors
    ) {}

    public static AnonymizationDryRunWithTokenResponse from(
            String dryRunToken,
            long tokenExpiresAtSeconds,
            AnonymizationDryRunResponse dryRunResponse
    ) {
        var summary = new AnonymizationSummary(
                dryRunResponse.summary().totalScanned(),
                dryRunResponse.summary().totalAffected(),
                dryRunResponse.summary().totalSkipped(),
                dryRunResponse.summary().totalErrors()
        );

        return new AnonymizationDryRunWithTokenResponse(
                dryRunToken,
                tokenExpiresAtSeconds,
                summary,
                dryRunResponse.domains(),
                dryRunResponse.warnings()
        );
    }
}
