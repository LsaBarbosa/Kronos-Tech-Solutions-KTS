package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.DryRunTokenRepository;
import com.kts.kronos.adapter.out.persistence.entity.DryRunTokenEntity;
import com.kts.kronos.domain.model.DryRunToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DryRunTokenProviderImplTest {

    @Mock
    private DryRunTokenRepository repository;

    @InjectMocks
    private DryRunTokenProviderImpl provider;

    private DryRunTokenEntity makeEntity(DryRunTokenEntity.DryRunTokenStatus status) {
        DryRunTokenEntity e = new DryRunTokenEntity();
        e.setTokenId(UUID.randomUUID());
        e.setRequestId(UUID.randomUUID());
        e.setTokenValue(UUID.randomUUID());
        e.setEmployeeId(UUID.randomUUID());
        e.setCompanyId(UUID.randomUUID());
        e.setGeneratedByUserId(UUID.randomUUID());
        e.setCreatedAt(Instant.now());
        e.setExpiresAt(Instant.now().plusSeconds(3600));
        e.setConsumedAt(null);
        e.setStatus(status);
        return e;
    }

    private DryRunToken makeToken(DryRunToken.Status status) {
        return new DryRunToken(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                Instant.now(), Instant.now().plusSeconds(3600), null, status
        );
    }

    @Test
    void shouldSaveAndReturnDomainToken() {
        DryRunToken token = makeToken(DryRunToken.Status.PENDING);
        DryRunTokenEntity savedEntity = makeEntity(DryRunTokenEntity.DryRunTokenStatus.PENDING);
        when(repository.save(any())).thenReturn(savedEntity);

        DryRunToken result = provider.save(token);

        assertNotNull(result);
        assertEquals(savedEntity.getTokenId(), result.tokenId());
        assertEquals(DryRunToken.Status.PENDING, result.status());
        verify(repository).save(any(DryRunTokenEntity.class));
    }

    @Test
    void shouldSaveWithConsumedStatus() {
        DryRunToken token = makeToken(DryRunToken.Status.CONSUMED);
        DryRunTokenEntity savedEntity = makeEntity(DryRunTokenEntity.DryRunTokenStatus.CONSUMED);
        when(repository.save(any())).thenReturn(savedEntity);

        DryRunToken result = provider.save(token);

        assertEquals(DryRunToken.Status.CONSUMED, result.status());
    }

    @Test
    void shouldSaveWithExpiredStatus() {
        DryRunToken token = makeToken(DryRunToken.Status.EXPIRED);
        DryRunTokenEntity savedEntity = makeEntity(DryRunTokenEntity.DryRunTokenStatus.EXPIRED);
        when(repository.save(any())).thenReturn(savedEntity);

        DryRunToken result = provider.save(token);

        assertEquals(DryRunToken.Status.EXPIRED, result.status());
    }

    @Test
    void shouldFindByTokenValueWhenPresent() {
        UUID tokenValue = UUID.randomUUID();
        DryRunTokenEntity entity = makeEntity(DryRunTokenEntity.DryRunTokenStatus.PENDING);
        when(repository.findByTokenValue(tokenValue)).thenReturn(Optional.of(entity));

        Optional<DryRunToken> result = provider.findByTokenValue(tokenValue);

        assertTrue(result.isPresent());
        assertEquals(entity.getTokenId(), result.get().tokenId());
    }

    @Test
    void shouldReturnEmptyWhenTokenValueNotFound() {
        UUID tokenValue = UUID.randomUUID();
        when(repository.findByTokenValue(tokenValue)).thenReturn(Optional.empty());

        Optional<DryRunToken> result = provider.findByTokenValue(tokenValue);

        assertFalse(result.isPresent());
    }

    @Test
    void shouldFindByRequestId() {
        UUID requestId = UUID.randomUUID();
        DryRunTokenEntity e1 = makeEntity(DryRunTokenEntity.DryRunTokenStatus.PENDING);
        DryRunTokenEntity e2 = makeEntity(DryRunTokenEntity.DryRunTokenStatus.EXPIRED);
        when(repository.findByRequestId(requestId)).thenReturn(List.of(e1, e2));

        List<DryRunToken> result = provider.findByRequestId(requestId);

        assertEquals(2, result.size());
    }

    @Test
    void shouldFindValidPendingByRequestIdFilteringExpired() {
        UUID requestId = UUID.randomUUID();
        Instant now = Instant.now();

        DryRunTokenEntity valid = makeEntity(DryRunTokenEntity.DryRunTokenStatus.PENDING);
        valid.setExpiresAt(now.plusSeconds(3600)); // valid

        DryRunTokenEntity expired = makeEntity(DryRunTokenEntity.DryRunTokenStatus.PENDING);
        expired.setExpiresAt(now.minusSeconds(1)); // already expired

        when(repository.findByRequestIdAndStatus(requestId, DryRunTokenEntity.DryRunTokenStatus.PENDING))
                .thenReturn(List.of(valid, expired));

        List<DryRunToken> result = provider.findValidPendingByRequestId(requestId, now);

        assertEquals(1, result.size());
        assertEquals(valid.getTokenId(), result.get(0).tokenId());
    }

    @Test
    void shouldMarkAsConsumedWhenTokenFound() {
        UUID tokenValue = UUID.randomUUID();
        Instant consumedAt = Instant.now();
        DryRunTokenEntity entity = makeEntity(DryRunTokenEntity.DryRunTokenStatus.PENDING);
        when(repository.findByTokenValue(tokenValue)).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenReturn(entity);

        provider.markAsConsumed(tokenValue, consumedAt);

        assertEquals(DryRunTokenEntity.DryRunTokenStatus.CONSUMED, entity.getStatus());
        assertEquals(consumedAt, entity.getConsumedAt());
        verify(repository).save(entity);
    }

    @Test
    void shouldNotSaveWhenMarkAsConsumedTokenNotFound() {
        UUID tokenValue = UUID.randomUUID();
        when(repository.findByTokenValue(tokenValue)).thenReturn(Optional.empty());

        provider.markAsConsumed(tokenValue, Instant.now());

        verify(repository, never()).save(any());
    }

    @Test
    void shouldMarkAsExpiredWhenTokenFound() {
        UUID tokenId = UUID.randomUUID();
        DryRunTokenEntity entity = makeEntity(DryRunTokenEntity.DryRunTokenStatus.PENDING);
        when(repository.findById(tokenId)).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenReturn(entity);

        provider.markAsExpired(tokenId);

        assertEquals(DryRunTokenEntity.DryRunTokenStatus.EXPIRED, entity.getStatus());
        verify(repository).save(entity);
    }

    @Test
    void shouldNotSaveWhenMarkAsExpiredTokenNotFound() {
        UUID tokenId = UUID.randomUUID();
        when(repository.findById(tokenId)).thenReturn(Optional.empty());

        provider.markAsExpired(tokenId);

        verify(repository, never()).save(any());
    }

    @Test
    void shouldDelegateExpireOldTokens() {
        Instant now = Instant.now();
        when(repository.expireOldTokens(now)).thenReturn(5);

        int result = provider.expireOldTokens(now);

        assertEquals(5, result);
        verify(repository).expireOldTokens(now);
    }

    @Test
    void shouldDelegateDeleteExpiredTokens() {
        Instant cutoff = Instant.now().minusSeconds(86400);
        when(repository.deleteExpiredTokens(cutoff)).thenReturn(3);

        int result = provider.deleteExpiredTokens(cutoff);

        assertEquals(3, result);
        verify(repository).deleteExpiredTokens(cutoff);
    }

    @Test
    void shouldDelegateExistsValidTokenForRequest() {
        UUID requestId = UUID.randomUUID();
        Instant now = Instant.now();
        when(repository.existsValidTokenForRequest(requestId, now)).thenReturn(true);

        boolean result = provider.existsValidTokenForRequest(requestId, now);

        assertTrue(result);
        verify(repository).existsValidTokenForRequest(requestId, now);
    }
}
