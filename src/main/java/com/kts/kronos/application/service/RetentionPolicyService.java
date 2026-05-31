package com.kts.kronos.application.service;

import com.kts.kronos.application.port.out.provider.RetentionPolicyProvider;
import com.kts.kronos.application.service.retention.RetentionPolicyExecutor;
import com.kts.kronos.domain.model.RetentionExecutionResult;
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
                RetentionExecutionResult result = executor.executePolicy(policy);
                if (isExecutionCompleted(result)) {
                    retentionPolicyProvider.save(policy.markExecuted(executedAt));
                } else {
                    log.warn(
                            "event=retention_policy_not_marked_executed policyCode={} status={}",
                            policy.policyCode(),
                            result.status()
                    );
                }
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

    private boolean isExecutionCompleted(RetentionExecutionResult result) {
        return "SUCCESS".equals(result.status()) || "PARTIAL".equals(result.status());
    }
}
