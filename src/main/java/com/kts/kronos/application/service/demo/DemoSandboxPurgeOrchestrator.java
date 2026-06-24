package com.kts.kronos.application.service.demo;

import com.kts.kronos.adapter.in.web.dto.demo.*;
import com.kts.kronos.config.demo.DemoSandboxProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoSandboxPurgeOrchestrator {

    private final DemoSandboxProperties        props;
    private final DemoSandboxLockService       lockService;
    private final DemoSandboxAuditService      auditService;
    private final DemoSandboxPurgeService      purgeService;
    private final DemoSandboxValidationService validationService;

    public DemoPurgeResponse purge(UUID actorUserId, String actorRole) {
        // Intentionally does NOT check props.isEnabled(): purge must work even when
        // demo creation is disabled, so leftover data can always be cleaned up.
        if (props.isKillSwitch()) {
            throw new IllegalStateException("Demo sandbox kill switch is active.");
        }

        UUID jobId   = lockService.acquireLock(actorUserId);
        UUID auditId = auditService.startAudit(jobId, "PURGE", actorUserId, actorRole);
        DemoSandboxPurgeService.PurgeCounters pc = null;

        try {
            pc = purgeService.purgeAll();

            var counters = new DemoOperationCounters(
                    pc.companies(), pc.users(), pc.employees(),
                    pc.pointRecords(), pc.documents(), pc.approvals(),
                    pc.files(), pc.sessions(), 0
            );

            var validation = validationService.validateAfterPurge();

            if (!validation.clean()) {
                auditService.partialAudit(auditId, counters,
                        "Residue found after purge: " + validation.issues().size() + " issue(s)");
            } else {
                auditService.completeAudit(auditId, counters);
            }

            log.info("[DemoSandbox] Purge completed: jobId={} clean={}", jobId, validation.clean());
            return new DemoPurgeResponse(jobId, validation.clean() ? "SUCCESS" : "PARTIAL", counters, validation);

        } catch (Exception e) {
            String safeError = e.getMessage() != null
                    ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 400))
                    : "Unknown error";
            auditService.failAudit(auditId, safeError);
            log.error("[DemoSandbox] Purge failed: jobId={}", jobId, e);
            throw new RuntimeException("Demo sandbox purge failed: " + safeError, e);
        } finally {
            lockService.releaseLock();
        }
    }
}
