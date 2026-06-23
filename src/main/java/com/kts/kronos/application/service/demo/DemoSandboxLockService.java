package com.kts.kronos.application.service.demo;

import com.kts.kronos.adapter.out.persistence.DemoSandboxLockRepository;
import com.kts.kronos.adapter.out.persistence.entity.DemoSandboxLockEntity;
import com.kts.kronos.config.demo.DemoSandboxProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoSandboxLockService {

    private static final String LOCK_KEY = "KRONOS_DEMO_SANDBOX_LOCK";

    private final DemoSandboxLockRepository lockRepository;
    private final DemoSandboxProperties props;

    /**
     * Tries to acquire an exclusive lock for the demo sandbox operation.
     * Cleans up any expired lock first.
     *
     * @return jobId if lock was acquired, throws if another job holds the lock.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID acquireLock(UUID actorUserId) {
        LocalDateTime now = LocalDateTime.now();

        lockRepository.deleteExpired(LOCK_KEY, now);

        UUID jobId = UUID.randomUUID();
        DemoSandboxLockEntity lock = DemoSandboxLockEntity.builder()
                .lockKey(LOCK_KEY)
                .lockedAt(now)
                .lockedBy(actorUserId)
                .jobId(jobId)
                .expiresAt(now.plus(props.getLockTimeout()))
                .build();

        try {
            lockRepository.saveAndFlush(lock);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Demo sandbox operation already in progress. Try again later.", e);
        }

        log.info("[DemoSandbox] Lock acquired: jobId={}", jobId);
        return jobId;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void releaseLock() {
        lockRepository.deleteById(LOCK_KEY);
        log.info("[DemoSandbox] Lock released");
    }

    public boolean isLocked() {
        return lockRepository.findById(LOCK_KEY)
                .map(l -> l.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElse(false);
    }
}
