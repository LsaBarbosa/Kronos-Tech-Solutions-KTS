package com.kts.kronos.application.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class IpAddressValidatorTest {

    @Test
    @DisplayName("Validates IPv4 in CIDR range")
    void isTrustedProxy_ipv4InRange_returnsTrue() {
        assertTrue(IpAddressValidator.isTrustedProxy("192.168.1.5", Arrays.asList("192.168.1.0/24")));
    }

    @Test
    @DisplayName("Rejects IPv4 outside CIDR range")
    void isTrustedProxy_ipv4OutOfRange_returnsFalse() {
        assertFalse(IpAddressValidator.isTrustedProxy("192.168.2.5", Arrays.asList("192.168.1.0/24")));
    }

    @Test
    @DisplayName("Validates localhost 127.0.0.1")
    void isTrustedProxy_localhost_returnsTrue() {
        assertTrue(IpAddressValidator.isTrustedProxy("127.0.0.1", Arrays.asList("127.0.0.1/32")));
    }

    @Test
    @DisplayName("Validates IPv6 ::1")
    void isTrustedProxy_ipv6Localhost_returnsTrue() {
        assertTrue(IpAddressValidator.isTrustedProxy("::1", Arrays.asList("::1/128")));
    }

    @Test
    @DisplayName("Handles null remote address")
    void isTrustedProxy_nullRemoteAddr_returnsFalse() {
        assertFalse(IpAddressValidator.isTrustedProxy(null, Arrays.asList("127.0.0.1/32")));
    }

    @Test
    @DisplayName("Handles empty CIDR list")
    void isTrustedProxy_emptyCidrs_returnsFalse() {
        assertFalse(IpAddressValidator.isTrustedProxy("127.0.0.1", Collections.emptyList()));
    }

    @Test
    @DisplayName("Handles null CIDR list")
    void isTrustedProxy_nullCidrs_returnsFalse() {
        assertFalse(IpAddressValidator.isTrustedProxy("127.0.0.1", null));
    }

    @Test
    @DisplayName("Matches first IP in multiple CIDR entries")
    void isTrustedProxy_multipleEntries_matchesOne() {
        assertTrue(IpAddressValidator.isTrustedProxy("10.0.0.5", Arrays.asList("192.168.1.0/24", "10.0.0.0/8")));
    }

    @Test
    @DisplayName("Handles invalid CIDR format gracefully")
    void isTrustedProxy_invalidCidrFormat_returnsFalse() {
        assertFalse(IpAddressValidator.isTrustedProxy("192.168.1.1", Arrays.asList("invalid/cidr")));
    }

    @Test
    @DisplayName("Validates IPv4 /32 (single host)")
    void isTrustedProxy_ipv4Slash32_validatesExactHost() {
        assertTrue(IpAddressValidator.isTrustedProxy("192.168.1.100", Arrays.asList("192.168.1.100/32")));
        assertFalse(IpAddressValidator.isTrustedProxy("192.168.1.101", Arrays.asList("192.168.1.100/32")));
    }

    @Test
    @DisplayName("Validates IPv4 /8 (class A)")
    void isTrustedProxy_ipv4Slash8_validatesClassA() {
        assertTrue(IpAddressValidator.isTrustedProxy("10.255.255.255", Arrays.asList("10.0.0.0/8")));
        assertFalse(IpAddressValidator.isTrustedProxy("11.0.0.0", Arrays.asList("10.0.0.0/8")));
    }

    @Test
    @DisplayName("Validates IPv4 /16 (class B)")
    void isTrustedProxy_ipv4Slash16_validatesClassB() {
        assertTrue(IpAddressValidator.isTrustedProxy("172.16.255.255", Arrays.asList("172.16.0.0/16")));
        assertFalse(IpAddressValidator.isTrustedProxy("172.17.0.0", Arrays.asList("172.16.0.0/16")));
    }

    @Test
    @DisplayName("Handles blank remote address")
    void isTrustedProxy_blankRemoteAddr_returnsFalse() {
        assertFalse(IpAddressValidator.isTrustedProxy("", Arrays.asList("127.0.0.1/32")));
        assertFalse(IpAddressValidator.isTrustedProxy("   ", Arrays.asList("127.0.0.1/32")));
    }

    @Test
    @DisplayName("Validates IPv6 CIDR range")
    void isTrustedProxy_ipv6InRange_returnsTrue() {
        assertTrue(IpAddressValidator.isTrustedProxy("2001:db8::1", Arrays.asList("2001:db8::/32")));
    }

    @Test
    @DisplayName("Rejects invalid IP addresses")
    void isTrustedProxy_invalidIpAddress_returnsFalse() {
        assertFalse(IpAddressValidator.isTrustedProxy("not-an-ip", Arrays.asList("127.0.0.1/32")));
        assertFalse(IpAddressValidator.isTrustedProxy("999.999.999.999", Arrays.asList("127.0.0.1/32")));
    }
}
