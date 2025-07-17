package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.domain.model.Address;

public interface AddressLookupProvider {
    Address lookup(String postalCode);

}
