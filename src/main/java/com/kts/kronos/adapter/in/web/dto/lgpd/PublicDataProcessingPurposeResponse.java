package com.kts.kronos.adapter.in.web.dto.lgpd;

import com.kts.kronos.domain.model.enuns.DataCategory;
import com.kts.kronos.domain.model.enuns.LegalBasis;

public record PublicDataProcessingPurposeResponse(
        String code,
        DataCategory dataCategory,
        LegalBasis legalBasis,
        String purpose,
        String retentionPolicyCode,
        boolean sensitive,
        boolean active
) {
}
