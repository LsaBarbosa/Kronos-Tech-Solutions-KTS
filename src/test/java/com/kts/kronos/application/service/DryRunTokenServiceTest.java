package com.kts.kronos.application.service;

import com.kts.kronos.application.exceptions.DryRunTokenExpiredException;
import com.kts.kronos.application.exceptions.DryRunTokenInvalidException;
import com.kts.kronos.application.port.out.provider.DryRunTokenProvider;
import com.kts.kronos.domain.model.DryRunToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DryRunTokenServiceTest {

    @Mock private DryRunTokenProvider dryRunTokenProvider;

    private DryRunTokenService service;

    @BeforeEach
    void setUp() {
        service = new DryRunTokenService(dryRunTokenProvider);
    }

    private DryRunToken makePendingToken(UUID tokenValue, Instant expiresAt) {
        return new DryRunToken(
                UUID.randomUUID(),
                UUID.randomUUID(),
                tokenValue,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now(),
                expiresAt,
                null,
                DryRunToken.Status.PENDING
        );
    }

    private DryRunToken makeConsumedToken(UUID tokenValue) {
        return new DryRunToken(
                UUID.randomUUID(),
                UUID.randomUUID(),
                tokenValue,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(600),
                Instant.now().minusSeconds(10),
                DryRunToken.Status.CONSUMED
        );
    }

    // ── generateToken ─────────────────────────────────────────────────────────

    @Test
    void deveGerarTokenComCamposCorretos() {
        var requestId = UUID.randomUUID();
        var employeeId = UUID.randomUUID();
        var companyId = UUID.randomUUID();
        var generatedBy = UUID.randomUUID();

        var savedToken = makePendingToken(UUID.randomUUID(), Instant.now().plusSeconds(900));
        when(dryRunTokenProvider.save(any())).thenReturn(savedToken);

        var result = service.generateToken(requestId, employeeId, companyId, generatedBy);

        assertNotNull(result);
        verify(dryRunTokenProvider).save(any(DryRunToken.class));

        var captor = ArgumentCaptor.forClass(DryRunToken.class);
        verify(dryRunTokenProvider).save(captor.capture());
        var captured = captor.getValue();
        assertEquals(requestId, captured.requestId());
        assertEquals(employeeId, captured.employeeId());
        assertEquals(companyId, captured.companyId());
        assertEquals(generatedBy, captured.generatedByUserId());
        assertEquals(DryRunToken.Status.PENDING, captured.status());
        assertNull(captured.consumedAt());
        assertTrue(captured.expiresAt().isAfter(Instant.now()));
    }

    // ── validateAndGetToken ───────────────────────────────────────────────────

    @Test
    void deveValidarTokenPendente() {
        var tokenValue = UUID.randomUUID();
        var validToken = makePendingToken(tokenValue, Instant.now().plusSeconds(900));
        when(dryRunTokenProvider.findByTokenValue(tokenValue)).thenReturn(Optional.of(validToken));

        var result = service.validateAndGetToken(tokenValue);

        assertNotNull(result);
        assertEquals(tokenValue, result.tokenValue());
    }

    @Test
    void deveLancarDryRunTokenInvalidExceptionQuandoTokenNaoEncontrado() {
        var tokenValue = UUID.randomUUID();
        when(dryRunTokenProvider.findByTokenValue(tokenValue)).thenReturn(Optional.empty());

        var ex = assertThrows(DryRunTokenInvalidException.class,
                () -> service.validateAndGetToken(tokenValue));
        assertTrue(ex.getMessage().contains("não encontrado"));
    }

    @Test
    void deveLancarDryRunTokenExpiredExceptionQuandoTokenExpirou() {
        var tokenValue = UUID.randomUUID();
        var expiredToken = makePendingToken(tokenValue, Instant.now().minusSeconds(1));
        when(dryRunTokenProvider.findByTokenValue(tokenValue)).thenReturn(Optional.of(expiredToken));

        var ex = assertThrows(DryRunTokenExpiredException.class,
                () -> service.validateAndGetToken(tokenValue));
        assertTrue(ex.getMessage().contains("Token expirou"));
    }

    @Test
    void deveLancarDryRunTokenInvalidExceptionQuandoTokenJaFoiConsumido() {
        var tokenValue = UUID.randomUUID();
        var consumedToken = makeConsumedToken(tokenValue);
        when(dryRunTokenProvider.findByTokenValue(tokenValue)).thenReturn(Optional.of(consumedToken));

        var ex = assertThrows(DryRunTokenInvalidException.class,
                () -> service.validateAndGetToken(tokenValue));
        assertTrue(ex.getMessage().contains("já foi utilizado"));
    }

    // ── consumeToken ──────────────────────────────────────────────────────────

    @Test
    void deveConsumirTokenValidoComSucesso() {
        var tokenValue = UUID.randomUUID();
        var validToken = makePendingToken(tokenValue, Instant.now().plusSeconds(900));
        when(dryRunTokenProvider.findByTokenValue(tokenValue)).thenReturn(Optional.of(validToken));
        doNothing().when(dryRunTokenProvider).markAsConsumed(any(), any());

        service.consumeToken(tokenValue);

        verify(dryRunTokenProvider).markAsConsumed(eq(tokenValue), any(Instant.class));
    }

    @Test
    void deveLancarExcecaoAoConsumirTokenExpirado() {
        var tokenValue = UUID.randomUUID();
        var expiredToken = makePendingToken(tokenValue, Instant.now().minusSeconds(1));
        when(dryRunTokenProvider.findByTokenValue(tokenValue)).thenReturn(Optional.of(expiredToken));

        assertThrows(DryRunTokenExpiredException.class,
                () -> service.consumeToken(tokenValue));
        verify(dryRunTokenProvider, never()).markAsConsumed(any(), any());
    }

    @Test
    void deveLancarExcecaoAoConsumirTokenInvalido() {
        var tokenValue = UUID.randomUUID();
        when(dryRunTokenProvider.findByTokenValue(tokenValue)).thenReturn(Optional.empty());

        assertThrows(DryRunTokenInvalidException.class,
                () -> service.consumeToken(tokenValue));
        verify(dryRunTokenProvider, never()).markAsConsumed(any(), any());
    }

    // ── cleanupExpiredTokens ──────────────────────────────────────────────────

    @Test
    void deveLimparTokensExpiradosERetornarContagem() {
        when(dryRunTokenProvider.deleteExpiredTokens(any())).thenReturn(5);

        int deleted = service.cleanupExpiredTokens();

        assertEquals(5, deleted);
        var captor = ArgumentCaptor.forClass(Instant.class);
        verify(dryRunTokenProvider).deleteExpiredTokens(captor.capture());
        // cutoff deve ser ~7 dias atrás
        var cutoff = captor.getValue();
        assertTrue(cutoff.isBefore(Instant.now().minusSeconds(6 * 24 * 3600)));
    }

    @Test
    void deveRetornarZeroQuandoNaoHaTokensParaLimpar() {
        when(dryRunTokenProvider.deleteExpiredTokens(any())).thenReturn(0);

        int deleted = service.cleanupExpiredTokens();

        assertEquals(0, deleted);
    }
}
