package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.enuns.RetentionPolicyCode;

public record RetentionPolicyCatalogEntry(
    RetentionPolicyCode code,
    String description,
    int retentionDays,
    String action,
    boolean requiresManualApproval,
    boolean active
) {}
