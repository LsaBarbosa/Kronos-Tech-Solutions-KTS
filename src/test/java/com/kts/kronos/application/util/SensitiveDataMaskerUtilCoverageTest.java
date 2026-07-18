package com.kts.kronos.application.util;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

class SensitiveDataMaskerUtilCoverageTest {

    // L133: PIS masking in sanitizeDetails — input containing PIS number (not CPF pattern)
    @Test
    void sanitizeDetails_withPisNumber_masksPis() {
        // PIS pattern: \d{3}.\d{5}.\d{2}-\d{1} — 3+5+2+1 = 11 digits
        // "123.45678.90-1" does NOT match CPF (CPF middle group is 3 digits, PIS is 5)
        // → CPF masking skips it, PIS masking at L133 runs
        String result = SensitiveDataMasker.sanitizeDetails("Empregado PIS 123.45678.90-1 ok");
        assertFalse(result.contains("123.45678.90-1"),
                "PIS number should have been masked by L133");
    }

    // L162-163: sha256Hex private — NoSuchAlgorithmException catch via maskStorageReference
    @Test
    void maskStorageReference_throwsIllegalState_whenMessageDigestUnavailable() {
        try (MockedStatic<MessageDigest> mockMd = mockStatic(MessageDigest.class, Answers.CALLS_REAL_METHODS)) {
            mockMd.when(() -> MessageDigest.getInstance("SHA-256"))
                  .thenThrow(new NoSuchAlgorithmException("SHA-256 unavailable"));

            assertThrows(IllegalStateException.class,
                    () -> SensitiveDataMasker.maskStorageReference("s3://bucket/key.pdf"));
        }
    }
}
