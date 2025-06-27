package com.kts.kronos.adapter.out.security.loginattempt;


import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.kts.kronos.domain.port.out.LoginAttemptPort;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
/*
 Bloqeuia tentativa após 5 erros
 faz a contagem e o bloqueio de tentativas de login usando um cache configurado
*/
@Component
public class GuavaLoginAttemptAdapter implements LoginAttemptPort {
    private final LoadingCache<String, Integer> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(100)
            .build(new CacheLoader<>() {
                @Override
                public Integer load(String key) {
                    return 0;
                }
            });

    @Override
    public void recordSuccess(String key) {
        cache.invalidate(key);
    }

    @Override
    public void recordFailure(String key) {
        cache.put(key, cache.getUnchecked(key) + 1);
    }

    @Override
    public boolean isBlocked(String key) {
        return cache.getUnchecked(key) >= 5;
    }
}
