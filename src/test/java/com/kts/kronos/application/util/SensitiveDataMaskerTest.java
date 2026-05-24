package com.kts.kronos.application.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SensitiveDataMaskerTest {

    @Test
    void maskCpf_shouldMaskValidCpf() {
        String cpf = "12345678901";
        String masked = SensitiveDataMasker.maskCpf(cpf);
        assertEquals("***.456.789-**", masked);
        assertFalse(masked.contains("12345678901"));
    }

    @Test
    void maskCpf_shouldHandleFormattedCpf() {
        String cpf = "123.456.789-01";
        String masked = SensitiveDataMasker.maskCpf(cpf);
        assertEquals("***.456.789-**", masked);
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
    void maskCpf_shouldNotExposeRawCpf() {
        String cpf = "12345678901";

        String masked = SensitiveDataMasker.maskCpf(cpf);

        assertNotEquals(cpf, masked);
        assertFalse(masked.contains(cpf));
        assertTrue(masked.startsWith("***."));
        assertTrue(masked.endsWith("-**"));
    }

    @Test
    void maskCpf_shouldHandleNullBlankAndInvalidCpf() {
        assertNull(SensitiveDataMasker.maskCpf(null));
        assertEquals("", SensitiveDataMasker.maskCpf(""));
        assertEquals("   ", SensitiveDataMasker.maskCpf("   "));
        assertEquals("***", SensitiveDataMasker.maskCpf("123"));
        assertEquals("***", SensitiveDataMasker.maskCpf("abc"));
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
    void maskStorageReference_shouldNotExposeRawStoragePath() {
        String raw = "company/123/employee/456/BIOMETRIC/2026/05/file.jpg";

        String masked = SensitiveDataMasker.maskStorageReference(raw);

        assertTrue(masked.startsWith("storage_ref_sha256="));
        assertTrue(masked.contains(",length="));
        assertFalse(masked.contains("company"));
        assertFalse(masked.contains("employee"));
        assertFalse(masked.contains("123"));
        assertFalse(masked.contains("456"));
        assertFalse(masked.contains("file.jpg"));
        assertNotEquals(raw, masked);
    }

    @Test
    void maskStorageReference_shouldReturnStableHashForSameValue() {
        String raw = "company/123/employee/456/BIOMETRIC/2026/05/file.jpg";

        String first = SensitiveDataMasker.maskStorageReference(raw);
        String second = SensitiveDataMasker.maskStorageReference(raw);

        assertEquals(first, second);
    }

    @Test
    void maskStorageReference_shouldHandleNullAndBlank() {
        assertEquals("storage_ref_empty", SensitiveDataMasker.maskStorageReference(null));
        assertEquals("storage_ref_empty", SensitiveDataMasker.maskStorageReference(""));
        assertEquals("storage_ref_empty", SensitiveDataMasker.maskStorageReference("   "));
    }

    @Test
    void shouldSanitizeDetailsMaskingCpf() {
        String details = "Employee CPF 12345678901 was verified";
        String sanitized = SensitiveDataMasker.sanitizeDetails(details);
        assertTrue(sanitized.contains("***.456.789-**"));
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

    @Test
    @DisplayName("sanitizeDetails masks /uploads/ path")
    void shouldSanitizeDetailsWithUploadsPath() {
        String details = "User uploaded file at /uploads/employee/document.pdf";
        String sanitized = SensitiveDataMasker.sanitizeDetails(details);
        assertTrue(sanitized.contains("[MASKED_PATH]"));
        assertFalse(sanitized.contains("/uploads/employee/document.pdf"));
        assertFalse(sanitized.contains("document.pdf"));
    }

    @Test
    @DisplayName("sanitizeDetails handles blank/whitespace text")
    void shouldSanitizeDetailsWithBlankText() {
        String blankText = "   ";
        String sanitized = SensitiveDataMasker.sanitizeDetails(blankText);
        assertEquals(blankText, sanitized);
    }

    @Test
    @DisplayName("sanitizeDetails masks bucket/ path")
    void shouldSanitizeDetailsWithBucketPath() {
        String details = "File at bucket/company/doc.pdf";
        String sanitized = SensitiveDataMasker.sanitizeDetails(details);
        assertTrue(sanitized.contains("[MASKED_PATH]"));
        assertFalse(sanitized.contains("bucket/company/doc.pdf"));
        assertFalse(sanitized.contains("company"));
    }

    @Test
    @DisplayName("sanitizeDetails preserves blank and returns blank")
    void shouldSanitizeDetailsReturnBlankForBlankInput() {
        assertEquals("", SensitiveDataMasker.sanitizeDetails(""));
        assertEquals("   ", SensitiveDataMasker.sanitizeDetails("   "));
    }

    @Test
    @DisplayName("sanitizeDetails masks multiple email addresses in one string")
    void shouldSanitizeDetailsWithMultipleEmails() {
        String details = "Contacted user1@company.com and user2@company.com for verification";
        String sanitized = SensitiveDataMasker.sanitizeDetails(details);
        assertFalse(sanitized.contains("user1@company.com"));
        assertFalse(sanitized.contains("user2@company.com"));
        assertTrue(sanitized.contains("u***@company.com"));
    }

    @Test
    @DisplayName("sanitizeDetails masks email with special characters")
    void shouldSanitizeDetailsWithComplexEmail() {
        String details = "Email: joao.silva+tag@empresa.com.br was used";
        String sanitized = SensitiveDataMasker.sanitizeDetails(details);
        assertFalse(sanitized.contains("joao.silva+tag@empresa.com.br"));
        assertTrue(sanitized.contains("j***@empresa.com.br"));
    }

    @Test
    @DisplayName("sanitizeDetails masks multiple CPFs")
    void shouldSanitizeDetailsWithMultipleCpfs() {
        String details = "Employees CPF 12345678901 and 98765432100 were verified";
        String sanitized = SensitiveDataMasker.sanitizeDetails(details);
        assertFalse(sanitized.contains("12345678901"));
        assertFalse(sanitized.contains("98765432100"));
        assertEquals(2, sanitized.split("\\*\\*\\*\\.").length - 1);
    }

    @Test
    @DisplayName("sanitizeDetails masks JWT in different formats")
    void shouldSanitizeDetailsWithJwtVariations() {
        String token1 = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ";
        String token2 = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        String details = "Auth tokens: " + token1 + " and " + token2;
        String sanitized = SensitiveDataMasker.sanitizeDetails(details);
        assertFalse(sanitized.contains("TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ"));
        assertFalse(sanitized.contains("SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"));
    }

    @Test
    @DisplayName("sanitizeDetails masks s3 paths")
    void shouldSanitizeDetailsWithS3Paths() {
        String details = "Files at s3://bucket1/file1.pdf stored";
        String sanitized = SensitiveDataMasker.sanitizeDetails(details);
        assertTrue(sanitized.contains("[MASKED_PATH]"));
        assertFalse(sanitized.contains("s3://bucket1/file1.pdf"));
        assertFalse(sanitized.contains("bucket1"));
    }

    @Test
    @DisplayName("maskEmail preserves domain structure")
    void shouldMaskEmailButShowDomain() {
        String email = "very.long.name.with.dots@example.com.br";
        String masked = SensitiveDataMasker.maskEmail(email);
        assertTrue(masked.startsWith("v***@"));
        assertTrue(masked.endsWith("example.com.br"));
        assertFalse(masked.contains("very.long.name"));
    }

    @Test
    @DisplayName("maskEmail handles email with numeric prefix")
    void shouldMaskEmailWithNumericPrefix() {
        String email = "123admin@company.com";
        String masked = SensitiveDataMasker.maskEmail(email);
        assertTrue(masked.startsWith("1***@"));
        assertFalse(masked.contains("23admin"));
    }

    @Test
    void shouldReturnBlankForBlankEmail() {
        assertEquals("", SensitiveDataMasker.maskEmail(""));
        assertEquals("   ", SensitiveDataMasker.maskEmail("   "));
    }

    @Test
    void shouldReturnBlankForBlankToken() {
        assertEquals("", SensitiveDataMasker.maskToken(""));
        assertEquals("   ", SensitiveDataMasker.maskToken("   "));
    }

    @Test
    void shouldReturnBlankForBlankStoragePath() {
        assertEquals("", SensitiveDataMasker.maskStoragePath(""));
        assertEquals("   ", SensitiveDataMasker.maskStoragePath("   "));
    }

    @Test
    @DisplayName("sanitizeDetails does not mask when no sensitive data present")
    void shouldPreservePlainTextWithoutSensitiveData() {
        String plainText = "This is a plain text log entry with normal information and numbers 12345 but no sensitive data patterns";
        String sanitized = SensitiveDataMasker.sanitizeDetails(plainText);
        assertEquals(plainText, sanitized);
    }

    @Test
    @DisplayName("sanitizeDetails handles empty string")
    void shouldSanitizeEmptyString() {
        String result = SensitiveDataMasker.sanitizeDetails("");
        assertEquals("", result);
    }
}
