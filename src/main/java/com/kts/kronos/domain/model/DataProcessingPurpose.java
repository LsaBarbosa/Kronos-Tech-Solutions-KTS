package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.enuns.DataCategory;
import com.kts.kronos.domain.model.enuns.LegalBasis;

public record DataProcessingPurpose(
    String code,
    DataCategory dataCategory,
    LegalBasis legalBasis,
    String purpose,
    String retentionPolicyCode,
    boolean sensitive,
    boolean active
) {}
