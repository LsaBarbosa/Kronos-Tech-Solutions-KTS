package com.kts.kronos.application.legal;

import com.kts.kronos.domain.model.enuns.RetentionPolicyType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RetentionPolicyCatalogTest {

    private final RetentionPolicyCatalog catalog = new RetentionPolicyCatalog();

    @Test
    void shouldReturnOnlyValidActivePolicies() {
        var activePolicies = catalog.getActivePolicies();

        assertFalse(activePolicies.isEmpty());
        assertTrue(activePolicies.stream().allMatch(policy -> policy.code() != null));
        assertTrue(activePolicies.stream().allMatch(policy -> policy.resourceType() != null));
        assertTrue(activePolicies.stream()
                .filter(policy -> policy.policyType() == RetentionPolicyType.TIME_BASED)
                .allMatch(policy -> policy.retentionDays() != null && policy.retentionDays() > 0));
        assertTrue(activePolicies.stream()
                .filter(policy -> policy.policyType() == RetentionPolicyType.CONSENT_BASED)
                .allMatch(policy -> policy.retentionDays() == null));
        assertTrue(activePolicies.stream()
                .noneMatch(policy -> policy.retentionDays() != null && policy.retentionDays() == -1));
    }

    @Test
    void shouldActivateTimeRecordAndEmployeeContractPoliciesWhenProcessorsExist() {
        var allPolicies = catalog.getAllPolicies();

        // TIME_RECORD and EMPLOYEE_CONTRACT policies should be active now that processors exist
        assertTrue(allPolicies.stream()
                .filter(policy -> policy.code().name().equals("RETENTION_TIME_RECORD")
                        || policy.code().name().equals("RETENTION_EMPLOYEE_CONTRACT"))
                .allMatch(policy -> policy.active()));
    }
}
