package com.kts.kronos.application.demo;

import com.kts.kronos.adapter.out.persistence.DemoSandboxLockRepository;
import com.kts.kronos.adapter.out.persistence.entity.DemoSandboxLockEntity;
import com.kts.kronos.application.service.demo.DemoSandboxLockService;
import com.kts.kronos.config.demo.DemoSandboxProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DemoSandboxLockServiceTest {

    private static final String LOCK_KEY = "KRONOS_DEMO_SANDBOX_LOCK";

    @Mock DemoSandboxLockRepository lockRepository;
    @Mock DemoSandboxProperties      props;

    @InjectMocks DemoSandboxLockService service;

    @BeforeEach
    void setup() {
        when(props.getLockTimeout()).thenReturn(Duration.ofMinutes(5));
        when(lockRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    }

    // ─────────────────────── acquireLock ──────────────────────

    @Test
    void acquireLock_shouldReturnJobIdAndPersistLock() {
        UUID actorId = UUID.randomUUID();

        UUID jobId = service.acquireLock(actorId);

        assertThat(jobId).isNotNull();
        verify(lockRepository).saveAndFlush(argThat(l ->
                LOCK_KEY.equals(l.getLockKey()) &&
                actorId.equals(l.getLockedBy()) &&
                l.getExpiresAt().isAfter(LocalDateTime.now())
        ));
    }

    @Test
    void acquireLock_whenLockAlreadyExists_shouldThrowIllegalStateException() {
        doThrow(new DataIntegrityViolationException("duplicate key"))
                .when(lockRepository).saveAndFlush(any());

        assertThatThrownBy(() -> service.acquireLock(UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already in progress");
    }

    // ─────────────────────── releaseLock ──────────────────────

    @Test
    void releaseLock_shouldDeleteByLockKey() {
        service.releaseLock();
        verify(lockRepository).deleteById(LOCK_KEY);
    }

    // ─────────────────────── isLocked ─────────────────────────

    @Test
    void isLocked_whenActiveLockExists_shouldReturnTrue() {
        DemoSandboxLockEntity lock = DemoSandboxLockEntity.builder()
                .lockKey(LOCK_KEY)
                .expiresAt(LocalDateTime.now().plusMinutes(4))
                .build();
        when(lockRepository.findById(LOCK_KEY)).thenReturn(Optional.of(lock));

        assertThat(service.isLocked()).isTrue();
    }

    @Test
    void isLocked_whenLockIsExpired_shouldReturnFalse() {
        DemoSandboxLockEntity lock = DemoSandboxLockEntity.builder()
                .lockKey(LOCK_KEY)
                .expiresAt(LocalDateTime.now().minusSeconds(1))
                .build();
        when(lockRepository.findById(LOCK_KEY)).thenReturn(Optional.of(lock));

        assertThat(service.isLocked()).isFalse();
    }

    @Test
    void isLocked_whenNoLockFound_shouldReturnFalse() {
        when(lockRepository.findById(LOCK_KEY)).thenReturn(Optional.empty());

        assertThat(service.isLocked()).isFalse();
    }
}
