package com.kts.kronos.application.security;

import java.net.InetAddress;
import java.util.List;

public class IpAddressValidator {
    private IpAddressValidator() {}

    public static boolean isTrustedProxy(String remoteAddr, List<String> trustedCidrs) {
        if (remoteAddr == null || remoteAddr.isBlank() || trustedCidrs == null || trustedCidrs.isEmpty()) {
            return false;
        }

        try {
            InetAddress remote = InetAddress.getByName(remoteAddr);
            for (String cidr : trustedCidrs) {
                if (isInCidr(remote, cidr)) {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }

        return false;
    }

    private static boolean isInCidr(InetAddress address, String cidr) {
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) {
                return false;
            }

            InetAddress network = InetAddress.getByName(parts[0]);
            int prefixLen = Integer.parseInt(parts[1]);

            byte[] addressBytes = address.getAddress();
            byte[] networkBytes = network.getAddress();

            if (addressBytes.length != networkBytes.length) {
                return false;
            }

            int byteLen = prefixLen / 8;
            int bitLen = prefixLen % 8;

            for (int i = 0; i < byteLen; i++) {
                if (addressBytes[i] != networkBytes[i]) {
                    return false;
                }
            }

            if (bitLen > 0) {
                int mask = (0xFF << (8 - bitLen)) & 0xFF;
                return (addressBytes[byteLen] & mask) == (networkBytes[byteLen] & mask);
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
