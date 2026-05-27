package com.kts.kronos.adapter.in.web.dto.public_privacy;

import java.time.LocalDate;
import java.util.List;

public record PublicProcessingCatalogResponse(
        String version,
        LocalDate effectiveDate,
        List<PublicProcessingActivityResponse> activities
) {
}
