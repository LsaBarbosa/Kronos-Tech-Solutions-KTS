package com.kts.kronos.adapter.out.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtUtils {
    private static final String PREVIOUS_SECRET_SEPARATOR = ",";
    private static final String PREVIOUS_SECRET_PAIR_SEPARATOR = ":";

    private final Key signingKey;
    private final Map<String, Key> verificationKeys;
    private final long expirationMs;
    private final String issuer;
    private final String audience;
    private final String currentKeyId;
    private final long allowedClockSkewSeconds;
    private final long notBeforeSkewSeconds;

    public JwtUtils(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expirationMs,
            @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.audience}") String audience,
            @Value("${jwt.current-key-id}") String currentKeyId,
            @Value("${jwt.previous-secrets:}") String previousSecrets,
            @Value("${jwt.allowed-clock-skew-seconds:30}") long allowedClockSkewSeconds,
            @Value("${jwt.not-before-skew-seconds:0}") long notBeforeSkewSeconds
    ) {
        this.issuer = requireConfigured("JWT_ISSUER", issuer);
        this.audience = requireConfigured("JWT_AUDIENCE", audience);
        this.currentKeyId = requireConfigured("JWT_CURRENT_KEY_ID", currentKeyId);
        this.signingKey = buildHmacKey("JWT_SECRET", secret);
        this.verificationKeys = buildVerificationKeys(this.currentKeyId, signingKey, previousSecrets);
        this.expirationMs = expirationMs;
        this.allowedClockSkewSeconds = validateNonNegative("JWT_ALLOWED_CLOCK_SKEW_SECONDS", allowedClockSkewSeconds);
        this.notBeforeSkewSeconds = validateNonNegative("JWT_NOT_BEFORE_SKEW_SECONDS", notBeforeSkewSeconds);
    }

    private Key buildHmacKey(String settingName, String secret) {
        String normalizedSecret = requireConfigured(settingName, secret);
        byte[] secretBytes;
        try {
            secretBytes = Base64.getDecoder().decode(normalizedSecret);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Valor inválido para " + settingName + ". Use uma chave Base64 válida (sem aspas).",
                    ex
            );
        }
        return Keys.hmacShaKeyFor(secretBytes);
    }

    private String requireConfigured(String settingName, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(settingName + " não pode ser nulo ou vazio.");
        }

        String normalized = value.trim();
        if (hasMatchingWrappingQuotes(normalized)) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        if (normalized.isBlank() || normalized.startsWith("${")) {
            throw new IllegalArgumentException(settingName + " deve ser configurado explicitamente por ambiente.");
        }
        return normalized;
    }

    private boolean hasMatchingWrappingQuotes(String value) {
        if (value.length() < 2) {
            return false;
        }
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        return (first == '"' && last == '"') || (first == '\'' && last == '\'');
    }

    private Map<String, Key> buildVerificationKeys(String currentKeyId, Key signingKey, String previousSecrets) {
        Map<String, Key> keys = new LinkedHashMap<>();
        keys.put(currentKeyId, signingKey);

        if (previousSecrets == null || previousSecrets.isBlank()) {
            return Map.copyOf(keys);
        }

        for (String entry : previousSecrets.split(PREVIOUS_SECRET_SEPARATOR)) {
            if (entry.isBlank()) {
                continue;
            }
            String[] pair = entry.split(PREVIOUS_SECRET_PAIR_SEPARATOR, 2);
            if (pair.length != 2) {
                throw new IllegalArgumentException(
                        "JWT_PREVIOUS_SECRETS deve usar o formato kid:secretBase64 separado por vírgulas."
                );
            }
            String previousKeyId = requireConfigured("JWT_PREVIOUS_SECRETS kid", pair[0]);
            if (keys.containsKey(previousKeyId)) {
                throw new IllegalArgumentException("JWT_PREVIOUS_SECRETS contém kid duplicado: " + previousKeyId);
            }
            keys.put(previousKeyId, buildHmacKey("JWT_PREVIOUS_SECRETS secret", pair[1]));
        }

        return Map.copyOf(keys);
    }

    private long validateNonNegative(String settingName, long value) {
        if (value < 0) {
            throw new IllegalArgumentException(settingName + " não pode ser negativo.");
        }
        return value;
    }

    public String generateToken(
            UUID employeeId,
            String username,
            String roleName,
            UUID userId,
            boolean termsAccepted
    ) {
        var now = new Date();
        var notBefore = new Date(now.getTime() - (notBeforeSkewSeconds * 1000L));
        return Jwts.builder()
                .setHeaderParam("kid", currentKeyId)
                .setIssuer(issuer)
                .setAudience(audience)
                .setId(UUID.randomUUID().toString())
                .setSubject(username)
                .claim("userId", userId != null ? userId.toString() : null)
                .claim("role", roleName)
                .claim("employeeId", employeeId != null ? employeeId.toString() : null)
                .claim("terms_accepted", termsAccepted)
                .setNotBefore(notBefore)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationMs))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return parseClaims(token)
                .getBody()
                .getSubject();
    }

    public boolean getTermsAcceptedFromToken(String token) {
        var claims = parseClaims(token).getBody();

        // Se não houver a claim (tokens antigos), assume falso por segurança
        Object accepted = claims.get("terms_accepted");
        return Boolean.TRUE.equals(accepted);
    }

    public UUID getEmployeeIdFromToken(String token) {
        var claims = parseClaims(token).getBody();

        String employeeIdStr = claims.get("employeeId", String.class);
        if (employeeIdStr == null || employeeIdStr.isBlank()) {
            return null;
        }
        return UUID.fromString(employeeIdStr);
    }

    public UUID getUserIdFromToken(String token) {
        var claims = parseClaims(token).getBody();

        String userIdStr = claims.get("userId", String.class);
        if (userIdStr == null || userIdStr.isBlank()) {
            return null;
        }
        return UUID.fromString(userIdStr);
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Jws<Claims> parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKeyResolver(new SigningKeyResolverAdapter() {
                    @Override
                    public Key resolveSigningKey(JwsHeader header, Claims claims) {
                        return resolveVerificationKey(header.getKeyId());
                    }
                })
                .requireIssuer(issuer)
                .requireAudience(audience)
                .setAllowedClockSkewSeconds(allowedClockSkewSeconds)
                .build()
                .parseClaimsJws(token);
    }

    private Key resolveVerificationKey(String keyId) {
        if (keyId == null || keyId.isBlank()) {
            throw new JwtException("Token JWT sem kid não é aceito.");
        }

        Key verificationKey = verificationKeys.get(keyId);
        if (verificationKey == null) {
            throw new JwtException("Token JWT assinado com kid desconhecido.");
        }
        return verificationKey;
    }
}
