package com.kts.kronos.domain.model;

import java.util.Objects;

public record Address(String street,
                      String number,
                      String postalCode,
                      String city,
                      String state) {

    public Address{
        Objects.requireNonNull(postalCode,"cep não pode ser nulo");
    }
    public Address withNumber(String number) {
        return new Address(street, number, postalCode, city, state);
    }
}
