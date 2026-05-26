package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.RetentionPolicyRepository;
import com.kts.kronos.adapter.out.persistence.entity.RetentionPolicyEntity;
import com.kts.kronos.adapter.out.persistence.mapper.RetentionPolicyMapper;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.enuns.RetentionExecutionMode;
import com.kts.kronos.domain.model.enuns.RetentionPolicyType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetentionPolicyProviderImplTest {

    @Mock
    private RetentionPolicyRepository repository;

    @Spy
    private RetentionPolicyMapper mapper;

    @InjectMocks
    private RetentionPolicyProviderImpl provider;

    @Test
    void shouldSavePolicy() {
        var policy = domain();
        when(repository.save(any(RetentionPolicyEntity.class))).thenReturn(entity(policy));

        var saved = provider.save(policy);

        assertEquals(policy.policyCode(), saved.policyCode());
        verify(repository).save(any(RetentionPolicyEntity.class));
    }

    @Test
    void shouldListEnabledPolicies() {
        var policy = domain();
        when(repository.findByEnabledTrueOrderByPolicyCodeAsc()).thenReturn(List.of(entity(policy)));

        var enabledPolicies = provider.findEnabledPolicies();

        assertEquals(1, enabledPolicies.size());
        assertEquals(policy.resourceType(), enabledPolicies.getFirst().resourceType());
    }

    private RetentionPolicy domain() {
        return new RetentionPolicy(
                UUID.randomUUID(),
                "BIOMETRIC_RETENTION_REVIEW",
                "Review biometric retention.",
                "BIOMETRIC_CONSENT",
                3650,
                RetentionExecutionMode.DRY_RUN,
                true,
                true,
                true,
                null,
                Instant.parse("2026-05-19T10:00:00Z"),
                null
        );
    }

    private RetentionPolicyEntity entity(RetentionPolicy policy) {
        return RetentionPolicyEntity.builder()
                .policyId(policy.policyId())
                .policyCode(policy.policyCode())
                .description(policy.description())
                .policyType(RetentionPolicyType.TIME_BASED)
                .resourceType(policy.resourceType())
                .retentionDays(policy.retentionDays())
                .executionMode(policy.executionMode())
                .enabled(policy.enabled())
                .preserveLaborData(policy.preserveLaborData())
                .preserveFiscalData(policy.preserveFiscalData())
                .lastExecutedAt(policy.lastExecutedAt())
                .createdAt(policy.createdAt())
                .updatedAt(policy.updatedAt())
                .build();
    }
}
