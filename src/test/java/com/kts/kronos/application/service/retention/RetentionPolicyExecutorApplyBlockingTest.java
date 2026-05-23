package com.kts.kronos.application.service.retention;

import com.kts.kronos.application.port.out.provider.RetentionExecutionLogProvider;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.enuns.RetentionExecutionMode;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import com.kts.kronos.domain.model.RetentionExecutionLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RetentionPolicyExecutorApplyBlockingTest {

    @Mock
    private RetentionExecutionLogProvider executionLogProvider;

    private RetentionPolicyExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new RetentionPolicyExecutor(Collections.emptyList(), executionLogProvider);
    }

    @Test
    void testApplyIsBlockedWhenFlagIsFalse() {
        var policy = createApplyPolicy();
        executor.executePolicy(policy);

        ArgumentCaptor<RetentionExecutionLog> captor = ArgumentCaptor.forClass(RetentionExecutionLog.class);
        verify(executionLogProvider, times(1)).save(captor.capture());

        var savedLog = captor.getValue();
        assertEquals("BLOCKED", savedLog.status());
        assertTrue(savedLog.notes().contains("APPLY execution is currently disabled"));
    }

    @Test
    void testDryRunIsNotBlockedWhenFlagIsFalse() {
        var policy = createDryRunPolicy();

        executor.executePolicy(policy);

        verify(executionLogProvider, times(0)).save(any());
    }

    private RetentionPolicy createApplyPolicy() {
        return new RetentionPolicy(
                UUID.randomUUID(),
                "RET_TEST",
                "Test retention policy",
                RetentionResourceType.BLACKLISTED_TOKEN.toString(),
                30,
                RetentionExecutionMode.APPLY,
                true,
                false,
                false,
                null,
                Instant.now(),
                null
        );
    }

    private RetentionPolicy createDryRunPolicy() {
        return new RetentionPolicy(
                UUID.randomUUID(),
                "RET_TEST",
                "Test retention policy",
                RetentionResourceType.BLACKLISTED_TOKEN.toString(),
                30,
                RetentionExecutionMode.DRY_RUN,
                true,
                false,
                false,
                null,
                Instant.now(),
                null
        );
    }
}
