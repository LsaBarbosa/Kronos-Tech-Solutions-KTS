package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.retention.RetentionExecutionSummaryResponse;
import com.kts.kronos.adapter.in.web.dto.retention.RetentionMetricsResponse;
import com.kts.kronos.adapter.in.web.dto.retention.RetentionPolicyResponse;
import com.kts.kronos.application.port.out.provider.RetentionExecutionLogProvider;
import com.kts.kronos.application.port.out.provider.RetentionPolicyProvider;
import com.kts.kronos.application.service.retention.RetentionPolicyExecutor;
import com.kts.kronos.domain.model.RetentionExecutionLog;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.enuns.RetentionExecutionMode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/admin/retention")
@RequiredArgsConstructor
public class RetentionController {
    private final RetentionPolicyProvider retentionPolicyProvider;
    private final RetentionExecutionLogProvider retentionExecutionLogProvider;
    private final RetentionPolicyExecutor retentionPolicyExecutor;

    @PreAuthorize("hasAnyRole('CTO', 'MANAGER')")
    @GetMapping("/dashboard")
    public ResponseEntity<RetentionMetricsResponse> getDashboard() {
        List<RetentionPolicy> policies = retentionPolicyProvider.findAll();
        List<RetentionExecutionLog> recentExecutions = retentionExecutionLogProvider.findRecent(10);

        List<RetentionMetricsResponse.PolicyMetric> policyMetrics = policies.stream()
                .map(policy -> new RetentionMetricsResponse.PolicyMetric(
                        policy.policyCode(),
                        policy.resourceType(),
                        (long) policy.retentionDays(),
                        policy.enabled(),
                        policy.preserveLaborData(),
                        policy.preserveFiscalData(),
                        policy.lastExecutedAt(),
                        policy.executionMode().name()
                ))
                .toList();

        List<RetentionMetricsResponse.ExecutionMetric> executionMetrics = recentExecutions.stream()
                .map(log -> new RetentionMetricsResponse.ExecutionMetric(
                        log.executionId().toString(),
                        log.policyCode(),
                        log.resourceType().name(),
                        log.executionMode(),
                        log.scannedCount(),
                        log.affectedCount(),
                        log.skippedCount(),
                        log.errorCount(),
                        log.finishedAt(),
                        log.status()
                ))
                .toList();

        long enabledCount = policies.stream().filter(RetentionPolicy::enabled).count();
        long disabledCount = policies.size() - enabledCount;

        var response = new RetentionMetricsResponse(
                policyMetrics,
                executionMetrics,
                enabledCount,
                disabledCount,
                Instant.now()
        );

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyRole('CTO', 'MANAGER')")
    @GetMapping("/policies")
    public ResponseEntity<List<RetentionPolicyResponse>> listPolicies() {
        List<RetentionPolicy> policies = retentionPolicyProvider.findAll();
        List<RetentionPolicyResponse> responses = policies.stream()
                .map(RetentionPolicyResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PreAuthorize("hasAnyRole('CTO', 'MANAGER')")
    @GetMapping("/policies/{policyCode}")
    public ResponseEntity<RetentionPolicyResponse> getPolicy(@PathVariable String policyCode) {
        RetentionPolicy policy = retentionPolicyProvider.findByCode(policyCode);
        return ResponseEntity.ok(RetentionPolicyResponse.fromDomain(policy));
    }

    @PreAuthorize("hasRole('CTO')")
    @PostMapping("/policies/{policyCode}/dry-run")
    public ResponseEntity<RetentionExecutionSummaryResponse> dryRunPolicy(@PathVariable String policyCode) {
        RetentionPolicy policy = retentionPolicyProvider.findByCode(policyCode);
        var dryRunPolicy = new RetentionPolicy(
                policy.policyId(),
                policy.policyCode(),
                policy.description(),
                policy.resourceType(),
                policy.retentionDays(),
                RetentionExecutionMode.DRY_RUN,
                policy.enabled(),
                policy.preserveLaborData(),
                policy.preserveFiscalData(),
                policy.lastExecutedAt(),
                policy.createdAt(),
                policy.updatedAt()
        );
        retentionPolicyExecutor.executePolicy(dryRunPolicy);
        List<RetentionExecutionLog> logs = retentionExecutionLogProvider.findRecent(1);
        return ResponseEntity.ok(RetentionExecutionSummaryResponse.fromDomain(logs.get(0)));
    }

    @PreAuthorize("hasRole('CTO')")
    @PostMapping("/policies/{policyCode}/apply")
    public ResponseEntity<RetentionExecutionSummaryResponse> applyPolicy(@PathVariable String policyCode) {
        RetentionPolicy policy = retentionPolicyProvider.findByCode(policyCode);
        var applyPolicy = new RetentionPolicy(
                policy.policyId(),
                policy.policyCode(),
                policy.description(),
                policy.resourceType(),
                policy.retentionDays(),
                RetentionExecutionMode.APPLY,
                policy.enabled(),
                policy.preserveLaborData(),
                policy.preserveFiscalData(),
                policy.lastExecutedAt(),
                policy.createdAt(),
                policy.updatedAt()
        );
        retentionPolicyExecutor.executePolicy(applyPolicy);
        List<RetentionExecutionLog> logs = retentionExecutionLogProvider.findRecent(1);
        return ResponseEntity.ok(RetentionExecutionSummaryResponse.fromDomain(logs.get(0)));
    }

    @PreAuthorize("hasAnyRole('CTO', 'MANAGER')")
    @GetMapping("/executions")
    public ResponseEntity<Page<RetentionExecutionSummaryResponse>> listExecutions(Pageable pageable) {
        Page<RetentionExecutionLog> executionLogs = retentionExecutionLogProvider.findAll(pageable);
        List<RetentionExecutionSummaryResponse> responses = executionLogs.getContent().stream()
                .map(RetentionExecutionSummaryResponse::fromDomain)
                .toList();

        Page<RetentionExecutionSummaryResponse> responsePage = new PageImpl<>(
                responses,
                pageable,
                executionLogs.getTotalElements()
        );

        return ResponseEntity.ok(responsePage);
    }
}
