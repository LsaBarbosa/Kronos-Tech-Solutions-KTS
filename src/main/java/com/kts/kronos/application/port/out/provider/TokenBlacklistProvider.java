package com.kts.kronos.application.port.out.provider;

import java.util.Date;

public interface TokenBlacklistProvider {
    void addToBlacklist(String rawToken, Date tokenExpiration);
    boolean isBlacklisted(String rawToken);
    void deleteExpiredTokens();
}
