package com.kts.kronos.application.service;

import com.kts.kronos.application.port.out.provider.RetentionPolicyProvider;
import com.kts.kronos.domain.model.RetentionPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * First-stage retention scheduler.
 *
 * This implementation intentionally does not perform destructive actions.
 * It marks policies as executed and logs what would be processed.
 * Real deletion/anonymization must be implemented per data domain after legal validation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RetentionPolicyService {
    private final RetentionPolicyProvider retentionPolicyProvider;

    public int executeEnabledPolicies() {
        Instant executedAt = Instant.now();
        List<RetentionPolicy> policies = retentionPolicyProvider.findEnabledPolicies();

        policies.stream()
                .map(policy -> executePolicy(policy, executedAt))
                .forEach(retentionPolicyProvider::save);

        return policies.size();
    }

    private RetentionPolicy executePolicy(RetentionPolicy policy, Instant executedAt) {
        if (policy.isDryRun()) {
            log.info(
                    "event=retention_policy_execution mode=dry_run policyCode={} resourceType={} retentionDays={}",
                    policy.policyCode(),
                    policy.resourceType(),
                    policy.retentionDays()
            );
            return policy.markExecuted(executedAt);
        }

        if (policy.preserveLaborData() || policy.preserveFiscalData()) {
            log.info(
                    "event=retention_policy_execution mode=protected_noop policyCode={} preserveLaborData={} preserveFiscalData={}",
                    policy.policyCode(),
                    policy.preserveLaborData(),
                    policy.preserveFiscalData()
            );
            return policy.markExecuted(executedAt);
        }

        log.info(
                "event=retention_policy_execution mode=apply_noop policyCode={} resourceType={}",
                policy.policyCode(),
                policy.resourceType()
        );
        return policy.markExecuted(executedAt);
    }
}
