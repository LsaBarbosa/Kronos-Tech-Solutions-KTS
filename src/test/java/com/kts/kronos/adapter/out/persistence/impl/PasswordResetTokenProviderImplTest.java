package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.domain.model.PasswordResetToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PasswordResetTokenEntityTest {

    @Test
    @DisplayName("toDomain deve mapear todos os campos da entidade")
    void shouldMapEntityToDomain() {
        UUID userId = UUID.randomUUID();
        LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(30);
        LocalDateTime createdAt = LocalDateTime.now();

        PasswordResetTokenEntity entity = PasswordResetTokenEntity.builder()
                .token("token-abc")
                .userId(userId)
                .expiryDate(expiryDate)
                .createdAt(createdAt)
                .build();

        PasswordResetToken domain = entity.toDomain();

        assertEquals("token-abc", domain.token());
        assertEquals(userId, domain.userId());
        assertEquals(expiryDate, domain.expiryDate());
        assertEquals(createdAt, domain.createdAt());
    }

    @Test
    @DisplayName("fromDomain deve mapear todos os campos do domínio")
    void shouldMapDomainToEntity() {
        UUID userId = UUID.randomUUID();
        LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(30);
        LocalDateTime createdAt = LocalDateTime.now();

        PasswordResetToken domain = new PasswordResetToken(
                "token-xyz",
                userId,
                expiryDate,
                createdAt
        );

        PasswordResetTokenEntity entity = PasswordResetTokenEntity.fromDomain(domain);

        assertEquals("token-xyz", entity.getToken());
        assertEquals(userId, entity.getUserId());
        assertEquals(expiryDate, entity.getExpiryDate());
        assertEquals(createdAt, entity.getCreatedAt());
    }
}
