package com.kts.kronos.application.service.demo;

import com.kts.kronos.adapter.in.web.dto.demo.*;
import com.kts.kronos.config.demo.DemoSandboxProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Orchestrates the CTO Demo Sandbox creation flow.
 *
 * <p>Flow: check kill switch → acquire lock → purge residue →
 *         seed data → validate → release lock → audit.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemoSandboxCreateService {

    private final DemoSandboxProperties        props;
    private final DemoSandboxLockService       lockService;
    private final DemoSandboxAuditService      auditService;
    private final DemoSandboxPurgeService      purgeService;
    private final DemoSandboxDataFactory       dataFactory;
    private final DemoSandboxValidationService validationService;

    public DemoCreateResponse create(UUID actorUserId, String actorRole) {
        guardEnabled();

        UUID jobId   = lockService.acquireLock(actorUserId);
        UUID auditId = auditService.startAudit(jobId, "CREATE", actorUserId, actorRole);

        try {
            purgeService.purgeAll();

            DemoSandboxDataFactory.SeedResult seed = dataFactory.createAll();

            var counters = new DemoOperationCounters(
                    1, 1, 1,
                    seed.pointRecords(),
                    seed.documents(),
                    seed.requests(),
                    seed.files(),
                    0, 0
            );

            var validation = validationService.validateSandboxHealth();
            auditService.completeAudit(auditId, counters);

            log.info("[DemoSandbox] Create completed: jobId={}", jobId);

            return new DemoCreateResponse(
                    jobId, "SUCCESS",
                    props.getCompanyName(),
                    props.getUsername(),
                    true,
                    counters,
                    validation
            );

        } catch (Exception e) {
            String safeError = sanitize(e.getMessage());
            auditService.failAudit(auditId, safeError);
            log.error("[DemoSandbox] Create failed: jobId={}", jobId, e);
            throw new RuntimeException("Demo sandbox creation failed: " + safeError, e);
        } finally {
            lockService.releaseLock();
        }
    }

    private void guardEnabled() {
        if (!props.isEnabled()) {
            throw new IllegalStateException("Demo sandbox is disabled. Set KRONOS_DEMO_ENABLED=true.");
        }
        if (props.isKillSwitch()) {
            throw new IllegalStateException("Demo sandbox kill switch is active.");
        }
    }

    private String sanitize(String message) {
        if (message == null) return "Unknown error";
        return message.replaceAll("(?i)(password|token|secret|key|credential)=[^\\s,;]+", "$1=[REDACTED]")
                      .substring(0, Math.min(message.length(), 400));
    }
}
