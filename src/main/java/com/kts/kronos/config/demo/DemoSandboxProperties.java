package com.kts.kronos.config.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "kronos.demo")
public class DemoSandboxProperties {

    private boolean enabled = false;
    private boolean killSwitch = false;
    private String sandboxKey = "KRONOS_TESTE";
    private String companyName = "Kronos Teste";
    private String username = "kronos_teste";
    private String initialPassword = "kronos_teste#";
    private String localStorageRoot = "/opt/kronos/sandbox/kronos-teste";
    private Duration lockTimeout = Duration.ofMinutes(5);

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isKillSwitch() { return killSwitch; }
    public void setKillSwitch(boolean killSwitch) { this.killSwitch = killSwitch; }

    public String getSandboxKey() { return sandboxKey; }
    public void setSandboxKey(String sandboxKey) { this.sandboxKey = sandboxKey; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getInitialPassword() { return initialPassword; }
    public void setInitialPassword(String initialPassword) { this.initialPassword = initialPassword; }

    public String getLocalStorageRoot() { return localStorageRoot; }
    public void setLocalStorageRoot(String localStorageRoot) { this.localStorageRoot = localStorageRoot; }

    public Duration getLockTimeout() { return lockTimeout; }
    public void setLockTimeout(Duration lockTimeout) { this.lockTimeout = lockTimeout; }
}