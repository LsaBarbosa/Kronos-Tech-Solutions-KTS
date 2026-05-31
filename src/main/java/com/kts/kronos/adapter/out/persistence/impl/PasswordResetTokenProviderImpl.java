package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.PasswordResetTokenRepository;
import com.kts.kronos.adapter.out.persistence.entity.PasswordResetTokenEntity;
import com.kts.kronos.application.port.out.provider.PasswordResetTokenProvider;
import com.kts.kronos.application.security.PrivacyLogReferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.SAO_PAULO;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetTokenProviderImpl implements PasswordResetTokenProvider {
    private static final long EXPIRATION_MINUTES = 30;
    private static final int TOKEN_SIZE_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();

    private final PasswordResetTokenRepository repository;
    private final PrivacyLogReferenceService privacyLogReferenceService;

    @Override
    public String generateAndSaveToken(UUID userId) {
        String rawToken = generateRawToken();
        String tokenHash = hashToken(rawToken);

        LocalDateTime expiryDate = LocalDateTime.now(SAO_PAULO).plusMinutes(EXPIRATION_MINUTES);

        repository.findByUserId(userId).ifPresent(repository::delete);

        var entity = PasswordResetTokenEntity.builder()
                .token(tokenHash)
                .userId(userId)
                .expiryDate(expiryDate)
                .build();

        repository.save(entity);

        log.info("event=password_reset_token_hash_created userRef={} expirationMinutes={}",
                privacyLogReferenceService.userRef(userId), EXPIRATION_MINUTES);
        return rawToken;
    }

    @Override
    public Optional<UUID> validateToken(String token) {
        LocalDateTime now = LocalDateTime.now(SAO_PAULO);
        String tokenHash = hashToken(token);

        Optional<PasswordResetTokenEntity> entityOpt = repository.findByTokenAndExpiryDateAfter(tokenHash, now)
                .filter(entity -> secureEquals(entity.getToken(), tokenHash));

        return entityOpt.map(PasswordResetTokenEntity::getUserId);
    }

    @Override
    public void deleteToken(String token) {
        String tokenHash = hashToken(token);
        repository.findById(tokenHash).ifPresent(repository::delete);
        log.info("Hash de token de recuperação deletado do JPA.");
    }

    private String generateRawToken() {
        byte[] randomBytes = new byte[TOKEN_SIZE_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HEX.formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 não disponível.", e);
        }
    }

    private boolean secureEquals(String left, String right) {
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8)
        );
    }
}
