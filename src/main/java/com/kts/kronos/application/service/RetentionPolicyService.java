package com.kts.kronos.application.service;

import com.kts.kronos.application.port.out.provider.RetentionPolicyProvider;
import com.kts.kronos.application.service.retention.RetentionPolicyExecutor;
import com.kts.kronos.domain.model.RetentionPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RetentionPolicyService {
    private final RetentionPolicyProvider retentionPolicyProvider;
    private final RetentionPolicyExecutor executor;

    public int executeEnabledPolicies() {
        List<RetentionPolicy> policies = retentionPolicyProvider.findEnabledPolicies();
        var executedAt = Instant.now();

        for (var policy : policies) {
            try {
                executor.executePolicy(policy);
                retentionPolicyProvider.save(policy.markExecuted(executedAt));
            } catch (Exception e) {
                log.error(
                        "event=retention_policy_execution_failed policyCode={} error={}",
                        policy.policyCode(),
                        e.getMessage(),
                        e
                );
            }
        }

        return policies.size();
    }
}
