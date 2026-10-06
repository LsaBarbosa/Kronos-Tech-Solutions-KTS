package com.kts.kronos.application.security;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;

import javax.crypto.Mac;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;

class PrivacyLogReferenceServiceCoverage2Test {

    // L87-88: catch (Exception ex) in hmac() when Mac.getInstance throws
    @Test
    void hmac_throwsIllegalState_whenMacUnavailable() {
        var service = new PrivacyLogReferenceService("test-hmac-secret-32bytes!!!");

        try (MockedStatic<Mac> mockMac = mockStatic(Mac.class, Answers.CALLS_REAL_METHODS)) {
            mockMac.when(() -> Mac.getInstance("HmacSHA256"))
                   .thenThrow(new NoSuchAlgorithmException("HmacSHA256 unavailable"));

            assertThrows(IllegalStateException.class, () -> service.employeeRef(UUID.randomUUID()));
        }
    }
}
