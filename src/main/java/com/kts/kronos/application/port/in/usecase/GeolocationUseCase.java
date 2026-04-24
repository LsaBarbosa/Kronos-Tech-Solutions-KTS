package com.kts.kronos.application.port.in.usecase;

import com.kts.kronos.adapter.in.web.dto.geolocation.GeolocationResolveRequest;
import com.kts.kronos.adapter.in.web.dto.company.Location;

public interface GeolocationUseCase {
    Location resolve(GeolocationResolveRequest request);
}
