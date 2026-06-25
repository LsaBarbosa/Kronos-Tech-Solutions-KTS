package com.kts.kronos.config.chat;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "support.chat.tawk")
public class TawkChatProperties {

    private boolean enabled = false;
    private String propertyId = "";
    private String widgetId = "";
    private String secureKey = "";
    private String webhookSecret = "";
    private long identityTtlSeconds = 3600;
    private int retentionDays = 90;
    private boolean retentionCleanupEnabled = false;
    private String retentionCleanupCron = "0 0 3 * * ?";
    private int maxMetadataEntries = 20;
    private int maxValueLength = 500;
    private int maxSearchQueryLength = 200;
    private int maxWebhookPayloadCharacters = 8192;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getPropertyId() { return propertyId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }

    public String getWidgetId() { return widgetId; }
    public void setWidgetId(String widgetId) { this.widgetId = widgetId; }

    public String getSecureKey() { return secureKey; }
    public void setSecureKey(String secureKey) { this.secureKey = secureKey; }

    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }

    public long getIdentityTtlSeconds() { return identityTtlSeconds; }
    public void setIdentityTtlSeconds(long identityTtlSeconds) { this.identityTtlSeconds = identityTtlSeconds; }

    public int getRetentionDays() { return retentionDays; }
    public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }

    public boolean isRetentionCleanupEnabled() { return retentionCleanupEnabled; }
    public void setRetentionCleanupEnabled(boolean retentionCleanupEnabled) { this.retentionCleanupEnabled = retentionCleanupEnabled; }

    public String getRetentionCleanupCron() { return retentionCleanupCron; }
    public void setRetentionCleanupCron(String retentionCleanupCron) { this.retentionCleanupCron = retentionCleanupCron; }

    public int getMaxMetadataEntries() { return maxMetadataEntries; }
    public void setMaxMetadataEntries(int maxMetadataEntries) { this.maxMetadataEntries = maxMetadataEntries; }

    public int getMaxValueLength() { return maxValueLength; }
    public void setMaxValueLength(int maxValueLength) { this.maxValueLength = maxValueLength; }

    public int getMaxSearchQueryLength() { return maxSearchQueryLength; }
    public void setMaxSearchQueryLength(int maxSearchQueryLength) { this.maxSearchQueryLength = maxSearchQueryLength; }

    public int getMaxWebhookPayloadCharacters() { return maxWebhookPayloadCharacters; }
    public void setMaxWebhookPayloadCharacters(int maxWebhookPayloadCharacters) { this.maxWebhookPayloadCharacters = maxWebhookPayloadCharacters; }
}
