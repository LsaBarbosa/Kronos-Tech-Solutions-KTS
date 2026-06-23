package com.kts.kronos.application.service.demo;

import com.kts.kronos.adapter.in.web.dto.demo.DemoOperationCounters;
import com.kts.kronos.adapter.out.persistence.DemoJobAuditRepository;
import com.kts.kronos.adapter.out.persistence.entity.DemoJobAuditEntity;
import com.kts.kronos.config.demo.DemoSandboxProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoSandboxAuditService {

    private final DemoJobAuditRepository auditRepository;
    private final DemoSandboxProperties props;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID startAudit(UUID jobId, String operation, UUID actorUserId, String actorRole) {
        UUID auditId = UUID.randomUUID();
        DemoJobAuditEntity entity = DemoJobAuditEntity.builder()
                .auditId(auditId)
                .jobId(jobId)
                .operation(operation)
                .status("STARTED")
                .sandboxKey(props.getSandboxKey())
                .actorUserId(actorUserId)
                .actorRole(actorRole)
                .startedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        auditRepository.save(entity);
        log.info("[DemoSandbox] Audit started: jobId={} op={}", jobId, operation);
        return auditId;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeAudit(UUID auditId, DemoOperationCounters counters) {
        auditRepository.findById(auditId).ifPresent(entity -> {
            LocalDateTime now = LocalDateTime.now();
            entity.setStatus("SUCCESS");
            entity.setFinishedAt(now);
            entity.setDurationMs(java.time.Duration.between(entity.getStartedAt(), now).toMillis());
            applyCounters(entity, counters);
            auditRepository.save(entity);
            log.info("[DemoSandbox] Audit completed: auditId={} status=SUCCESS", auditId);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failAudit(UUID auditId, String sanitizedError) {
        auditRepository.findById(auditId).ifPresent(entity -> {
            LocalDateTime now = LocalDateTime.now();
            entity.setStatus("FAILED");
            entity.setFinishedAt(now);
            entity.setDurationMs(java.time.Duration.between(entity.getStartedAt(), now).toMillis());
            entity.setErrorMessage(sanitizedError != null
                    ? sanitizedError.substring(0, Math.min(sanitizedError.length(), 490))
                    : "Unknown error");
            auditRepository.save(entity);
            log.warn("[DemoSandbox] Audit failed: auditId={} error={}", auditId, sanitizedError);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void partialAudit(UUID auditId, DemoOperationCounters counters, String sanitizedError) {
        auditRepository.findById(auditId).ifPresent(entity -> {
            LocalDateTime now = LocalDateTime.now();
            entity.setStatus("PARTIAL");
            entity.setFinishedAt(now);
            entity.setDurationMs(java.time.Duration.between(entity.getStartedAt(), now).toMillis());
            applyCounters(entity, counters);
            if (sanitizedError != null) {
                entity.setErrorMessage(sanitizedError.substring(0, Math.min(sanitizedError.length(), 490)));
            }
            auditRepository.save(entity);
            log.warn("[DemoSandbox] Audit partial: auditId={}", auditId);
        });
    }

    public Optional<DemoJobAuditEntity> findLastSuccess(String operation) {
        return auditRepository.findTopBySandboxKeyAndStatusOrderByStartedAtDesc(
                props.getSandboxKey(), "SUCCESS");
    }

    private void applyCounters(DemoJobAuditEntity entity, DemoOperationCounters c) {
        if (c == null) return;
        entity.setCompaniesCount(c.companies());
        entity.setUsersCount(c.users());
        entity.setEmployeesCount(c.employees());
        entity.setPointRecordsCount(c.pointRecords());
        entity.setDocumentsCount(c.documents());
        entity.setRequestsCount(c.requests());
        entity.setFilesCount(c.files());
        entity.setSessionsCount(c.sessions());
        entity.setCacheKeysCount(c.cacheKeys());
    }
}
