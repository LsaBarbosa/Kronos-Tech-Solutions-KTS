package com.kts.kronos.adapter.in.web.dto.address;

import com.kts.kronos.domain.model.Address;

public record AddressResponse (
        String street,
        String number,
        String postalCode,
        String city,
        String state
){
    public static AddressResponse fromDomain(Address address) {
        return new AddressResponse(
                address.street(),
                address.number(),
                address.postalCode(),
                address.city(),
                address.state()
        );
    }
}
