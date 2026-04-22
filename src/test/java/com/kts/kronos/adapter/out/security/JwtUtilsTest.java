package com.kts.kronos.adapter.out.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilsTest {

    private static final String ISSUER = "kronos-test-api";
    private static final String AUDIENCE = "kronos-test-clients";
    private static final String CURRENT_KEY_ID = "test-current";
    private static final String PREVIOUS_KEY_ID = "test-previous";
    private static final String CURRENT_SECRET = base64Secret("01234567890123456789012345678901");
    private static final String PREVIOUS_SECRET = base64Secret("abcdefghijklmnopqrstuvwxyzABCDEF");

    @Test
    @DisplayName("deve aceitar JWT_SECRET Base64 com aspas externas")
    void shouldAcceptBase64SecretWrappedInQuotes() {
        String plainSecret = "this-is-a-jwt-secret-with-32-bytes!!!";
        String base64Secret = Base64.getEncoder()
                .encodeToString(plainSecret.getBytes(StandardCharsets.UTF_8));
        String wrappedSecret = "\"" + base64Secret + "\"";

        JwtUtils jwtUtils = new JwtUtils(wrappedSecret, 60_000L);
        UUID employeeId = UUID.randomUUID();
        String token = jwtUtils.generateToken(
                employeeId,
                "alice",
                "MANAGER",
                UUID.randomUUID(),
                true,
                3
        );

        assertTrue(jwtUtils.validateToken(token));
        assertEquals(employeeId, jwtUtils.getEmployeeIdFromToken(token));
    }

    @Test
    @DisplayName("deve expor claims principais de token gerado")
    void shouldReadPrincipalClaimsFromGeneratedToken() {
        JwtUtils jwtUtils = new JwtUtils(validSecret(), 60_000L);
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        String token = jwtUtils.generateToken(employeeId, "alice", "MANAGER", userId, true);

        assertEquals("alice", jwtUtils.getUsernameFromToken(token));
        assertEquals(employeeId, jwtUtils.getEmployeeIdFromToken(token));
        assertEquals(userId, jwtUtils.getUserIdFromToken(token));
        assertTrue(jwtUtils.getTermsAcceptedFromToken(token));
    }

    @Test
    @DisplayName("deve retornar null para claims opcionais ausentes")
    void shouldReturnNullForMissingOptionalUuidClaims() {
        JwtUtils jwtUtils = new JwtUtils("'" + validSecret() + "'", 60_000L);

        String token = jwtUtils.generateToken(null, "bob", "PARTNER", null, false);

        assertEquals("bob", jwtUtils.getUsernameFromToken(token));
        assertEquals(null, jwtUtils.getEmployeeIdFromToken(token));
        assertEquals(null, jwtUtils.getUserIdFromToken(token));
        assertFalse(jwtUtils.getTermsAcceptedFromToken(token));
    }

    @Test
    @DisplayName("deve rejeitar token inválido na validação")
    void shouldRejectInvalidToken() {
        JwtUtils jwtUtils = new JwtUtils(validSecret(), 60_000L);

        assertFalse(jwtUtils.validateToken("not-a-jwt"));
    }

    @Test
    @DisplayName("deve falhar quando JWT_SECRET está ausente")
    void shouldFailWhenSecretIsMissing() {
        assertThrows(IllegalArgumentException.class, () -> new JwtUtils(null, 60_000L));
        assertThrows(IllegalArgumentException.class, () -> new JwtUtils("   ", 60_000L));
    }

    @Test
    @DisplayName("deve avaliar segredo curto sem aspas")
    void shouldEvaluateShortUnquotedSecret() {
        assertThrows(IllegalArgumentException.class, () -> new JwtUtils("a", 60_000L));
    }

    @Test
    @DisplayName("deve falhar com mensagem clara quando JWT_SECRET não for Base64 válido")
    @DisplayName("deve falhar com mensagem clara quando JWT_SECRET nao for Base64 valido")
    void shouldFailWithClearMessageWhenSecretIsNotValidBase64() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new JwtUtils(
                        "secret-not-base64-%%%$",
                        60_000L,
                        ISSUER,
                        AUDIENCE,
                        CURRENT_KEY_ID,
                        "",
                        30L,
                        0L
                )
        );

        assertTrue(exception.getMessage().contains("JWT_SECRET"));
        assertTrue(exception.getMessage().contains("Base64"));
    }

    private static String validSecret() {
        return Base64.getEncoder()
                .encodeToString("this-is-a-jwt-secret-with-32-bytes!!!".getBytes(StandardCharsets.UTF_8));
    @Test
    @DisplayName("deve emitir token com issuer, audience, jti, nbf, kid e token_version")
    void shouldGenerateTokenWithRequiredSecurityClaims() {
        JwtUtils jwtUtils = jwtUtils();

        String token = jwtUtils.generateToken(UUID.randomUUID(), "alice", "MANAGER", UUID.randomUUID(), true, 7);

        var parsed = Jwts.parserBuilder()
                .setSigningKey(key(CURRENT_SECRET))
                .requireIssuer(ISSUER)
                .requireAudience(AUDIENCE)
                .build()
                .parseClaimsJws(token);

        assertEquals(CURRENT_KEY_ID, parsed.getHeader().getKeyId());
        assertEquals("alice", parsed.getBody().getSubject());
        assertEquals(7, parsed.getBody().get("token_version", Integer.class));
        assertNotNull(parsed.getBody().getId());
        assertNotNull(parsed.getBody().getNotBefore());
        assertTrue(jwtUtils.validateToken(token));
    }

    @Test
    @DisplayName("deve rejeitar token sem issuer ou sem audience obrigatorios")
    void shouldRejectTokenWithoutRequiredIssuerOrAudience() {
        JwtUtils jwtUtils = jwtUtils();

        String tokenWithoutIssuer = buildToken(CURRENT_KEY_ID, CURRENT_SECRET, null, AUDIENCE, 0);
        String tokenWithoutAudience = buildToken(CURRENT_KEY_ID, CURRENT_SECRET, ISSUER, null, 0);

        assertFalse(jwtUtils.validateToken(tokenWithoutIssuer));
        assertFalse(jwtUtils.validateToken(tokenWithoutAudience));
    }

    @Test
    @DisplayName("deve rejeitar token com issuer ou audience invalidos")
    void shouldRejectTokenWithInvalidIssuerOrAudience() {
        JwtUtils jwtUtils = jwtUtils();

        String tokenWithInvalidIssuer = buildToken(CURRENT_KEY_ID, CURRENT_SECRET, "invalid-issuer", AUDIENCE, 0);
        String tokenWithInvalidAudience = buildToken(CURRENT_KEY_ID, CURRENT_SECRET, ISSUER, "invalid-audience", 0);

        assertFalse(jwtUtils.validateToken(tokenWithInvalidIssuer));
        assertFalse(jwtUtils.validateToken(tokenWithInvalidAudience));
    }

    @Test
    @DisplayName("deve aceitar segredo anterior somente quando kid estiver configurado para rotacao")
    void shouldAcceptPreviousSecretOnlyWhenKeyIdIsConfiguredForRotation() {
        JwtUtils jwtUtils = new JwtUtils(
                CURRENT_SECRET,
                60_000L,
                ISSUER,
                AUDIENCE,
                CURRENT_KEY_ID,
                PREVIOUS_KEY_ID + ":" + PREVIOUS_SECRET,
                30L,
                0L
        );

        String tokenSignedWithPreviousSecret = buildToken(PREVIOUS_KEY_ID, PREVIOUS_SECRET, ISSUER, AUDIENCE, 0);
        String tokenSignedWithUnknownKey = buildToken("unknown-key", PREVIOUS_SECRET, ISSUER, AUDIENCE, 0);

        assertTrue(jwtUtils.validateToken(tokenSignedWithPreviousSecret));
        assertFalse(jwtUtils.validateToken(tokenSignedWithUnknownKey));
    }

    @Test
    @DisplayName("deve rejeitar token sem kid")
    void shouldRejectTokenWithoutKeyId() {
        JwtUtils jwtUtils = jwtUtils();

        String tokenWithoutKeyId = buildToken(null, CURRENT_SECRET, ISSUER, AUDIENCE, 0);

        assertFalse(jwtUtils.validateToken(tokenWithoutKeyId));
    }

    @Test
    @DisplayName("deve rejeitar token sem token_version")
    void shouldRejectTokenWithoutTokenVersion() {
        JwtUtils jwtUtils = jwtUtils();

        String tokenWithoutTokenVersion = buildToken(CURRENT_KEY_ID, CURRENT_SECRET, ISSUER, AUDIENCE, null);

        assertFalse(jwtUtils.validateToken(tokenWithoutTokenVersion));
    }

    @Test
    @DisplayName("deve falhar quando issuer nao estiver configurado por ambiente")
    void shouldFailWhenIssuerIsMissing() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new JwtUtils(
                        CURRENT_SECRET,
                        60_000L,
                        " ",
                        AUDIENCE,
                        CURRENT_KEY_ID,
                        "",
                        30L,
                        0L
                )
        );

        assertTrue(exception.getMessage().contains("JWT_ISSUER"));
    }

    private JwtUtils jwtUtils() {
        return new JwtUtils(
                CURRENT_SECRET,
                60_000L,
                ISSUER,
                AUDIENCE,
                CURRENT_KEY_ID,
                "",
                30L,
                0L
        );
    }

    private String buildToken(String keyId, String secret, String issuer, String audience, Integer tokenVersion) {
        Date now = new Date();
        var builder = Jwts.builder()
                .setSubject("alice")
                .claim("terms_accepted", true)
                .setId(UUID.randomUUID().toString())
                .setNotBefore(now)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + 60_000L));

        if (keyId != null) {
            builder.setHeaderParam("kid", keyId);
        }
        if (issuer != null) {
            builder.setIssuer(issuer);
        }
        if (audience != null) {
            builder.setAudience(audience);
        }
        if (tokenVersion != null) {
            builder.claim("token_version", tokenVersion);
        }

        return builder.signWith(key(secret), SignatureAlgorithm.HS256).compact();
    }

    private Key key(String secret) {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
    }

    private static String base64Secret(String plainSecret) {
        return Base64.getEncoder().encodeToString(plainSecret.getBytes(StandardCharsets.UTF_8));
    }
}
