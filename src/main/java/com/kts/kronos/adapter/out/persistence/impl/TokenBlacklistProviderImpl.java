package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.BlacklistedTokenRepository;
import com.kts.kronos.adapter.out.persistence.entity.BlacklistedTokenEntity;
import com.kts.kronos.application.port.out.provider.TokenBlacklistProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HexFormat;

import static com.kts.kronos.constants.Messages.SAO_PAULO;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenBlacklistProviderImpl implements TokenBlacklistProvider {
    private static final HexFormat HEX = HexFormat.of();

    private final BlacklistedTokenRepository repository;

    @Override
    public void addToBlacklist(String rawToken, Date tokenExpiration) {
        String tokenHash = hashToken(rawToken);
        LocalDateTime expiresAt = convertToLocalDateTime(tokenExpiration);

        var entity = BlacklistedTokenEntity.builder()
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .build();

        repository.save(entity);
        log.info("Token adicionado à blacklist com expiração em {}.", expiresAt);
    }

    @Override
    public boolean isBlacklisted(String rawToken) {
        String tokenHash = hashToken(rawToken);
        return repository.existsByTokenHash(tokenHash);
    }

    @Override
    public void deleteExpiredTokens() {
        LocalDateTime now = LocalDateTime.now(SAO_PAULO);
        repository.deleteExpiredTokens(now);
        log.info("Tokens expirados removidos da blacklist.");
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

    private LocalDateTime convertToLocalDateTime(Date date) {
        return date.toInstant()
                .atZone(ZoneId.of("America/Sao_Paulo"))
                .toLocalDateTime();
    }
}
