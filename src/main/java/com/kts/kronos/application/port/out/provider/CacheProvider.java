package com.kts.kronos.application.port.out.provider;

import java.util.function.Supplier;

public interface CacheProvider {
    <T> T getOrLoad(String cacheName, String scope, Class<T> type, Supplier<T> loader);

    void evict(String cacheName, String scope);

    void evictNamespace(String cacheName);
}
