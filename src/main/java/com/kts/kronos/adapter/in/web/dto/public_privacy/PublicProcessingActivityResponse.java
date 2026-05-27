package com.kts.kronos.adapter.in.web.dto.public_privacy;

import java.util.List;

public record PublicProcessingActivityResponse(
        String code,
        String title,
        String description,
        List<String> dataCategories,
        List<String> purposes,
        List<String> legalBases,
        String retentionPolicy,
        List<String> dataSubjectRights
) {
}
