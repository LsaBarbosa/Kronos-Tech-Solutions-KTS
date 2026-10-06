package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.BlacklistedTokenRepository;
import com.kts.kronos.application.config.KronosRedisProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistCoverage3Test {

    @Mock
    private BlacklistedTokenRepository repository;

    private TokenBlacklistProviderImpl provider;

    @BeforeEach
    void setUp() {
        provider = new TokenBlacklistProviderImpl(repository);
        KronosRedisProperties disabled = new KronosRedisProperties();
        disabled.setEnabled(false);
        ReflectionTestUtils.setField(provider, "redisProperties", disabled);
    }

    // L115-116: NoSuchAlgorithmException catch in hashToken
    @Test
    void hashToken_throwsIllegalState_whenSha256NotAvailable() {
        try (MockedStatic<MessageDigest> mockMd = mockStatic(MessageDigest.class)) {
            mockMd.when(() -> MessageDigest.getInstance("SHA-256"))
                  .thenThrow(new NoSuchAlgorithmException("SHA-256 unavailable"));

            assertThrows(IllegalStateException.class, () -> provider.isBlacklisted("any-token"));
        }
    }
}
