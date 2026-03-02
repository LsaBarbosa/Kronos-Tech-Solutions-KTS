package com.kts.kronos.adapter.out.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtUtils {
    private final Key key;
    private final long expirationMs;
    private final String issuer;
    private final String audience;

    public JwtUtils(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expirationMs,
            @Value("${jwt.issuer:kronos-api}") String issuer,
            @Value("${jwt.audience:kronos-clients}") String audience
    ) {
        byte[] secretBytes = Base64.getDecoder().decode(secret);
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expirationMs = expirationMs;
        this.issuer = issuer;
        this.audience = audience;
    }

    public String generateToken(UUID employeeId, String username, String roleName, UUID userId, boolean termsAccepted) {
        var now = new Date();
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setIssuer(issuer)
                .setAudience(audience)
                .setSubject(username)
                .claim("userId", userId != null ? userId.toString() : null)
                .claim("role", roleName)
                .claim("employeeId", employeeId != null ? employeeId.toString() : null)
                .claim("terms_accepted", termsAccepted)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean getTermsAcceptedFromToken(String token) {
        var claims = parseClaims(token);

        var accepted = claims.get("terms_accepted");
        return accepted != null && (boolean) accepted;
    }

    public UUID getEmployeeIdFromToken(String token) {
        var claims = parseClaims(token);

        String employeeIdStr = claims.get("employeeId", String.class);
        if (employeeIdStr == null || employeeIdStr.isBlank()) {
            return null;
        }
        return UUID.fromString(employeeIdStr);
    }

    public UUID getUserIdFromToken(String token) {
        var claims = parseClaims(token);

        String userIdStr = claims.get("userId", String.class);
        if (userIdStr == null || userIdStr.isBlank()) {
            return null;
        }
        return UUID.fromString(userIdStr);
    }

    public String getRoleFromToken(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public Optional<Claims> getValidClaims(String token) {
        try {
            return Optional.of(parseClaims(token));
        } catch (JwtException e) {
            return Optional.empty();
        }
    }

    private Claims parseClaims(String token) {
        var claims = Jwts.parser()
                .verifyWith((SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (claims.getId() == null || claims.getId().isBlank()) {
            throw new JwtException("JWT sem jti");
        }

        var tokenAudience = claims.getAudience();
        if (tokenAudience == null || tokenAudience.isEmpty() || !tokenAudience.contains(audience)) {
            throw new JwtException("JWT com audience inválida");
        }

        return claims;
    }
}
