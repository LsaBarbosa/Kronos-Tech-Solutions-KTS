package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.enuns.RetentionAction;
import com.kts.kronos.domain.model.enuns.RetentionPolicyCode;
import com.kts.kronos.domain.model.enuns.RetentionPolicyType;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;

public record RetentionPolicyCatalogEntry(
    RetentionPolicyCode code,
    String description,
    RetentionPolicyType policyType,
    RetentionResourceType resourceType,
    Integer retentionDays,
    RetentionAction action,
    boolean sensitive,
    boolean active,
    boolean preserveLaborData,
    boolean preserveFiscalData,
    boolean schedulerExecutable
) {
    public RetentionPolicyCatalogEntry(
            RetentionPolicyCode code,
            String description,
            RetentionPolicyType policyType,
            RetentionResourceType resourceType,
            int retentionDays,
            RetentionAction action,
            boolean sensitive,
            boolean active,
            boolean preserveLaborData,
            boolean preserveFiscalData,
            boolean schedulerExecutable
    ) {
        this(
                code,
                description,
                policyType,
                resourceType,
                Integer.valueOf(retentionDays),
                action,
                sensitive,
                active,
                preserveLaborData,
                preserveFiscalData,
                schedulerExecutable
        );
    }
}
