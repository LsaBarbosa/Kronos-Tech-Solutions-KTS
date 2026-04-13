package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.PasswordResetTokenRepository;
import com.kts.kronos.adapter.out.persistence.entity.PasswordResetTokenEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetTokenProviderImplTest {

    @Mock
    private PasswordResetTokenRepository repository;

    @InjectMocks
    private PasswordResetTokenProviderImpl provider;

    @Test
    @DisplayName("generateAndSaveToken: deve persistir hash do token e retornar token bruto")
    void shouldPersistHashedTokenAndReturnRawToken() {
        UUID userId = UUID.randomUUID();
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());

        String rawToken = provider.generateAndSaveToken(userId);

        ArgumentCaptor<PasswordResetTokenEntity> entityCaptor = ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
        verify(repository).save(entityCaptor.capture());
        PasswordResetTokenEntity saved = entityCaptor.getValue();

        assertEquals(userId, saved.getUserId());
        assertFalse(rawToken.isBlank());
        assertFalse(rawToken.equals(saved.getToken()));
        assertTrue(saved.getToken().matches("^[a-f0-9]{64}$"));
    }

    @Test
    @DisplayName("validateToken: deve validar usando hash e retornar userId")
    void shouldValidateUsingHashAndReturnUserId() {
        UUID userId = UUID.randomUUID();
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());

        String rawToken = provider.generateAndSaveToken(userId);
        ArgumentCaptor<PasswordResetTokenEntity> entityCaptor = ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
        verify(repository).save(entityCaptor.capture());
        PasswordResetTokenEntity saved = entityCaptor.getValue();

        when(repository.findByTokenAndExpiryDateAfter(any(), any(LocalDateTime.class)))
                .thenReturn(Optional.of(saved));

        Optional<UUID> validatedUserId = provider.validateToken(rawToken);

        assertTrue(validatedUserId.isPresent());
        assertEquals(userId, validatedUserId.get());
    }

    @Test
    @DisplayName("deleteToken: deve deletar registro a partir do hash")
    void shouldDeleteTokenByHashedValue() {
        UUID userId = UUID.randomUUID();
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());

        String rawToken = provider.generateAndSaveToken(userId);
        ArgumentCaptor<PasswordResetTokenEntity> entityCaptor = ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
        verify(repository).save(entityCaptor.capture());
        PasswordResetTokenEntity saved = entityCaptor.getValue();

        when(repository.findById(saved.getToken())).thenReturn(Optional.of(saved));

        provider.deleteToken(rawToken);

        verify(repository).delete(saved);
    }
}
