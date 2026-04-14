package com.kts.kronos.adapter.out.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

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
    @DisplayName("deve falhar com mensagem clara quando JWT_SECRET não for Base64 válido")
    void shouldFailWithClearMessageWhenSecretIsNotValidBase64() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new JwtUtils("secret-not-base64-%%%$", 60_000L)
        );

        assertTrue(exception.getMessage().contains("JWT_SECRET"));
        assertTrue(exception.getMessage().contains("Base64"));
    }
}
