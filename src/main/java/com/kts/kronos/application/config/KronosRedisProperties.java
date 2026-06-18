package com.kts.kronos.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "kronos.redis")
public class KronosRedisProperties {
    private boolean enabled = false;
    private String namespace = "kronos";
    private String keyHmacSecret = "local-dev-redis-key-secret";
    private Duration cacheDefaultTtl = Duration.ofMinutes(5);
    private Duration cacheShortTtl = Duration.ofSeconds(30);
    private Duration cacheMediumTtl = Duration.ofMinutes(5);
    private Duration cacheLongTtl = Duration.ofHours(24);
    private Duration passwordResetTtl = Duration.ofMinutes(30);
    private Duration lockTtl = Duration.ofSeconds(10);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getKeyHmacSecret() {
        return keyHmacSecret;
    }

    public void setKeyHmacSecret(String keyHmacSecret) {
        this.keyHmacSecret = keyHmacSecret;
    }

    public Duration getCacheDefaultTtl() {
        return cacheDefaultTtl;
    }

    public void setCacheDefaultTtl(Duration cacheDefaultTtl) {
        this.cacheDefaultTtl = cacheDefaultTtl;
    }

    public Duration getCacheShortTtl() {
        return cacheShortTtl;
    }

    public void setCacheShortTtl(Duration cacheShortTtl) {
        this.cacheShortTtl = cacheShortTtl;
    }

    public Duration getCacheMediumTtl() {
        return cacheMediumTtl;
    }

    public void setCacheMediumTtl(Duration cacheMediumTtl) {
        this.cacheMediumTtl = cacheMediumTtl;
    }

    public Duration getCacheLongTtl() {
        return cacheLongTtl;
    }

    public void setCacheLongTtl(Duration cacheLongTtl) {
        this.cacheLongTtl = cacheLongTtl;
    }

    public Duration getPasswordResetTtl() {
        return passwordResetTtl;
    }

    public void setPasswordResetTtl(Duration passwordResetTtl) {
        this.passwordResetTtl = passwordResetTtl;
    }

    public Duration getLockTtl() {
        return lockTtl;
    }

    public void setLockTtl(Duration lockTtl) {
        this.lockTtl = lockTtl;
    }
}
