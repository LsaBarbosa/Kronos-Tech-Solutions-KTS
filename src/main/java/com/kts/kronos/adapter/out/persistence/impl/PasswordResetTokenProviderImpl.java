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
    // 30 minutos de expiração, como no Redis
    private static final long EXPIRATION_MINUTES = 30;

    // Troca o RedisTemplate pelo JPA Repository
    private final PasswordResetTokenRepository repository;

    @Override
    public String generateAndSaveToken(UUID userId) {
        // Gera o token (UUID para manter o formato original)
        String token = UUID.randomUUID().toString();

        // Define a expiração baseada no fuso horário SAO_PAULO
        LocalDateTime expiryDate = LocalDateTime.now(SAO_PAULO).plusMinutes(EXPIRATION_MINUTES);

        // Antes de salvar, verifica se já existe um token para o usuário e o remove (opcional)
        repository.findByUserId(userId).ifPresent(repository::delete);

        // Cria e salva a entidade no banco de dados
        var entity = PasswordResetTokenEntity.builder()
                .token(token)
                .userId(userId)
                .expiryDate(expiryDate)
                .build();

        repository.save(entity);

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
