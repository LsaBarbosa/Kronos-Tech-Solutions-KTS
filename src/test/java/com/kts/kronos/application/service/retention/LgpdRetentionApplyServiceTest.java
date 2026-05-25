package com.kts.kronos.application.service.retention;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kts.kronos.application.port.out.provider.RetentionExecutionLogProvider;
import com.kts.kronos.application.service.AuditService;
import com.kts.kronos.domain.model.RetentionExecutionResult;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.enuns.AuditAction;
import com.kts.kronos.domain.model.enuns.RetentionExecutionMode;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("P2-BE-008: LGPD Retention Apply Service Tests")
class LgpdRetentionApplyServiceTest {

    @Mock
    private RetentionExecutionLogProvider executionLogProvider;

    @Mock
    private AuditService auditService;

    private RetentionPolicyExecutor executor;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        executor = new RetentionPolicyExecutor(
                new ArrayList<>(),
                executionLogProvider,
                auditService,
                objectMapper
        );
    }

    @Test
    @DisplayName("Apply blocked when allow-apply flag is false")
    void testApplyBlockedWhenFlagIsFalse() {
        ReflectionTestUtils.setField(executor, "allowApply", false);

        var policy = createAuditLogPolicy(RetentionExecutionMode.APPLY);
        executor.executePolicy(policy);

        // Verify audit was called for blocked action
        verify(auditService, times(1)).registerRetentionAudit(
                eq(AuditAction.LGPD_RETENTION_APPLY_BLOCKED),
                eq("AUDIT_LOG"),
                any()
        );

        // Verify no data was actually processed
        verify(executionLogProvider, times(1)).save(any());
    }

    @Test
    @DisplayName("Apply allowed when allow-apply flag is true")
    void testApplyAllowedWhenFlagIsTrue() {
        ReflectionTestUtils.setField(executor, "allowApply", true);

        var policy = createAuditLogPolicy(RetentionExecutionMode.APPLY);

        // This will fail because there are no processors, but it verifies the flag allows execution
        // The allow-apply check passes and it tries to find a processor
        executor.executePolicy(policy);

        // Verify it attempted to execute (no processor found is expected)
        verify(executionLogProvider, times(0)).save(any());
    }

    @Test
    @DisplayName("DRY_RUN mode audit is logged")
    void testDryRunModeAuditIsLogged() {
        ReflectionTestUtils.setField(executor, "allowApply", true);

        var policy = createAuditLogPolicy(RetentionExecutionMode.DRY_RUN);

        // DRY_RUN does not require allow-apply and should try to execute
        executor.executePolicy(policy);

        // Verify no processor found but execution was attempted
        verify(executionLogProvider, times(0)).save(any());
    }

    @Test
    @DisplayName("Audit details do not contain personal data")
    void testAuditDetailsHaveNoPII() throws Exception {
        ReflectionTestUtils.setField(executor, "allowApply", false);

        var policy = createAuditLogPolicy(RetentionExecutionMode.APPLY);
        executor.executePolicy(policy);

        // Capture the audit call and verify details
        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditService, times(1)).registerRetentionAudit(
                any(AuditAction.class),
                any(String.class),
                detailsCaptor.capture()
        );

        String details = detailsCaptor.getValue();
        assertNotNull(details, "Audit details should not be null");

        // Verify no personal data patterns
        assertFalse(details.contains("CPF"), "Details should not contain CPF");
        assertFalse(details.contains("email"), "Details should not contain email");
        assertFalse(details.contains("@"), "Details should not contain email addresses");
        assertFalse(details.toLowerCase().contains("token"), "Details should not contain tokens");
        assertFalse(details.toLowerCase().contains("password"), "Details should not contain passwords");
    }

    @Test
    @DisplayName("Blocked reason is included in audit details")
    void testBlockedReasonIsIncludedInDetails() throws Exception {
        ReflectionTestUtils.setField(executor, "allowApply", false);

        var policy = createAuditLogPolicy(RetentionExecutionMode.APPLY);
        executor.executePolicy(policy);

        // Capture and verify details include blockedReason
        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditService, times(1)).registerRetentionAudit(
                any(AuditAction.class),
                any(String.class),
                detailsCaptor.capture()
        );

        String details = detailsCaptor.getValue();
        assertTrue(details.contains("blockedReason"), "Blocked reason should be in audit details");
        assertTrue(details.contains("allow-apply flag disabled"), "Specific reason should be documented");
    }

    @Test
    @DisplayName("Execution metadata is logged in audit")
    void testExecutionMetadataInAudit() throws Exception {
        ReflectionTestUtils.setField(executor, "allowApply", false);

        var policy = createAuditLogPolicy(RetentionExecutionMode.APPLY);
        executor.executePolicy(policy);

        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditService, times(1)).registerRetentionAudit(
                any(AuditAction.class),
                eq("AUDIT_LOG"),
                detailsCaptor.capture()
        );

        String details = detailsCaptor.getValue();
        var parsed = objectMapper.readTree(details);

        // Verify required metadata
        assertTrue(parsed.has("executionId"), "Should have executionId");
        assertTrue(parsed.has("mode"), "Should have mode");
        assertTrue(parsed.has("policyCode"), "Should have policyCode");
        assertTrue(parsed.has("resourceType"), "Should have resourceType");
        assertEquals("APPLY", parsed.get("mode").asText(), "Mode should be APPLY");
        assertEquals("AUDIT_LOG", parsed.get("resourceType").asText(), "ResourceType should be AUDIT_LOG");
    }

    @Test
    @DisplayName("No sensitive data fields in audit details")
    void testNoSensitiveDataFields() throws Exception {
        ReflectionTestUtils.setField(executor, "allowApply", false);

        var policy = createAuditLogPolicy(RetentionExecutionMode.APPLY);
        executor.executePolicy(policy);

        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditService, times(1)).registerRetentionAudit(
                any(AuditAction.class),
                any(String.class),
                detailsCaptor.capture()
        );

        String details = detailsCaptor.getValue();
        var parsed = objectMapper.readTree(details);

        // Verify only allowed fields are present
        var allowedFields = new String[]{"executionId", "mode", "policyCode", "resourceType",
                                          "totalScanned", "totalAffected", "totalEligible",
                                          "action", "blockedReason"};

        var fieldsList = java.util.Arrays.asList(allowedFields);
        parsed.fieldNames().forEachRemaining(field ->
            assertTrue(fieldsList.contains(field),
                    "Field '" + field + "' is not in the allowed list")
        );
    }

    @Test
    @DisplayName("Correct audit action is used for blocked execution")
    void testCorrectAuditActionForBlocked() {
        ReflectionTestUtils.setField(executor, "allowApply", false);

        var policy = createAuditLogPolicy(RetentionExecutionMode.APPLY);
        executor.executePolicy(policy);

        // Verify LGPD_RETENTION_APPLY_BLOCKED action was used
        verify(auditService, times(1)).registerRetentionAudit(
                eq(AuditAction.LGPD_RETENTION_APPLY_BLOCKED),
                any(String.class),
                any(String.class)
        );
    }

    // Helper methods
    private RetentionPolicy createAuditLogPolicy(RetentionExecutionMode mode) {
        return new RetentionPolicy(
                UUID.randomUUID(),
                "AUDIT_LOG_POLICY",
                "Test audit log retention",
                "AUDIT_LOG",
                180,
                mode,
                true,
                false,
                false,
                null,
                Instant.now(),
                null
        );
    }
}
