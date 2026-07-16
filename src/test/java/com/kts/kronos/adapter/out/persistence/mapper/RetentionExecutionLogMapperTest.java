package com.kts.kronos.adapter.out.persistence.mapper;

import com.kts.kronos.adapter.out.persistence.entity.RetentionExecutionLogEntity;
import com.kts.kronos.domain.model.RetentionExecutionLog;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RetentionExecutionLogMapperTest {

    private final RetentionExecutionLogMapper mapper = new RetentionExecutionLogMapper();

    @Test
    void toDomain_mapsAllFieldsCorrectly() {
        var now = Instant.now();
        var id = UUID.randomUUID();
        var entity = RetentionExecutionLogEntity.builder()
                .executionId(id)
                .policyCode("TEST_POLICY")
                .resourceType(RetentionResourceType.MESSAGE)
                .executionMode("DRY_RUN")
                .startedAt(now)
                .finishedAt(now.plusSeconds(5))
                .status("SUCCESS")
                .scannedCount(100)
                .affectedCount(50)
                .skippedCount(20)
                .errorCount(0)
                .notes("OK")
                .build();

        RetentionExecutionLog result = mapper.toDomain(entity);

        assertEquals(id, result.executionId());
        assertEquals("TEST_POLICY", result.policyCode());
        assertEquals(RetentionResourceType.MESSAGE, result.resourceType());
        assertEquals("DRY_RUN", result.executionMode());
        assertEquals(now, result.startedAt());
        assertEquals(now.plusSeconds(5), result.finishedAt());
        assertEquals("SUCCESS", result.status());
        assertEquals(100, result.scannedCount());
        assertEquals(50, result.affectedCount());
        assertEquals(20, result.skippedCount());
        assertEquals(0, result.errorCount());
        assertEquals("OK", result.notes());
    }

    @Test
    void toPersistence_mapsAllFieldsCorrectly() {
        var now = Instant.now();
        var id = UUID.randomUUID();
        var log = new RetentionExecutionLog(
                id, "TEST_POLICY", RetentionResourceType.DOCUMENT,
                "APPLY", now, now.plusSeconds(3), "SUCCESS",
                200, 100, 50, 0, "notes"
        );

        RetentionExecutionLogEntity entity = mapper.toPersistence(log);

        assertEquals(id, entity.getExecutionId());
        assertEquals("TEST_POLICY", entity.getPolicyCode());
        assertEquals(RetentionResourceType.DOCUMENT, entity.getResourceType());
        assertEquals("APPLY", entity.getExecutionMode());
        assertEquals(now, entity.getStartedAt());
        assertEquals(200, entity.getScannedCount());
        assertEquals("notes", entity.getNotes());
    }
}
