package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.adapter.in.web.dto.geolocation.GeolocationResolveRequest;
import com.kts.kronos.application.port.out.provider.AddressLookupProvider;
import com.kts.kronos.application.port.out.provider.CacheProvider;
import com.kts.kronos.application.port.out.provider.GeolocationProvider;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.observability.application.KronosMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GeolocationServiceCoverage2Test {

    @Mock private AddressLookupProvider addressLookupProvider;
    @Mock private GeolocationProvider geolocationProvider;
    @Mock private KronosMetrics kronosMetrics;
    @Mock private CacheProvider cacheProvider;

    private static final GeolocationResolveRequest REQUEST =
            new GeolocationResolveRequest("01310100", "100");
    private static final Address ADDRESS =
            new Address("Av. Paulista", null, "01310100", "São Paulo", "SP");
    private static final Location LOCATION = new Location(-23.5614, -46.6558);

    // Covers L63 (return cacheProvider.getOrLoad) + L60 FALSE branch (cacheProvider != null)
    @Test
    void resolve_withNonNullCacheProvider_delegatesGetOrLoad() {
        when(addressLookupProvider.lookup("01310100")).thenReturn(ADDRESS);
        when(geolocationProvider.resolve(any())).thenReturn(LOCATION);
        when(cacheProvider.getOrLoad(any(), any(), eq(Location.class), any()))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<Location> supplier = inv.getArgument(3);
                    return supplier.get();
                });

        var svc = new GeolocationService(addressLookupProvider, geolocationProvider, kronosMetrics, cacheProvider);
        var result = svc.resolve(REQUEST);

        assertEquals(LOCATION, result);
        verify(cacheProvider).getOrLoad(any(), any(), eq(Location.class), any());
    }

    // Covers L52-55 (catch block: geolocationLookupFailure + rethrow)
    @Test
    void resolveLocation_whenProviderThrows_coversCatchBlock() {
        when(addressLookupProvider.lookup("01310100")).thenReturn(ADDRESS);
        when(geolocationProvider.resolve(any())).thenThrow(new RuntimeException("provider down"));

        var svc = new GeolocationService(addressLookupProvider, geolocationProvider, kronosMetrics, null);
        assertThrows(RuntimeException.class, () -> svc.resolve(REQUEST));
        verify(kronosMetrics).geolocationLookupFailure("provider_error");
    }
}
