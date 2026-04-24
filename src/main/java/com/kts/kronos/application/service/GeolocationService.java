package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.adapter.in.web.dto.geolocation.GeolocationResolveRequest;
import com.kts.kronos.application.port.in.usecase.GeolocationUseCase;
import com.kts.kronos.application.port.out.provider.AddressLookupProvider;
import com.kts.kronos.application.port.out.provider.GeolocationProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GeolocationService implements GeolocationUseCase {

    private final AddressLookupProvider addressLookupProvider;
    private final GeolocationProvider geolocationProvider;

    @Override
    public Location resolve(GeolocationResolveRequest request) {
        var address = addressLookupProvider.lookup(request.postalCode())
                .withNumber(request.number());

        return geolocationProvider.resolve(address);
    }
}
