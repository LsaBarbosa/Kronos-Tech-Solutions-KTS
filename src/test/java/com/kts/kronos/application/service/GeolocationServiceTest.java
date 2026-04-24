package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.adapter.in.web.dto.geolocation.GeolocationResolveRequest;
import com.kts.kronos.application.port.out.provider.AddressLookupProvider;
import com.kts.kronos.application.port.out.provider.GeolocationProvider;
import com.kts.kronos.domain.model.Address;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeolocationServiceTest {

    @InjectMocks
    private GeolocationService service;

    @Mock
    private AddressLookupProvider addressLookupProvider;

    @Mock
    private GeolocationProvider geolocationProvider;

    @Test
    @DisplayName("resolve: compõe endereço via ViaCEP e delega geocodificação")
    void shouldResolveCoordinatesFromPostalCodeAndNumber() {
        GeolocationResolveRequest request = new GeolocationResolveRequest("25900000", "123");
        Address address = new Address("Rua Central", null, "25900000", "Petropolis", "RJ");
        Location location = new Location(-22.509804, -43.177544);

        when(addressLookupProvider.lookup("25900000")).thenReturn(address);
        when(geolocationProvider.resolve(address.withNumber("123"))).thenReturn(location);

        Location result = service.resolve(request);

        assertEquals(location, result);
        verify(addressLookupProvider).lookup("25900000");
        verify(geolocationProvider).resolve(address.withNumber("123"));
    }
}
