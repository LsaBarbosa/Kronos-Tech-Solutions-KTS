package com.kts.kronos.application.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kronos.security.client-ip")
public class ClientIpResolverProperties {
    private boolean trustForwardedHeaders = true;
    private List<String> trustedProxyCidrs = new ArrayList<>();

    public ClientIpResolverProperties() {
        this.trustedProxyCidrs.add("127.0.0.1/32");
        this.trustedProxyCidrs.add("::1/128");
    }
}
