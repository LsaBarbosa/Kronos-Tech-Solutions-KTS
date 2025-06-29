package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Company;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Embeddable @AllArgsConstructor @NoArgsConstructor @Data @EqualsAndHashCode @Builder
public class AddressEmbeddable {
    @Column(name = "street", length = 255)
    private String street;

    @Column(name = "number", length = 20)
    private String number;

    @Column(name = "postal_code", length = 20, nullable = false)
    private String postalCode;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    public Address toDomain() {
        return new Address(
                street, number, postalCode, city, state
        );
    }
    public static AddressEmbeddable fromDomain(Address address) {
        return new AddressEmbeddable(
                address.street(),
                address.number(),
                address.postalCode(),
                address.city(),
                address.state()
        );
    }
}