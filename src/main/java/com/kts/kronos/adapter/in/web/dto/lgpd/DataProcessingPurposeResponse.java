package com.kts.kronos.adapter.in.web.dto.lgpd;

import com.kts.kronos.domain.model.DataProcessingPurpose;
import com.kts.kronos.domain.model.enuns.DataCategory;
import com.kts.kronos.domain.model.enuns.LegalBasis;

public record DataProcessingPurposeResponse(
        String code,
        DataCategory dataCategory,
        LegalBasis legalBasis,
        String purpose,
        String retentionPolicyCode,
        boolean sensitive,
        boolean active
) {
    public static DataProcessingPurposeResponse fromDomain(DataProcessingPurpose purpose) {
        return new DataProcessingPurposeResponse(
                purpose.code(),
                purpose.dataCategory(),
                purpose.legalBasis(),
                purpose.purpose(),
                purpose.retentionPolicyCode(),
                purpose.sensitive(),
                purpose.active()
        );
    }
}
