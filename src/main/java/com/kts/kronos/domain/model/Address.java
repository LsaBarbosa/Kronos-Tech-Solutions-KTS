package com.kts.kronos.domain.model;

import java.util.Objects;

import static com.kts.kronos.constants.Messages.POSTAL_CODE_NOT_BLANK;

public record Address(String street,
                      String number,
                      String postalCode,
                      String city,
                      String state) {

    public Address{
        Objects.requireNonNull(postalCode,POSTAL_CODE_NOT_BLANK);
    }
    public Address withNumber(String number) {
        return new Address(street, number, postalCode, city, state);
    }
}
