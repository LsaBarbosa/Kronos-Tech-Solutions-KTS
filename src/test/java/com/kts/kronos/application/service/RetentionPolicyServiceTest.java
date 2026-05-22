package com.kts.kronos.application.service;

import com.kts.kronos.application.port.out.provider.RetentionPolicyProvider;
import com.kts.kronos.application.service.retention.RetentionPolicyExecutor;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.enuns.RetentionExecutionMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetentionPolicyServiceTest {

    @Mock
    private RetentionPolicyProvider retentionPolicyProvider;

    @Mock
    private RetentionPolicyExecutor retentionPolicyExecutor;

    @InjectMocks
    private RetentionPolicyService service;

    @Test
    void shouldExecuteEnabledPoliciesAsNonDestructiveRun() {
        when(retentionPolicyProvider.findEnabledPolicies()).thenReturn(List.of(policy("A"), policy("B")));
        when(retentionPolicyProvider.save(any(RetentionPolicy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int processed = service.executeEnabledPolicies();

        assertEquals(2, processed);
        ArgumentCaptor<RetentionPolicy> captor = ArgumentCaptor.forClass(RetentionPolicy.class);
        verify(retentionPolicyProvider, times(2)).save(captor.capture());
        captor.getAllValues().forEach(savedPolicy -> assertNotNull(savedPolicy.lastExecutedAt()));
    }

    private RetentionPolicy policy(String suffix) {
        return new RetentionPolicy(
                UUID.randomUUID(),
                "POLICY_" + suffix,
                "Policy " + suffix,
                "LGPD_REQUEST",
                30,
                RetentionExecutionMode.DRY_RUN,
                true,
                true,
                true,
                null,
                Instant.parse("2026-05-19T10:00:00Z"),
                null
        );
    }
}
