package com.kts.kronos.adapter.out.persistence.mapper;

import com.kts.kronos.adapter.out.persistence.entity.RetentionPolicyEntity;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.enuns.RetentionExecutionMode;
import com.kts.kronos.domain.model.enuns.RetentionPolicyType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RetentionPolicyMapperTest {

    private final RetentionPolicyMapper mapper = new RetentionPolicyMapper();

    @Test
    void shouldMapEntityToDomain() {
        var entity = entity();

        var domain = mapper.toDomain(entity);

        assertEquals(entity.getPolicyCode(), domain.policyCode());
        assertEquals(entity.getExecutionMode(), domain.executionMode());
        assertEquals(entity.getPolicyType(), domain.policyType());
    }

    @Test
    void shouldMapDomainToEntity() {
        var domain = domain();

        var entity = mapper.toEntity(domain);

        assertEquals(domain.policyId(), entity.getPolicyId());
        assertEquals(domain.lastExecutedAt(), entity.getLastExecutedAt());
        assertEquals(domain.policyType(), entity.getPolicyType());
    }

    @Test
    void shouldRoundTripPolicy() {
        var original = domain();

        var roundTrip = mapper.toDomain(mapper.toEntity(original));

        assertEquals(original, roundTrip);
    }

    private RetentionPolicy domain() {
        return new RetentionPolicy(
                UUID.randomUUID(),
                "LGPD_REQUEST_RETENTION_REVIEW",
                "Review LGPD request retention.",
                "LGPD_REQUEST",
                1825,
                RetentionExecutionMode.DRY_RUN,
                true,
                true,
                true,
                Instant.parse("2026-05-20T10:00:00Z"),
                Instant.parse("2026-05-19T10:00:00Z"),
                Instant.parse("2026-05-20T10:00:00Z")
        );
    }

    private RetentionPolicyEntity entity() {
        return RetentionPolicyEntity.builder()
                .policyId(UUID.randomUUID())
                .policyCode("LGPD_REQUEST_RETENTION_REVIEW")
                .description("Review LGPD request retention.")
                .policyType(RetentionPolicyType.TIME_BASED)
                .resourceType("LGPD_REQUEST")
                .retentionDays(1825)
                .executionMode(RetentionExecutionMode.DRY_RUN)
                .enabled(true)
                .preserveLaborData(true)
                .preserveFiscalData(true)
                .lastExecutedAt(Instant.parse("2026-05-20T10:00:00Z"))
                .createdAt(Instant.parse("2026-05-19T10:00:00Z"))
                .updatedAt(Instant.parse("2026-05-20T10:00:00Z"))
                .build();
    }
}
