package com.kts.kronos.adapter.out.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilsTest {

    private static final String SECRET = Base64.getEncoder().encodeToString("12345678901234567890123456789012".getBytes());

    @Test
    void shouldGenerateAndReadTokenClaims() {
        var jwtUtils = new JwtUtils(SECRET, 60_000L, "issuer", "aud");
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        String token = jwtUtils.generateToken(employeeId, "john", "MANAGER", userId, true);

        assertThat(jwtUtils.validateToken(token)).isTrue();
        assertThat(jwtUtils.getUsernameFromToken(token)).isEqualTo("john");
        assertThat(jwtUtils.getRoleFromToken(token)).isEqualTo("MANAGER");
        assertThat(jwtUtils.getEmployeeIdFromToken(token)).isEqualTo(employeeId);
        assertThat(jwtUtils.getUserIdFromToken(token)).isEqualTo(userId);
        assertThat(jwtUtils.getTermsAcceptedFromToken(token)).isTrue();
        assertThat(jwtUtils.getValidClaims(token)).isPresent();
    }

    @Test
    void shouldHandleNullIdsAndInvalidToken() {
        var jwtUtils = new JwtUtils(SECRET, 60_000L, "issuer", "aud");
        String token = jwtUtils.generateToken(null, "john", "PARTNER", null, false);

        assertThat(jwtUtils.getEmployeeIdFromToken(token)).isNull();
        assertThat(jwtUtils.getUserIdFromToken(token)).isNull();
        assertThat(jwtUtils.getTermsAcceptedFromToken(token)).isFalse();

        String tokenWithWrongAudience = Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setIssuer("issuer")
                .setAudience("other-aud")
                .setSubject("john")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 10_000))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET)), SignatureAlgorithm.HS256)
                .compact();

        assertThat(jwtUtils.validateToken(tokenWithWrongAudience)).isFalse();
        assertThat(jwtUtils.getValidClaims(tokenWithWrongAudience)).isEmpty();
    }

    @Test
    void shouldRejectTokenWithoutJtiAndThrowOnDirectClaimsAccess() {
        var jwtUtils = new JwtUtils(SECRET, 60_000L, "issuer", "aud");

        String tokenWithoutJti = Jwts.builder()
                .setIssuer("issuer")
                .setAudience("aud")
                .setSubject("john")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 10_000))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET)), SignatureAlgorithm.HS256)
                .compact();

        assertThat(jwtUtils.validateToken(tokenWithoutJti)).isFalse();
        assertThat(jwtUtils.getValidClaims(tokenWithoutJti)).isEmpty();
        assertThatThrownBy(() -> jwtUtils.getUsernameFromToken(tokenWithoutJti)).isInstanceOf(RuntimeException.class);
    }
}
