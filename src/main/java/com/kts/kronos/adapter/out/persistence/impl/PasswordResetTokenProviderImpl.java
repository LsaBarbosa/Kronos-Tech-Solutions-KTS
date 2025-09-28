package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.application.port.out.provider.PasswordResetTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.kts.kronos.constants.Messages.PASSWORD_RESET_KEY_PREFIX;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetTokenProviderImpl implements PasswordResetTokenProvider {
    private static final long EXPIRATION_MINUTES = 30;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public String generateAndSaveToken(UUID userId) {
        String token = UUID.randomUUID().toString();
        String redisKey = PASSWORD_RESET_KEY_PREFIX + token;

        // Salva o userId (convertido para String) no Redis com um TTL
        redisTemplate.opsForValue().set(
                redisKey,
                userId.toString(),
                EXPIRATION_MINUTES,
                TimeUnit.MINUTES
        );

        log.info("Token de recuperação gerado para userId: {} com expiração de {} minutos.", userId, EXPIRATION_MINUTES);
        return token;
    }

    @Override
    public Optional<UUID> validateToken(String token) {
        String redisKey = PASSWORD_RESET_KEY_PREFIX + token;
        Object userIdObj = redisTemplate.opsForValue().get(redisKey);

        if (userIdObj instanceof String userIdStr) {
            try {
                // O token é válido, retorna o userId
                return Optional.of(UUID.fromString(userIdStr));
            } catch (IllegalArgumentException e) {
                // Conteúdo corrompido, limpa a chave
                log.error("Token de recuperação encontrou um userId inválido no Redis. Chave: {}", redisKey);
                redisTemplate.delete(redisKey);
                return Optional.empty();
            }
        }
        // Token não encontrado ou expirado
        return Optional.empty();
    }

    @Override
    public void deleteToken(String token) {
        String redisKey = PASSWORD_RESET_KEY_PREFIX + token;
        redisTemplate.delete(redisKey);
        log.info("Token de recuperação deletado: {}", token);
    }
}
