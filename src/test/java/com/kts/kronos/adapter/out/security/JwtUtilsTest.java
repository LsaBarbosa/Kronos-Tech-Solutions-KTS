package com.kts.kronos.adapter.out.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilsTest {

    @Test
    @DisplayName("deve aceitar JWT_SECRET Base64 com aspas externas")
    void shouldAcceptBase64SecretWrappedInQuotes() {
        String plainSecret = "this-is-a-jwt-secret-with-32-bytes!!!";
        String base64Secret = Base64.getEncoder()
                .encodeToString(plainSecret.getBytes(StandardCharsets.UTF_8));
        String wrappedSecret = "\"" + base64Secret + "\"";

        JwtUtils jwtUtils = new JwtUtils(wrappedSecret, 60_000L);
        String token = jwtUtils.generateToken(
                UUID.randomUUID(),
                "alice",
                "MANAGER",
                UUID.randomUUID(),
                true
        );

        assertTrue(jwtUtils.validateToken(token));
        assertTrue(jwtUtils.getTermsAcceptedFromToken(token));
    }

    @Test
    @DisplayName("deve expor claims principais de token gerado")
    void shouldReadPrincipalClaimsFromGeneratedToken() {
        JwtUtils jwtUtils = new JwtUtils(validSecret(), 60_000L);
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        String token = jwtUtils.generateToken(employeeId, "alice", "MANAGER", userId, true, 3L);

        assertEquals("alice", jwtUtils.getUsernameFromToken(token));
        assertEquals(employeeId, jwtUtils.getEmployeeIdFromToken(token));
        assertEquals(userId, jwtUtils.getUserIdFromToken(token));
        assertEquals(3L, jwtUtils.getSessionVersionFromToken(token));
        assertTrue(jwtUtils.getTermsAcceptedFromToken(token));
    }

    @Test
    @DisplayName("deve incluir session_version no JWT")
    void jwt_shouldIncludeSessionVersionClaim() {
        JwtUtils jwtUtils = new JwtUtils(validSecret(), 60_000L);

        String token = jwtUtils.generateToken(UUID.randomUUID(), "alice", "MANAGER", UUID.randomUUID(), true, 7L);

        assertEquals(7L, jwtUtils.getSessionVersionFromToken(token));
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
    void shouldFailWithClearMessageWhenSecretIsNotValidBase64() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new JwtUtils("secret-not-base64-%%%$", 60_000L)
        );

        assertTrue(exception.getMessage().contains("JWT_SECRET"));
        assertTrue(exception.getMessage().contains("Base64"));
    }

    private static String validSecret() {
        return Base64.getEncoder()
                .encodeToString("this-is-a-jwt-secret-with-32-bytes!!!".getBytes(StandardCharsets.UTF_8));
    }
}
