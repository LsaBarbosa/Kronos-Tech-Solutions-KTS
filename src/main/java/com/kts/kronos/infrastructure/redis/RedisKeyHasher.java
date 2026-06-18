package com.kts.kronos.infrastructure.redis;

import com.kts.kronos.application.config.KronosRedisProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class RedisKeyHasher {
    private static final HexFormat HEX = HexFormat.of();
    private final byte[] hmacSecret;

    public RedisKeyHasher(KronosRedisProperties properties) {
        String secret = properties.getKeyHmacSecret() == null || properties.getKeyHmacSecret().isBlank()
                ? "local-dev-redis-key-secret"
                : properties.getKeyHmacSecret().trim();
        this.hmacSecret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String hmacSha256Hex(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacSecret, "HmacSHA256"));
            byte[] digest = mac.doFinal(normalize(value).getBytes(StandardCharsets.UTF_8));
            return HEX.formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao calcular HMAC-SHA256.", e);
        }
    }

    public String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalize(value).getBytes(StandardCharsets.UTF_8));
            return HEX.formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 não disponível.", e);
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }
}
