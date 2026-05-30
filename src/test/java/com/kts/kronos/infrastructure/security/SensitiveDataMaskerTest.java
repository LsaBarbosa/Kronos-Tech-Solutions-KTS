package com.kts.kronos.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SensitiveDataMaskerTest {

    @Test
    void shouldMaskCpfFormatted() {
        String input = "CPF: 123.456.789-10";
        String masked = SensitiveDataMasker.maskSensitiveData(input);
        assertNotNull(masked);
        assertFalse(masked.contains("123.456.789-10"));
        assertTrue(masked.contains("***"));
    }

    @Test
    void shouldMaskCpfUnformatted() {
        String input = "cpf=12345678901";
        String masked = SensitiveDataMasker.maskSensitiveData(input);
        assertNotNull(masked);
        assertFalse(masked.contains("12345678901"));
        assertTrue(masked.contains("***"));
    }

    @Test
    void shouldMaskCpfInQueryString() {
        String input = "/employee/check-cpf?cpf=12345678901";
        String masked = SensitiveDataMasker.maskSensitiveData(input);
        assertNotNull(masked);
        assertFalse(masked.contains("12345678901"));
        assertTrue(masked.contains("***"));
    }

    @Test
    void shouldMaskPis() {
        String input = "PIS: 123.45678.90-1";
        String masked = SensitiveDataMasker.maskSensitiveData(input);
        assertNotNull(masked);
        assertFalse(masked.contains("123.45678.90-1"));
        assertTrue(masked.contains("***"));
    }

    @Test
    void shouldMaskJwt() {
        String jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U";
        String input = "Token: " + jwt;
        String masked = SensitiveDataMasker.maskSensitiveData(input);
        assertNotNull(masked);
        assertFalse(masked.contains("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0"));
        assertTrue(masked.contains("[MASKED]"));
    }

    @Test
    void shouldMaskEmail() {
        String input = "Email: user@example.com";
        String masked = SensitiveDataMasker.maskSensitiveData(input);
        assertNotNull(masked);
        assertFalse(masked.contains("user@example.com"));
        assertTrue(masked.contains("****@***"));
    }

    @Test
    void shouldMaskPhoneNumber() {
        String input = "Phone: (11) 99999-9999";
        String masked = SensitiveDataMasker.maskSensitiveData(input);
        assertNotNull(masked);
        assertFalse(masked.contains("(11) 99999-9999"));
        assertTrue(masked.contains("(##)"));
    }

    @Test
    void shouldMaskPassword() {
        String input = "password=secretPassword123";
        String masked = SensitiveDataMasker.maskSensitiveData(input);
        assertNotNull(masked);
        assertFalse(masked.contains("secretPassword123"));
        assertTrue(masked.contains("[MASKED]"));
    }

    @Test
    void shouldMaskApiKey() {
        String input = "api_key=sk_live_4eC39HqLyjWDarht7kXiL";
        String masked = SensitiveDataMasker.maskSensitiveData(input);
        assertNotNull(masked);
        assertFalse(masked.contains("sk_live_4eC39HqLyjWDarht7kXiL"));
        assertTrue(masked.contains("[MASKED]"));
    }

    @Test
    void shouldMaskCoordinates() {
        String input = "Location: -23.550520, -46.633308";
        String masked = SensitiveDataMasker.maskSensitiveData(input);
        assertNotNull(masked);
        assertFalse(masked.contains("-23.550520"));
        assertTrue(masked.contains("[MASKED_COORDINATES]"));
    }

    @Test
    void shouldMaskResetToken() {
        String input = "resetToken=abc123def456xyz789";
        String masked = SensitiveDataMasker.maskSensitiveData(input);
        assertNotNull(masked);
        assertFalse(masked.contains("abc123def456xyz789"));
        assertTrue(masked.contains("[MASKED]"));
    }

    @Test
    void shouldMaskFaceImageBase64() {
        String faceData = "A".repeat(150);
        String input = "faceImageBase64=" + faceData;
        String masked = SensitiveDataMasker.maskSensitiveData(input);
        assertNotNull(masked);
        assertFalse(masked.contains(faceData));
        assertTrue(masked.contains("[MASKED]"));
    }

    @Test
    void shouldHandleNullInput() {
        assertNull(SensitiveDataMasker.maskSensitiveData(null));
    }

    @Test
    void shouldDetectSensitiveData() {
        assertTrue(SensitiveDataMasker.containsSensitiveData("CPF: 123.456.789-10"));
        assertTrue(SensitiveDataMasker.containsSensitiveData("password=secret123"));
        assertTrue(SensitiveDataMasker.containsSensitiveData("api_key=sk_123456"));
        assertTrue(SensitiveDataMasker.containsSensitiveData("resetToken=abc123"));
        assertTrue(SensitiveDataMasker.containsSensitiveData("PIS: 123.45678.90-1"));
        assertFalse(SensitiveDataMasker.containsSensitiveData("normal message with no sensitive data"));
    }

    @Test
    void shouldMaskMultipleSensitiveDataInOneMessage() {
        String input = "User email: john@example.com with CPF: 123.456.789-10 and phone: (11) 99999-9999";
        String masked = SensitiveDataMasker.maskSensitiveData(input);
        assertNotNull(masked);
        assertFalse(masked.contains("john@example.com"));
        assertFalse(masked.contains("123.456.789-10"));
        assertFalse(masked.contains("(11) 99999-9999"));
    }

    @Test
    void shouldMaskCpfSpecifically() {
        String cpf = "123.456.789-10";
        String masked = SensitiveDataMasker.maskCpf(cpf);
        assertNotNull(masked);
        assertTrue(masked.contains("-10"));
        assertTrue(masked.contains("***"));
    }

    @Test
    void shouldMaskInvalidCpf() {
        String invalid = "12345";
        String masked = SensitiveDataMasker.maskCpf(invalid);
        assertEquals("[INVALID_CPF]", masked);
    }

    @Test
    void shouldMaskEmailSpecifically() {
        String email = "user@example.com";
        String masked = SensitiveDataMasker.maskEmail(email);
        assertEquals("****@***", masked);
    }

    @Test
    void shouldMaskInvalidEmail() {
        String masked = SensitiveDataMasker.maskEmail("invalid");
        assertEquals("[INVALID_EMAIL]", masked);
    }

    @Test
    void shouldMaskPhoneSpecifically() {
        String phone = "(11) 99999-9999";
        String masked = SensitiveDataMasker.maskPhone(phone);
        assertEquals("(##)#####-****", masked);
    }

    @Test
    void shouldMaskInvalidPhone() {
        String masked = SensitiveDataMasker.maskPhone("123");
        assertEquals("[INVALID_PHONE]", masked);
    }

    @Test
    void shouldMaskJwtSpecifically() {
        String jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U";
        String masked = SensitiveDataMasker.maskJwt(jwt);
        assertEquals("eyJ[MASKED]", masked);
    }

    @Test
    void shouldMaskInvalidJwt() {
        String masked = SensitiveDataMasker.maskJwt("short");
        assertEquals("[INVALID_JWT]", masked);
    }
}
