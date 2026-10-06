package com.kts.kronos.application.service.retention;

import com.kts.kronos.adapter.out.persistence.TimeRecordRepository;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import com.kts.kronos.domain.model.enuns.RetentionExecutionMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeRecordRetentionProcessorTest {

    @Mock
    private TimeRecordRepository timeRecordRepository;

    @InjectMocks
    private TimeRecordRetentionProcessor processor;

    private RetentionPolicy createPolicy() {
        return new RetentionPolicy(UUID.randomUUID(), "TIME_RECORD_5Y", "Registros de ponto",
                "TIME_RECORD", 1825, RetentionExecutionMode.DRY_RUN, true, true, false, null, java.time.Instant.now(), java.time.Instant.now());
    }

    @Test
    void shouldSupportTimeRecordType() {
        assertEquals(RetentionResourceType.TIME_RECORD, processor.supports());
    }

    @Test
    void shouldNotSupportApply() {
        assertFalse(processor.supportsApply());
    }

    @Test
    void shouldNotBeDestructive() {
        assertFalse(processor.isDestructive());
    }

    @Test
    void shouldSupportDryRun() {
        assertTrue(processor.supportsDryRun());
    }

    @Test
    void shouldReturnDryRunSuccessWhenModeIsDryRun() {
        var policy = createPolicy();
        var result = processor.execute(policy, "DRY_RUN");

        assertNotNull(result);
        assertEquals("DRY_RUN", result.executionMode());
        assertEquals("SUCCESS", result.status());
    }

    @Test
    void shouldReturnBlockedWhenModeIsApply() {
        var policy = createPolicy();
        var result = processor.execute(policy, "APPLY");

        assertNotNull(result);
        assertEquals("BLOCKED", result.status());
    }
    @Test
    void shouldReturnErrorResultWhenPolicyThrowsInsideTryBlock() {
        // RetentionPolicy is a record, mockable with Mockito 5 inline mock maker
        // First call to policyCode() (inside executeDryRun log.info) throws
        // Second call to policyCode() (inside catch block log.error) must succeed
        RetentionPolicy mockPolicy = org.mockito.Mockito.mock(RetentionPolicy.class);
        org.mockito.Mockito.when(mockPolicy.retentionDays()).thenReturn(1825);
        org.mockito.Mockito.when(mockPolicy.policyCode())
                .thenThrow(new RuntimeException("forced-error"))
                .thenReturn("TIME_RECORD_5Y");

        var result = processor.execute(mockPolicy, "DRY_RUN");

        assertEquals("ERROR", result.status());
    }

}
