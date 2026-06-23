package com.kts.kronos.application.service.demo;

import com.kts.kronos.adapter.in.web.dto.demo.*;
import com.kts.kronos.adapter.out.persistence.DemoJobAuditRepository;
import com.kts.kronos.config.demo.DemoSandboxProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Top-level facade for the CTO demo sandbox endpoints.
 * Delegates to specialized services.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemoSandboxService {

    private final DemoSandboxProperties        props;
    private final DemoSandboxCreateService     createService;
    private final DemoSandboxPurgeOrchestrator purgeOrchestrator;
    private final DemoSandboxValidationService validationService;
    private final DemoSandboxAuditService      auditService;
    private final DemoSandboxLockService       lockService;

    public DemoCreateResponse create(UUID actorUserId, String actorRole) {
        return createService.create(actorUserId, actorRole);
    }

    public DemoPurgeResponse purge(UUID actorUserId, String actorRole) {
        return purgeOrchestrator.purge(actorUserId, actorRole);
    }

    public DemoStatusResponse status() {
        boolean exists = validationService.sandboxExists();
        DemoValidationResult validation = exists
                ? validationService.validateSandboxHealth()
                : DemoValidationResult.noResidues();

        DemoStatusResponse.LastOperation lastOp = auditService
                .findLastSuccess("CREATE")
                .map(a -> new DemoStatusResponse.LastOperation(
                        a.getOperation(), a.getStatus(), a.getFinishedAt()))
                .orElse(null);

        return new DemoStatusResponse(
                props.isEnabled(),
                props.isKillSwitch(),
                exists,
                props.getCompanyName(),
                props.getUsername(),
                props.getSandboxKey(),
                lastOp,
                validation
        );
    }

    public DemoValidationResult validate() {
        if (!validationService.sandboxExists()) {
            return validationService.validateAfterPurge();
        }
        return validationService.validateSandboxHealth();
    }
}
