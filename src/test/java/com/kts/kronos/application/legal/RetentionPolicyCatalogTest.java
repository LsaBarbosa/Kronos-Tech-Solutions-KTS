package com.kts.kronos.application.legal;

import com.kts.kronos.domain.model.enuns.RetentionPolicyType;
import com.kts.kronos.domain.model.RetentionPolicyCatalogEntry;
import com.kts.kronos.domain.model.enuns.RetentionAction;
import com.kts.kronos.domain.model.enuns.RetentionPolicyCode;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void shouldReturnSchedulerExecutablePolicies() {
        var schedulerPolicies = catalog.getSchedulerExecutablePolicies();
        assertFalse(schedulerPolicies.isEmpty());
        assertTrue(schedulerPolicies.stream().allMatch(RetentionPolicyCatalogEntry::schedulerExecutable));
        assertTrue(schedulerPolicies.size() <= catalog.getActivePolicies().size());
    }

    @Test
    void shouldGetPolicyByCode() {
        var policy = catalog.getPolicyByCode(RetentionPolicyCode.RETENTION_BIOMETRIC_ACTIVE_CONSENT);
        assertNotNull(policy);
        assertEquals(RetentionPolicyCode.RETENTION_BIOMETRIC_ACTIVE_CONSENT, policy.code());
    }

    @Test
    void shouldReturnNullForUnknownCode() {
        var policy = catalog.getPolicyByCode(RetentionPolicyCode.RETENTION_PASSWORD_RESET_TOKEN);
        assertNotNull(policy);
    }

    @Test
    void validateActivePolicy_throwsWhenCodeIsNull() {
        var badCatalog = new RetentionPolicyCatalog() {
            @Override
            public List<RetentionPolicyCatalogEntry> getAllPolicies() {
                return List.of(new RetentionPolicyCatalogEntry(
                    null, "desc", RetentionPolicyType.TIME_BASED,
                    RetentionResourceType.TIME_RECORD, 365, RetentionAction.DELETE,
                    false, true, false, false, false
                ));
            }
        };
        assertThrows(IllegalStateException.class, () -> badCatalog.getActivePolicies());
    }

    @Test
    void validateActivePolicy_throwsWhenPolicyTypeIsNull() {
        var badCatalog = new RetentionPolicyCatalog() {
            @Override
            public List<RetentionPolicyCatalogEntry> getAllPolicies() {
                return List.of(new RetentionPolicyCatalogEntry(
                    RetentionPolicyCode.RETENTION_TIME_RECORD, "desc", null,
                    RetentionResourceType.TIME_RECORD, 365, RetentionAction.DELETE,
                    false, true, false, false, false
                ));
            }
        };
        assertThrows(IllegalStateException.class, () -> badCatalog.getActivePolicies());
    }

    @Test
    void validateActivePolicy_throwsWhenResourceTypeIsNull() {
        var badCatalog = new RetentionPolicyCatalog() {
            @Override
            public List<RetentionPolicyCatalogEntry> getAllPolicies() {
                return List.of(new RetentionPolicyCatalogEntry(
                    RetentionPolicyCode.RETENTION_TIME_RECORD, "desc", RetentionPolicyType.TIME_BASED,
                    null, 365, RetentionAction.DELETE,
                    false, true, false, false, false
                ));
            }
        };
        assertThrows(IllegalStateException.class, () -> badCatalog.getActivePolicies());
    }

    @Test
    void validateActivePolicy_throwsWhenActionIsNull() {
        var badCatalog = new RetentionPolicyCatalog() {
            @Override
            public List<RetentionPolicyCatalogEntry> getAllPolicies() {
                return List.of(new RetentionPolicyCatalogEntry(
                    RetentionPolicyCode.RETENTION_TIME_RECORD, "desc", RetentionPolicyType.TIME_BASED,
                    RetentionResourceType.TIME_RECORD, 365, null,
                    false, true, false, false, false
                ));
            }
        };
        assertThrows(IllegalStateException.class, () -> badCatalog.getActivePolicies());
    }

    @Test
    void validateActivePolicy_throwsForTimeBasedWithNullRetentionDays() {
        var badCatalog = new RetentionPolicyCatalog() {
            @Override
            public List<RetentionPolicyCatalogEntry> getAllPolicies() {
                return List.of(new RetentionPolicyCatalogEntry(
                    RetentionPolicyCode.RETENTION_TIME_RECORD, "desc", RetentionPolicyType.TIME_BASED,
                    RetentionResourceType.TIME_RECORD, (Integer) null, RetentionAction.DELETE,
                    false, true, false, false, false
                ));
            }
        };
        assertThrows(IllegalStateException.class, () -> badCatalog.getActivePolicies());
    }

    @Test
    void validateActivePolicy_throwsForTimeBasedWithZeroRetentionDays() {
        var badCatalog = new RetentionPolicyCatalog() {
            @Override
            public List<RetentionPolicyCatalogEntry> getAllPolicies() {
                return List.of(new RetentionPolicyCatalogEntry(
                    RetentionPolicyCode.RETENTION_TIME_RECORD, "desc", RetentionPolicyType.TIME_BASED,
                    RetentionResourceType.TIME_RECORD, 0, RetentionAction.DELETE,
                    false, true, false, false, false
                ));
            }
        };
        assertThrows(IllegalStateException.class, () -> badCatalog.getActivePolicies());
    }

    @Test
    void validateActivePolicy_throwsForNonTimeBasedWithNonPositiveRetentionDays() {
        var badCatalog = new RetentionPolicyCatalog() {
            @Override
            public List<RetentionPolicyCatalogEntry> getAllPolicies() {
                return List.of(new RetentionPolicyCatalogEntry(
                    RetentionPolicyCode.RETENTION_BIOMETRIC_ACTIVE_CONSENT, "desc", RetentionPolicyType.CONSENT_BASED,
                    RetentionResourceType.BIOMETRIC_ARTIFACT, -1, RetentionAction.PRESERVE_WHILE_CONSENT_ACTIVE,
                    false, true, false, false, false
                ));
            }
        };
        assertThrows(IllegalStateException.class, () -> badCatalog.getActivePolicies());
    }

    @Test
    void validateActivePolicy_throwsWhenResourceTypeNotExecutable() {
        var badCatalog = new RetentionPolicyCatalog() {
            @Override
            public List<RetentionPolicyCatalogEntry> getAllPolicies() {
                return List.of(new RetentionPolicyCatalogEntry(
                    RetentionPolicyCode.RETENTION_TIME_RECORD, "desc", RetentionPolicyType.TIME_BASED,
                    RetentionResourceType.BLACKLISTED_TOKEN, 365, RetentionAction.DELETE,
                    false, true, false, false, false
                ));
            }
        };
        assertThrows(IllegalStateException.class, () -> badCatalog.getActivePolicies());
    }

    @Test
    void validateActivePolicy_nonTimeBased_withPositiveRetentionDays_passes() {
        var catEventBased = new RetentionPolicyCatalog() {
            @Override
            public java.util.List<com.kts.kronos.domain.model.RetentionPolicyCatalogEntry> getAllPolicies() {
                return java.util.List.of(new com.kts.kronos.domain.model.RetentionPolicyCatalogEntry(
                    com.kts.kronos.domain.model.enuns.RetentionPolicyCode.RETENTION_TIME_RECORD,
                    "Event-based with retentionDays",
                    com.kts.kronos.domain.model.enuns.RetentionPolicyType.EVENT_BASED,
                    com.kts.kronos.domain.model.enuns.RetentionResourceType.TIME_RECORD,
                    365,
                    com.kts.kronos.domain.model.enuns.RetentionAction.DELETE,
                    false, true, false, false, true
                ));
            }
        };
        var policies = catEventBased.getActivePolicies();
        assertFalse(policies.isEmpty());
    }

}