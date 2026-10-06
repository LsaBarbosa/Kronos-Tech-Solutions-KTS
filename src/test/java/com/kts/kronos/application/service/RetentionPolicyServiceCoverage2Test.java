package com.kts.kronos.application.service;

import com.kts.kronos.application.port.out.provider.RetentionPolicyProvider;
import com.kts.kronos.application.service.retention.RetentionPolicyExecutor;
import com.kts.kronos.domain.model.RetentionExecutionResult;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.enuns.RetentionExecutionMode;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RetentionPolicyServiceCoverage2Test {

    @Mock private RetentionPolicyProvider retentionPolicyProvider;
    @Mock private RetentionPolicyExecutor retentionPolicyExecutor;

    private RetentionPolicy policy(String code) {
        return new RetentionPolicy(UUID.randomUUID(), code, "desc",
                "LGPD_REQUEST", 30, RetentionExecutionMode.DRY_RUN, true,
                true, false, null, Instant.parse("2026-01-01T00:00:00Z"), null);
    }

    // Covers L39-45 (catch block when executor throws)
    @Test
    void executeEnabledPolicies_whenExecutorThrows_coversCatchBlock() {
        var p = policy("POLICY_A");
        when(retentionPolicyProvider.findEnabledPolicies()).thenReturn(List.of(p));
        when(retentionPolicyExecutor.executePolicy(p)).thenThrow(new RuntimeException("exec error"));

        int count = new RetentionPolicyService(retentionPolicyProvider, retentionPolicyExecutor)
                .executeEnabledPolicies();

        assertEquals(1, count);
        verify(retentionPolicyProvider, never()).save(any());
    }

    // Covers L53 right side: "PARTIAL" → left side false, right side true → isExecutionCompleted=true
    @Test
    void executeEnabledPolicies_withPartialResult_savesPolicy() {
        var p = policy("POLICY_B");
        when(retentionPolicyProvider.findEnabledPolicies()).thenReturn(List.of(p));
        when(retentionPolicyExecutor.executePolicy(p)).thenReturn(
                RetentionExecutionResult.partial(UUID.randomUUID(), "POLICY_B",
                        RetentionResourceType.LGPD_REQUEST, "DRY_RUN",
                        5L, 2L, 3L, 0L, "partial"));
        when(retentionPolicyProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int count = new RetentionPolicyService(retentionPolicyProvider, retentionPolicyExecutor)
                .executeEnabledPolicies();

        assertEquals(1, count);
        verify(retentionPolicyProvider).save(any(RetentionPolicy.class));
    }
}
