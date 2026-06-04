package com.kts.kronos.adapter.in.web.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kts.kronos.domain.model.enuns.Role;

import java.time.OffsetDateTime;
import java.util.List;

import static com.kts.kronos.constants.Messages.DATE_PATTERN;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DashboardSummaryResponse(
        Role role,
        @JsonFormat(pattern = DATE_PATTERN)
        OffsetDateTime generatedAt,
        DashboardCompanyInfo company,
        DashboardCtoSummary cto,
        DashboardManagerSummary manager,
        DashboardPartnerSummary partner,
        List<DashboardFallbackItem> fallbacks
) {
}
