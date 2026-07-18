package com.kts.kronos.application.service.anonymization.util;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;

class AnonymizationUtilCoverageTest {

    // L35-36: NoSuchAlgorithmException catch in hashSha256 → RuntimeException
    @Test
    void anonymizeCpf_shaUnavailable_throwsRuntimeException() {
        try (MockedStatic<MessageDigest> mdStatic = mockStatic(MessageDigest.class, Answers.CALLS_REAL_METHODS)) {
            mdStatic.when(() -> MessageDigest.getInstance("SHA-256"))
                    .thenThrow(new NoSuchAlgorithmException("SHA-256 not available"));

            assertThatThrownBy(() -> AnonymizationUtil.anonymizeCpf(UUID.randomUUID()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("SHA-256");
        }
    }
}
