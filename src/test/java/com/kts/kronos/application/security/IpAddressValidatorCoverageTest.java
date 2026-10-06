package com.kts.kronos.application.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import java.net.InetAddress;
import static org.mockito.Mockito.mockStatic;

/**
 * Supplemental coverage for IpAddressValidator:
 * - isInCidr: parts.length != 2 branch (L44/L45) — CIDR without "/"
 * - isInCidr: bitLen > 0 TRUE branch (L67-L69) — non-byte-aligned prefix length
 */
class IpAddressValidatorCoverageTest {

    @Test
    void isTrustedProxy_cidrWithoutSlash_returnsFalse() {
        // "192.168.1.0" → split("/") → ["192.168.1.0"] → length=1 ≠ 2 → return false (L44/45)
        assertFalse(IpAddressValidator.isTrustedProxy("192.168.1.1", List.of("192.168.1.0")));
    }

    @Test
    void isTrustedProxy_nonByteAlignedCidr_inRange_returnsTrue() {
        // /10 → byteLen=1, bitLen=2 > 0 → mask=0xC0=192
        // 10.64.0.1 & 0xC0 (byte[1]) = 64; 10.64.0.0 & 0xC0 = 64 → equal → true (L67-69)
        assertTrue(IpAddressValidator.isTrustedProxy("10.64.0.1", List.of("10.64.0.0/10")));
    }

    @Test
    void isTrustedProxy_nonByteAlignedCidr_outOfRange_returnsFalse() {
        // 10.128.0.1 → byte[1]=128; 128 & 0xC0=128 ≠ 64 → false (covers L67-69 FALSE return)
        assertFalse(IpAddressValidator.isTrustedProxy("10.128.0.1", List.of("10.64.0.0/10")));
    }

    @Test
    void isTrustedProxy_cidrWithTwoSlashes_returnsFalse() {
        // "10.0.0.0/8/extra" → split → 3 parts → length=3 ≠ 2 → return false (L44/45)
        assertFalse(IpAddressValidator.isTrustedProxy("10.0.0.1", List.of("10.0.0.0/8/extra")));
    }

    // L34-35: catch (Exception e) in isTrustedProxy — InetAddress.getByName throws on second call
    @Test
    void isTrustedProxy_inetAddressThrows_returnsFalse() throws Exception {
        InetAddress realAddr = InetAddress.getByName("192.168.1.1");
        try (MockedStatic<InetAddress> mockInet = mockStatic(InetAddress.class, Answers.CALLS_REAL_METHODS)) {
            // First call (in isValidIpAddress) → real result so it returns true
            // Second call (in isTrustedProxy try block) → throws exception → L34-35
            mockInet.when(() -> InetAddress.getByName("192.168.1.1"))
                    .thenReturn(realAddr)
                    .thenThrow(new java.net.UnknownHostException("simulated"));

            boolean result = IpAddressValidator.isTrustedProxy("192.168.1.1", List.of("10.0.0.0/8"));
            assertFalse(result);
        }
    }
}
