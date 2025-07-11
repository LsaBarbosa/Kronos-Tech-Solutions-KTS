package com.kts.kronos.application.port.out.repository;

import com.kts.kronos.domain.model.Address;

public interface AddressLookupPort {
    Address lookup(String postalCode);

}
