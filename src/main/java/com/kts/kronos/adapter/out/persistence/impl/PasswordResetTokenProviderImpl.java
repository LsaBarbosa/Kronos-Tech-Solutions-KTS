package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.PasswordResetTokenRepository;
import com.kts.kronos.adapter.out.persistence.entity.PasswordResetTokenEntity;
import com.kts.kronos.application.port.out.provider.PasswordResetTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.SAO_PAULO;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetTokenProviderImpl implements PasswordResetTokenProvider {

    private static final long EXPIRATION_MINUTES = 30;
     private final PasswordResetTokenRepository repository;

    @Override
    public String generateAndSaveToken(UUID userId) {
        var token = UUID.randomUUID().toString();
        var now = LocalDateTime.now(SAO_PAULO);
        var expiryDate = now.plusMinutes(EXPIRATION_MINUTES);

        repository.upsertTokenByUserId(token, userId, expiryDate, now);

        log.info("Token de recuperação JPA gerado para userId: {} com expiração de {} minutos.", userId, EXPIRATION_MINUTES);
        return token;
    }

    @Override
    public Optional<UUID> validateToken(String token) {
        // Busca o token e verifica se ele ainda não expirou
        LocalDateTime now = LocalDateTime.now(SAO_PAULO);
        Optional<PasswordResetTokenEntity> entityOpt = repository.findByTokenAndExpiryDateAfter(token, now);

        return entityOpt.map(PasswordResetTokenEntity::getUserId);
    }

    @Override
    public void deleteToken(String token) {
        // Remove o token do banco de dados
        repository.findById(token).ifPresent(repository::delete);
        log.info("Token de recuperação deletado do JPA: {}", token);
    }
}
