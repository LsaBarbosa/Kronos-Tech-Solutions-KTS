package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.PasswordResetTokenRepository;
import com.kts.kronos.adapter.out.persistence.entity.PasswordResetTokenEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
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
    @DisplayName("generateAndSaveToken: remove token antigo do usuário")
    void shouldDeleteExistingTokenBeforeSavingNewOne() {
        UUID userId = UUID.randomUUID();
        PasswordResetTokenEntity existing = PasswordResetTokenEntity.builder()
                .token("a".repeat(64))
                .userId(userId)
                .expiryDate(LocalDateTime.now().plusMinutes(10))
                .build();
        when(repository.findByUserId(userId)).thenReturn(Optional.of(existing));

        provider.generateAndSaveToken(userId);

        verify(repository).delete(existing);
        verify(repository).save(any(PasswordResetTokenEntity.class));
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
    @DisplayName("validateToken: retorna vazio quando comparação segura falha")
    void shouldReturnEmptyWhenSecureComparisonFails() {
        PasswordResetTokenEntity mismatched = PasswordResetTokenEntity.builder()
                .token("f".repeat(64))
                .userId(UUID.randomUUID())
                .expiryDate(LocalDateTime.now().plusMinutes(10))
                .build();

        when(repository.findByTokenAndExpiryDateAfter(any(), any(LocalDateTime.class)))
                .thenReturn(Optional.of(mismatched));

        assertTrue(provider.validateToken("raw-token").isEmpty());
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

    @Test
    @DisplayName("deleteToken: não deleta quando hash não existe")
    void shouldNotDeleteWhenTokenHashDoesNotExist() {
        when(repository.findById(any())).thenReturn(Optional.empty());

        provider.deleteToken("raw-token");

        verify(repository).findById(any());
    }

    @Test
    @DisplayName("hashToken: falha explicitamente quando SHA-256 não existe")
    void shouldFailWhenSha256IsUnavailable() {
        try (MockedStatic<MessageDigest> mocked = mockStatic(MessageDigest.class)) {
            mocked.when(() -> MessageDigest.getInstance("SHA-256"))
                    .thenThrow(new NoSuchAlgorithmException("missing"));

            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    () -> provider.validateToken("raw-token")
            );

            assertEquals("Algoritmo SHA-256 não disponível.", ex.getMessage());
        }
    }
}
