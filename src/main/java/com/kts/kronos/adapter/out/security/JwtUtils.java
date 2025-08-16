package com.kts.kronos.adapter.out.security;

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
        byte[] secretBytes = Base64.getDecoder().decode(secret);
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expirationMs = expirationMs;
    }

    public String generateToken(UUID employeeId, String username, String roleName,  UUID userId) {
        var now = new Date();
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId != null ? userId.toString() : null)
                .claim("role", roleName)
                .claim("employeeId", employeeId != null ? employeeId.toString() : null)
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

    public String getRoleFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role", String.class);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
