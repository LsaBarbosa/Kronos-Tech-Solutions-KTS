package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.domain.model.Address;

public interface GeolocationProvider {
    Location resolve(Address address);
}
