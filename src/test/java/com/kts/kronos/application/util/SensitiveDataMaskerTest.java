package com.kts.kronos.application.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SensitiveDataMaskerTest {

    @Test
    void shouldMaskCpfPartially() {
        String cpf = "12345678901";
        String masked = SensitiveDataMasker.maskCpf(cpf);
        assertEquals("123.***.901", masked);
        assertFalse(masked.contains("4567"));
    }

    @Test
    void shouldMaskCpfWithFormatting() {
        String cpf = "123.456.789-01";
        String masked = SensitiveDataMasker.maskCpf(cpf);
        assertEquals("123.***.901", masked);
    }

    @Test
    void shouldReturnNullForNullCpf() {
        assertNull(SensitiveDataMasker.maskCpf(null));
    }

    @Test
    void shouldReturnBlankForBlankCpf() {
        assertEquals("", SensitiveDataMasker.maskCpf(""));
        assertEquals("   ", SensitiveDataMasker.maskCpf("   "));
    }

    @Test
    void shouldMaskEmailShowingOnlyFirstChar() {
        String email = "joao.silva@empresa.com.br";
        String masked = SensitiveDataMasker.maskEmail(email);
        assertEquals("j***@empresa.com.br", masked);
        assertFalse(masked.contains("oao"));
    }

    @Test
    void shouldReturnNullForNullEmail() {
        assertNull(SensitiveDataMasker.maskEmail(null));
    }

    @Test
    void shouldMaskTokenShowingFirstEightChars() {
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        String masked = SensitiveDataMasker.maskToken(token);
        assertEquals("eyJhbGc***", masked);
        assertFalse(masked.contains("SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"));
    }

    @Test
    void shouldReturnMaskedForShortToken() {
        String token = "short";
        String masked = SensitiveDataMasker.maskToken(token);
        assertEquals("***", masked);
    }

    @Test
    void shouldReturnNullForNullToken() {
        assertNull(SensitiveDataMasker.maskToken(null));
    }

    @Test
    void shouldMaskStoragePath() {
        String path = "bucket/company/employee/document.pdf";
        String masked = SensitiveDataMasker.maskStoragePath(path);
        assertEquals("[MASKED_PATH]", masked);
    }

    @Test
    void shouldReturnNullForNullStoragePath() {
        assertNull(SensitiveDataMasker.maskStoragePath(null));
    }

    @Test
    void shouldSanitizeDetailsMaskingCpf() {
        String details = "Employee CPF 12345678901 was verified";
        String sanitized = SensitiveDataMasker.sanitizeDetails(details);
        assertTrue(sanitized.contains("123.***.901"));
        assertFalse(sanitized.contains("12345678901"));
    }

    @Test
    void shouldSanitizeDetailsMaskingS3Path() {
        String details = "Document stored at s3://kronos-bucket/employee/doc.pdf for verification";
        String sanitized = SensitiveDataMasker.sanitizeDetails(details);
        assertTrue(sanitized.contains("[MASKED_PATH]"));
        assertFalse(sanitized.contains("s3://kronos-bucket"));
    }

    @Test
    void shouldSanitizeDetailsPreservingOtherText() {
        String details = "Standard audit log entry without sensitive data";
        String sanitized = SensitiveDataMasker.sanitizeDetails(details);
        assertEquals(details, sanitized);
    }

    @Test
    void shouldReturnNullForNullDetails() {
        assertNull(SensitiveDataMasker.sanitizeDetails(null));
    }

    @Test
    @DisplayName("sanitizeDetails masks long base64 strings")
    void shouldSanitizeDetailsMaskingBase64() {
        // Create a very long base64-like string (>200 chars)
        String longBase64 = "A".repeat(250); // 250 'A's in base64 alphabet
        String text = "Face image: " + longBase64 + " captured at checkin";
        String sanitized = SensitiveDataMasker.sanitizeDetails(text);
        assertFalse(sanitized.contains("AAA")); // At least some should be masked
        assertTrue(sanitized.contains("[BASE64_REDACTED]"));
    }

    @Test
    void shouldSanitizeDetailsMaskingLocalStoragePath() {
        String details = "Generated file persisted at storage/documents/employee/term.pdf";
        String sanitized = SensitiveDataMasker.sanitizeDetails(details);
        assertTrue(sanitized.contains("[MASKED_PATH]"));
        assertFalse(sanitized.contains("storage/documents/employee/term.pdf"));
    }

    @Test
    @DisplayName("maskFaceBase64 with null returns null")
    void maskFaceBase64_null_returnsNull() {
        assertNull(SensitiveDataMasker.maskFaceBase64(null));
    }

    @Test
    @DisplayName("maskFaceBase64 with blank returns blank")
    void maskFaceBase64_blank_returnsBlank() {
        assertEquals("", SensitiveDataMasker.maskFaceBase64(""));
        assertEquals("   ", SensitiveDataMasker.maskFaceBase64("   "));
    }

    @Test
    @DisplayName("maskFaceBase64 with valid base64 returns redacted placeholder with length")
    void maskFaceBase64_valid_returnsRedactedPlaceholder() {
        String base64Image = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
        String masked = SensitiveDataMasker.maskFaceBase64(base64Image);
        assertTrue(masked.startsWith("[FACE_IMAGE_REDACTED:"));
        assertTrue(masked.endsWith("chars]"));
        assertTrue(masked.contains(String.valueOf(base64Image.length())));
        assertFalse(masked.contains("iVBORw0KGgoAAAA"));
    }

    @Test
    @DisplayName("maskFaceBase64 masks very long base64 strings")
    void maskFaceBase64_longString_returnsRedactedWithLength() {
        String shortBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
        String veryLongBase64 = shortBase64.repeat(20);
        String masked = SensitiveDataMasker.maskFaceBase64(veryLongBase64);
        assertTrue(masked.contains(String.valueOf(veryLongBase64.length())));
        assertTrue(masked.contains("[FACE_IMAGE_REDACTED:"));
    }
}
