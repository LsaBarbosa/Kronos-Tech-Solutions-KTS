package com.kts.kronos.domain.model;

public record ClientIpResolution(
    String ipAddress,
    String source,
    boolean trusted
) {
    public enum IpSource {
        X_FORWARDED_FOR,
        X_REAL_IP,
        REMOTE_ADDR,
        UNKNOWN
    }

    public static ClientIpResolution of(String ipAddress, IpSource source, boolean trusted) {
        return new ClientIpResolution(ipAddress, source.name(), trusted);
    }
}
