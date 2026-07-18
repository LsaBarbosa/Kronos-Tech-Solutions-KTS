package com.kts.kronos.application.service;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;

class TimesheetSignatureCoverage2Test {

    // L655-656: NoSuchAlgorithmException catch in static sha256Hex
    @Test
    void sha256Hex_throwsIllegalState_whenMessageDigestUnavailable() {
        try (MockedStatic<MessageDigest> mockMd = mockStatic(MessageDigest.class, Answers.CALLS_REAL_METHODS)) {
            mockMd.when(() -> MessageDigest.getInstance("SHA-256"))
                  .thenThrow(new NoSuchAlgorithmException("SHA-256 unavailable"));

            assertThrows(IllegalStateException.class,
                    () -> TimesheetSignatureService.sha256Hex(new byte[]{1, 2, 3}));
        }
    }
}
