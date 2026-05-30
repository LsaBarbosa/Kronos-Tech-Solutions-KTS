package com.kts.kronos.domain.model;

public record ClientIpResolution(
    String ipAddress,
    String source,
    boolean trusted,
    boolean proxyChainValid
) {
    public enum IpSource {
        X_FORWARDED_FOR,
        X_REAL_IP,
        REMOTE_ADDR,
        UNKNOWN
    }

    public ClientIpResolution(String ipAddress, String source, boolean trusted) {
        this(ipAddress, source, trusted, true);
    }

    public static ClientIpResolution of(String ipAddress, IpSource source, boolean trusted) {
        return new ClientIpResolution(ipAddress, source.name(), trusted, true);
    }

    public static ClientIpResolution of(String ipAddress, IpSource source, boolean trusted, boolean proxyChainValid) {
        return new ClientIpResolution(ipAddress, source.name(), trusted, proxyChainValid);
    }
}
