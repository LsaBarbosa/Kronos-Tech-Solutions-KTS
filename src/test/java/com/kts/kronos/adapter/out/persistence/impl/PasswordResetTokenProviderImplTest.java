package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.PasswordResetTokenRepository;
import com.kts.kronos.adapter.out.persistence.entity.PasswordResetTokenEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetTokenProviderImplTest {

    @InjectMocks
    private PasswordResetTokenProviderImpl provider;

    @Mock
    private PasswordResetTokenRepository repository;

    @Test
    void shouldPersistOnlyHashedTokenAndReturnRawToken() {
        UUID userId = UUID.randomUUID();

        when(repository.findByUserId(userId)).thenReturn(Optional.empty());

        String rawToken = provider.generateAndSaveToken(userId);

        ArgumentCaptor<PasswordResetTokenEntity> captor = ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
        verify(repository).save(captor.capture());

        PasswordResetTokenEntity saved = captor.getValue();

        assertNotNull(rawToken);
        assertFalse(rawToken.isBlank());
        assertNotEquals(rawToken, saved.getToken());
        assertEquals(64, saved.getToken().length());
        assertEquals(userId, saved.getUserId());
        assertNotNull(saved.getExpiryDate());
    }

    @Test
    void shouldValidateUsingHashOfProvidedRawToken() throws Exception {
        UUID userId = UUID.randomUUID();
        String rawToken = "token-bruto-teste";
        String tokenHash = sha256(rawToken);

        PasswordResetTokenEntity entity = PasswordResetTokenEntity.builder()
                .token(tokenHash)
                .userId(userId)
                .expiryDate(LocalDateTime.now().plusMinutes(10))
                .createdAt(LocalDateTime.now())
                .build();

        when(repository.findByTokenAndExpiryDateAfter(eq(tokenHash), any(LocalDateTime.class)))
                .thenReturn(Optional.of(entity));

        Optional<UUID> validated = provider.validateToken(rawToken);

        assertTrue(validated.isPresent());
        assertEquals(userId, validated.get());
    }

    @Test
    void shouldDeleteUsingHashOfProvidedRawToken() throws Exception {
        UUID userId = UUID.randomUUID();
        String rawToken = "token-bruto-teste";
        String tokenHash = sha256(rawToken);

        PasswordResetTokenEntity entity = PasswordResetTokenEntity.builder()
                .token(tokenHash)
                .userId(userId)
                .expiryDate(LocalDateTime.now().plusMinutes(10))
                .createdAt(LocalDateTime.now())
                .build();

        when(repository.findById(tokenHash)).thenReturn(Optional.of(entity));

        provider.deleteToken(rawToken);

        verify(repository).findById(tokenHash);
        verify(repository).delete(entity);
    }

    private String sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}