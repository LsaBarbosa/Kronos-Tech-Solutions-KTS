package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.adapter.in.web.dto.geolocation.GeolocationResolveRequest;
import com.kts.kronos.application.cache.ApplicationCacheNames;
import com.kts.kronos.application.cache.CacheScopes;
import com.kts.kronos.application.port.in.usecase.GeolocationUseCase;
import com.kts.kronos.application.port.out.provider.AddressLookupProvider;
import com.kts.kronos.application.port.out.provider.CacheProvider;
import com.kts.kronos.application.port.out.provider.GeolocationProvider;
import com.kts.kronos.observability.application.KronosMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GeolocationService implements GeolocationUseCase {

    private final AddressLookupProvider addressLookupProvider;
    private final GeolocationProvider geolocationProvider;
    private final KronosMetrics kronosMetrics;
    private final CacheProvider cacheProvider;

    @Override
    public Location resolve(GeolocationResolveRequest request) {
        return cache(
                ApplicationCacheNames.GEOLOCATION_RESOLVE,
                CacheScopes.authenticatedScope(
                        "postalCode=" + request.postalCode(),
                        "number=" + request.number()
                ),
                Location.class,
                () -> resolveLocation(request)
        );
    }

    private Location resolveLocation(GeolocationResolveRequest request) {
        var address = addressLookupProvider.lookup(request.postalCode())
                .withNumber(request.number());

        var start = Instant.now();
        try {
            var location = geolocationProvider.resolve(address);
            kronosMetrics.geolocationLookupSuccess();
            kronosMetrics.recordGeolocationDuration(Duration.between(start, Instant.now()));
            return location;
        } catch (Exception e) {
            kronosMetrics.geolocationLookupFailure("provider_error");
            kronosMetrics.recordGeolocationDuration(Duration.between(start, Instant.now()));
            throw e;
        }
    }

    private <T> T cache(String cacheName, String scope, Class<T> type, java.util.function.Supplier<T> loader) {
        if (cacheProvider == null) {
            return loader.get();
        }
        return cacheProvider.getOrLoad(cacheName, scope, type, loader);
    }
}
