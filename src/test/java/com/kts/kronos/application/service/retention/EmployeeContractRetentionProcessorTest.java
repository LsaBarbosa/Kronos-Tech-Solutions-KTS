package com.kts.kronos.application.service.retention;

import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import com.kts.kronos.domain.model.enuns.RetentionExecutionMode;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EmployeeContractRetentionProcessorTest {

    private final EmployeeContractRetentionProcessor processor = new EmployeeContractRetentionProcessor();

    private RetentionPolicy createPolicy() {
        return new RetentionPolicy(UUID.randomUUID(), "EMPLOYEE_CONTRACT_20Y", "Contratos CLT",
                "EMPLOYEE_CONTRACT", 7300, RetentionExecutionMode.DRY_RUN, true, true, false, null, java.time.Instant.now(), java.time.Instant.now());
    }

    @Test
    void shouldSupportEmployeeContractType() {
        assertEquals(RetentionResourceType.EMPLOYEE_CONTRACT, processor.supports());
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
        var result = processor.execute(createPolicy(), "DRY_RUN");

        assertNotNull(result);
        assertEquals("DRY_RUN", result.executionMode());
        assertEquals("SUCCESS", result.status());
    }

    @Test
    void shouldReturnBlockedWhenModeIsApply() {
        var result = processor.execute(createPolicy(), "APPLY");

        assertNotNull(result);
        assertEquals("BLOCKED", result.status());
    }
}
