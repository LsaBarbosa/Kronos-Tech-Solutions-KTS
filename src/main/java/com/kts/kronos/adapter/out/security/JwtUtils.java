package com.kts.kronos.adapter.out.security;

import com.kts.kronos.domain.model.BiometricConsentStatus;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtils {
    private final Key key;
    private final long expirationMs;

    public JwtUtils(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expirationMs
    ) {
        String normalizedSecret = normalizeSecret(secret);
        byte[] secretBytes;
        try {
            secretBytes = Base64.getDecoder().decode(normalizedSecret);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Valor inválido para JWT_SECRET. Use uma chave Base64 válida (sem aspas).",
                    ex
            );
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expirationMs = expirationMs;
    }

    private String normalizeSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT_SECRET não pode ser nulo ou vazio.");
        }

        String normalized = secret.trim();
        if (hasMatchingWrappingQuotes(normalized)) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
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

    public String generateToken(UUID employeeId, String username, String roleName, UUID userId, boolean termsAccepted) {
        return generateToken(employeeId, username, roleName, userId, termsAccepted, 0L);
    }

    public String generateToken(
            UUID employeeId,
            String username,
            String roleName,
            UUID userId,
            boolean termsAccepted,
            long sessionVersion
    ) {
        var now = new Date();
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId != null ? userId.toString() : null)
                .claim("role", roleName)
                .claim("employeeId", employeeId != null ? employeeId.toString() : null)
                .claim("terms_accepted", termsAccepted)
                .claim("biometricConsentAccepted", termsAccepted)
                .claim("session_version", sessionVersion)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateToken(
            UUID employeeId,
            String username,
            String roleName,
            UUID userId,
            BiometricConsentStatus consentStatus,
            long sessionVersion
    ) {
        var now = new Date();
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId != null ? userId.toString() : null)
                .claim("role", roleName)
                .claim("employeeId", employeeId != null ? employeeId.toString() : null)
                .claim("terms_accepted", consentStatus.accepted())
                .claim("biometricConsentAccepted", consentStatus.accepted())
                .claim("biometricConsentVersion", consentStatus.acceptedVersion())
                .claim("biometricConsentHash", consentStatus.acceptedHash())
                .claim("session_version", sessionVersion)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateToken(
            UUID employeeId,
            String username,
            String roleName,
            UUID userId,
            BiometricConsentStatus consentStatus,
            long sessionVersion,
            UUID activeCompanyId
    ) {
        var now = new Date();
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId != null ? userId.toString() : null)
                .claim("role", roleName)
                .claim("employeeId", employeeId != null ? employeeId.toString() : null)
                .claim("terms_accepted", consentStatus.accepted())
                .claim("biometricConsentAccepted", consentStatus.accepted())
                .claim("biometricConsentVersion", consentStatus.acceptedVersion())
                .claim("biometricConsentHash", consentStatus.acceptedHash())
                .claim("session_version", sessionVersion)
                .claim("activeCompanyId", activeCompanyId != null ? activeCompanyId.toString() : null)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean getTermsAcceptedFromToken(String token) {
        var claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        // Se não houver a claim (tokens antigos), assume falso por segurança
        Object accepted = claims.get("terms_accepted");
        return accepted != null && (boolean) accepted;
    }

    public UUID getEmployeeIdFromToken(String token) {
        var claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        String employeeIdStr = claims.get("employeeId", String.class);
        if (employeeIdStr == null || employeeIdStr.isBlank()) {
            return null;
        }
        return UUID.fromString(employeeIdStr);
    }
    public UUID getUserIdFromToken(String token) {
        var claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        String userIdStr = claims.get("userId", String.class);
        if (userIdStr == null || userIdStr.isBlank()) {
            return null;
        }
        return UUID.fromString(userIdStr);
    }

    public long getSessionVersionFromToken(String token) {
        var claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        Object sessionVersion = claims.get("session_version");
        if (sessionVersion == null) {
            return 0L;
        }
        if (sessionVersion instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(sessionVersion));
    }

    public String getBiometricConsentVersionFromToken(String token) {
        var claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get("biometricConsentVersion", String.class);
    }

    public String getBiometricConsentHashFromToken(String token) {
        var claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get("biometricConsentHash", String.class);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public Date getExpirationFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
    }

    public Claims getClaimsFromExpiredToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return null;
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    public UUID getActiveCompanyIdFromToken(String token) {
        var claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        String companyIdStr = claims.get("activeCompanyId", String.class);
        if (companyIdStr == null || companyIdStr.isBlank()) {
            return null;
        }
        return UUID.fromString(companyIdStr);
    }
}
