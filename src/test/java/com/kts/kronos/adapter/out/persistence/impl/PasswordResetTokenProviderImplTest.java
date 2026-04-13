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

import static com.kts.kronos.constants.Messages.SAO_PAULO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetTokenProviderImplTest {

    @InjectMocks
    private PasswordResetTokenProviderImpl provider;

    @Mock
    private PasswordResetTokenRepository repository;

    @Test
    @DisplayName("generate: remove token antigo do usuário e salva novo token")
    void shouldReplaceExistingTokenForUser() {
        UUID userId = UUID.randomUUID();
        PasswordResetTokenEntity oldToken = PasswordResetTokenEntity.builder()
                .token("old-token")
                .userId(userId)
                .expiryDate(LocalDateTime.now(SAO_PAULO).plusMinutes(5))
                .build();

        when(repository.findByUserId(userId)).thenReturn(Optional.of(oldToken));

        String newToken = provider.generateAndSaveToken(userId);

        verify(repository).delete(oldToken);
        ArgumentCaptor<PasswordResetTokenEntity> captor = ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
        verify(repository).save(captor.capture());

        PasswordResetTokenEntity saved = captor.getValue();
        assertEquals(newToken, saved.getToken());
        assertEquals(userId, saved.getUserId());
        assertTrue(saved.getExpiryDate().isAfter(LocalDateTime.now(SAO_PAULO).plusMinutes(20)));
    }

    @Test
    @DisplayName("validate: retorna userId quando token existe e não expirou")
    void shouldReturnUserIdForValidToken() {
        UUID userId = UUID.randomUUID();
        String token = "valid-token";
        PasswordResetTokenEntity entity = PasswordResetTokenEntity.builder()
                .token(token)
                .userId(userId)
                .expiryDate(LocalDateTime.now(SAO_PAULO).plusMinutes(10))
                .build();

        when(repository.findByTokenAndExpiryDateAfter(eq(token), any(LocalDateTime.class)))
                .thenReturn(Optional.of(entity));

        Optional<UUID> result = provider.validateToken(token);

        assertEquals(Optional.of(userId), result);
    }

    @Test
    @DisplayName("validate: retorna vazio quando token é inválido")
    void shouldReturnEmptyForInvalidToken() {
        when(repository.findByTokenAndExpiryDateAfter(eq("invalid"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        Optional<UUID> result = provider.validateToken("invalid");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("delete: remove token quando ele existe")
    void shouldDeleteTokenWhenExists() {
        PasswordResetTokenEntity entity = PasswordResetTokenEntity.builder()
                .token("to-delete")
                .userId(UUID.randomUUID())
                .expiryDate(LocalDateTime.now(SAO_PAULO).plusMinutes(10))
                .build();
        when(repository.findById("to-delete")).thenReturn(Optional.of(entity));

        provider.deleteToken("to-delete");

        verify(repository).delete(entity);
    }

    @Test
    @DisplayName("delete: não tenta remover token inexistente")
    void shouldIgnoreDeleteWhenTokenDoesNotExist() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        provider.deleteToken("missing");

        verify(repository, never()).delete(any());
    }
}
