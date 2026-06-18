package com.kts.kronos.infrastructure.redis;

import com.kts.kronos.application.cache.CacheScopes;

public final class RedisScopeKeyResolver {
    private RedisScopeKeyResolver() {
    }

    public static String authenticatedScope(String... parts) {
        return CacheScopes.authenticatedScope(parts);
    }

    public static String publicScope(String... parts) {
        return CacheScopes.publicScope(parts);
    }
}
