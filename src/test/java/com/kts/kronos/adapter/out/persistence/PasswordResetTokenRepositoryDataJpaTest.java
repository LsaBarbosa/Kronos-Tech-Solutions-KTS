package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.PasswordResetTokenEntity;
import com.kts.kronos.support.jpa.AbstractPostgresDataJpaTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordResetTokenRepositoryDataJpaTest extends AbstractPostgresDataJpaTest {
    @Autowired
    private PasswordResetTokenRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("findByTokenAndExpiryDateAfter: deve retornar token quando ele ainda estiver valido")
    void shouldFindValidToken() {
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 10, 0);

        repository.save(buildToken("valid-token", userId, now.plusMinutes(30), now.minusMinutes(5)));

        entityManager.flush();
        entityManager.clear();

        Optional<PasswordResetTokenEntity> result =
                repository.findByTokenAndExpiryDateAfter("valid-token", now);

        assertTrue(result.isPresent());
        assertEquals("valid-token", result.get().getToken());
        assertEquals(userId, result.get().getUserId());
    }

    @Test
    @DisplayName("findByTokenAndExpiryDateAfter: deve retornar vazio quando o token estiver expirado")
    void shouldReturnEmptyWhenTokenIsExpired() {
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 10, 0);

        repository.save(buildToken("expired-token", userId, now.minusMinutes(1), now.minusHours(1)));

        entityManager.flush();
        entityManager.clear();

        Optional<PasswordResetTokenEntity> result =
                repository.findByTokenAndExpiryDateAfter("expired-token", now);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findByUserId: deve retornar o token vinculado ao usuario")
    void shouldFindTokenByUserId() {
        UUID userId = UUID.randomUUID();

        repository.save(buildToken(
                "user-token",
                userId,
                LocalDateTime.of(2026, 1, 1, 12, 0),
                LocalDateTime.of(2026, 1, 1, 9, 0)
        ));

        entityManager.flush();
        entityManager.clear();

        Optional<PasswordResetTokenEntity> result = repository.findByUserId(userId);

        assertTrue(result.isPresent());
        assertEquals("user-token", result.get().getToken());
        assertEquals(userId, result.get().getUserId());
    }

    @Test
    @DisplayName("deleteExpiredTokens: deve remover apenas tokens expirados")
    void shouldDeleteOnlyExpiredTokens() {
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 10, 0);

        repository.save(buildToken(
                "expired-token",
                UUID.randomUUID(),
                now.minusMinutes(1),
                now.minusHours(2)
        ));
        repository.save(buildToken(
                "valid-token",
                UUID.randomUUID(),
                now.plusMinutes(30),
                now.minusHours(1)
        ));

        entityManager.flush();
        entityManager.clear();

        repository.deleteExpiredTokens(now);
        entityManager.flush();
        entityManager.clear();

        assertTrue(repository.findById("expired-token").isEmpty());
        assertTrue(repository.findById("valid-token").isPresent());
        assertEquals(1, repository.count());
    }

    private PasswordResetTokenEntity buildToken(
            String token,
            UUID userId,
            LocalDateTime expiryDate,
            LocalDateTime createdAt
    ) {
        return PasswordResetTokenEntity.builder()
                .token(token)
                .userId(userId)
                .expiryDate(expiryDate)
                .createdAt(createdAt)
                .build();
    }
}