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

    @Test
    @DisplayName("generateToken com BiometricConsentStatus (6 args) deve incluir claims corretos")
    void shouldGenerateTokenWithBiometricConsentStatus() {
        JwtUtils jwtUtils = new JwtUtils(validSecret(), 60_000L);
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        com.kts.kronos.domain.model.BiometricConsentStatus consent =
                new com.kts.kronos.domain.model.BiometricConsentStatus(true, "v2", "hash123", "v2", "hash123", false);

        String token = jwtUtils.generateToken(employeeId, "carol", "MANAGER", userId, consent, 5L);

        assertTrue(jwtUtils.validateToken(token));
        assertEquals("carol", jwtUtils.getUsernameFromToken(token));
        assertEquals(employeeId, jwtUtils.getEmployeeIdFromToken(token));
        assertEquals(5L, jwtUtils.getSessionVersionFromToken(token));
        assertEquals("v2", jwtUtils.getBiometricConsentVersionFromToken(token));
        assertEquals("hash123", jwtUtils.getBiometricConsentHashFromToken(token));
    }

    @Test
    @DisplayName("generateToken com BiometricConsentStatus + activeCompanyId (7 args)")
    void shouldGenerateTokenWithBiometricConsentStatusAndCompanyId() {
        JwtUtils jwtUtils = new JwtUtils(validSecret(), 60_000L);
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        com.kts.kronos.domain.model.BiometricConsentStatus consent =
                new com.kts.kronos.domain.model.BiometricConsentStatus(false, null, null, "v1", "h1", true);

        String token = jwtUtils.generateToken(employeeId, "dave", "EMPLOYEE", userId, consent, 2L, companyId);

        assertTrue(jwtUtils.validateToken(token));
        assertEquals(companyId, jwtUtils.getActiveCompanyIdFromToken(token));
        assertEquals("dave", jwtUtils.getUsernameFromToken(token));
        assertEquals(2L, jwtUtils.getSessionVersionFromToken(token));
    }

    @Test
    @DisplayName("generateToken com BiometricConsentStatus + activeCompanyId null")
    void shouldGenerateTokenWithNullActiveCompanyId() {
        JwtUtils jwtUtils = new JwtUtils(validSecret(), 60_000L);
        com.kts.kronos.domain.model.BiometricConsentStatus consent =
                new com.kts.kronos.domain.model.BiometricConsentStatus(true, "v1", "h1", "v1", "h1", false);

        String token = jwtUtils.generateToken(null, "eve", "PARTNER", null, consent, 0L, null);

        assertTrue(jwtUtils.validateToken(token));
        org.junit.jupiter.api.Assertions.assertNull(jwtUtils.getActiveCompanyIdFromToken(token));
        org.junit.jupiter.api.Assertions.assertNull(jwtUtils.getEmployeeIdFromToken(token));
    }

    @Test
    @DisplayName("getTermsAcceptedFromToken retorna false quando claim ausente")
    void shouldReturnFalseWhenTermsAcceptedClaimAbsent() {
        // Gera token com termsAccepted = false
        JwtUtils jwtUtils = new JwtUtils(validSecret(), 60_000L);
        String token = jwtUtils.generateToken(null, "anon", "EMPLOYEE", null, false);
        assertFalse(jwtUtils.getTermsAcceptedFromToken(token));
    }

    @Test
    @DisplayName("getEmployeeIdFromToken retorna null para claim vazio")
    void shouldReturnNullEmployeeIdWhenClaimBlank() {
        JwtUtils jwtUtils = new JwtUtils(validSecret(), 60_000L);
        // null employeeId → claim is null → getEmployeeIdFromToken returns null
        String token = jwtUtils.generateToken(null, "user", "EMPLOYEE", UUID.randomUUID(), true);
        org.junit.jupiter.api.Assertions.assertNull(jwtUtils.getEmployeeIdFromToken(token));
    }

    @Test
    @DisplayName("getUserIdFromToken retorna null para claim null")
    void shouldReturnNullUserIdWhenClaimNull() {
        JwtUtils jwtUtils = new JwtUtils(validSecret(), 60_000L);
        String token = jwtUtils.generateToken(UUID.randomUUID(), "user", "EMPLOYEE", null, true);
        org.junit.jupiter.api.Assertions.assertNull(jwtUtils.getUserIdFromToken(token));
    }

    @Test
    @DisplayName("getSessionVersionFromToken retorna 0 quando claim ausente (token sem session_version)")
    void shouldReturnZeroSessionVersionWhenClaimAbsent() {
        JwtUtils jwtUtils = new JwtUtils(validSecret(), 60_000L);
        // generateToken with 5 args sets session_version = 0 (default overload)
        String token = jwtUtils.generateToken(UUID.randomUUID(), "user", "EMPLOYEE", UUID.randomUUID(), false);
        assertEquals(0L, jwtUtils.getSessionVersionFromToken(token));
    }

    @Test
    @DisplayName("getBiometricConsentVersionFromToken retorna null quando claim ausente")
    void shouldReturnNullBiometricConsentVersionWhenAbsent() {
        JwtUtils jwtUtils = new JwtUtils(validSecret(), 60_000L);
        // token without biometricConsentVersion claim
        String token = jwtUtils.generateToken(UUID.randomUUID(), "user", "EMPLOYEE", UUID.randomUUID(), true);
        org.junit.jupiter.api.Assertions.assertNull(jwtUtils.getBiometricConsentVersionFromToken(token));
    }

    @Test
    @DisplayName("getBiometricConsentHashFromToken retorna null quando claim ausente")
    void shouldReturnNullBiometricConsentHashWhenAbsent() {
        JwtUtils jwtUtils = new JwtUtils(validSecret(), 60_000L);
        String token = jwtUtils.generateToken(UUID.randomUUID(), "user", "EMPLOYEE", UUID.randomUUID(), true);
        org.junit.jupiter.api.Assertions.assertNull(jwtUtils.getBiometricConsentHashFromToken(token));
    }

    @Test
    @DisplayName("getClaimsFromExpiredToken retorna null para token valido")
    void shouldReturnNullClaimsFromValidToken() {
        JwtUtils jwtUtils = new JwtUtils(validSecret(), 60_000L);
        String token = jwtUtils.generateToken(UUID.randomUUID(), "user", "EMPLOYEE", UUID.randomUUID(), true);
        org.junit.jupiter.api.Assertions.assertNull(jwtUtils.getClaimsFromExpiredToken(token));
    }

    @Test
    @DisplayName("getClaimsFromExpiredToken retorna claims de token expirado")
    void shouldReturnClaimsFromExpiredToken() {
        // Cria token com expiracao no passado (expiration = -1000ms)
        JwtUtils jwtUtils = new JwtUtils(validSecret(), -1000L);
        UUID employeeId = UUID.randomUUID();
        String token = jwtUtils.generateToken(employeeId, "expired-user", "EMPLOYEE", UUID.randomUUID(), true);

        io.jsonwebtoken.Claims claims = jwtUtils.getClaimsFromExpiredToken(token);
        org.junit.jupiter.api.Assertions.assertNotNull(claims);
        assertEquals("expired-user", claims.getSubject());
    }

    @Test
    @DisplayName("getActiveCompanyIdFromToken retorna null quando claim ausente")
    void shouldReturnNullActiveCompanyIdWhenAbsent() {
        JwtUtils jwtUtils = new JwtUtils(validSecret(), 60_000L);
        // Token sem activeCompanyId claim
        String token = jwtUtils.generateToken(UUID.randomUUID(), "user", "EMPLOYEE", UUID.randomUUID(), true);
        org.junit.jupiter.api.Assertions.assertNull(jwtUtils.getActiveCompanyIdFromToken(token));
    }

    @Test
    @DisplayName("getExpirationFromToken retorna data de expiracao do token")
    void shouldReturnExpirationFromToken() {
        JwtUtils jwtUtils = new JwtUtils(validSecret(), 60_000L);
        String token = jwtUtils.generateToken(UUID.randomUUID(), "user", "EMPLOYEE", UUID.randomUUID(), true);
        java.util.Date expiration = jwtUtils.getExpirationFromToken(token);
        org.junit.jupiter.api.Assertions.assertNotNull(expiration);
        assertTrue(expiration.after(new java.util.Date()));
    }

    @Test
    @DisplayName("hasMatchingWrappingQuotes: aspas simples sao removidas")
    void shouldAcceptBase64SecretWrappedInSingleQuotes() {
        String base64Secret = validSecret();
        char sq = 39; // single quote char
        String wrappedSecret = sq + base64Secret + sq;
        JwtUtils jwtUtils = new JwtUtils(wrappedSecret, 60_000L);
        String token = jwtUtils.generateToken(UUID.randomUUID(), "user", "EMPLOYEE", UUID.randomUUID(), false);
        assertTrue(jwtUtils.validateToken(token));
    }

}
