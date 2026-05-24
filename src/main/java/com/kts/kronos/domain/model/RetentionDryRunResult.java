package com.kts.kronos.domain.model;

public record RetentionDryRunResult(
    String policyCode,
    String resourceType,
    long totalScanned,
    long totalEligible,
    String action,
    boolean requiresManualApproval
) {}
