package com.kts.kronos.domain.model;

import com.kts.kronos.adapter.in.web.dto.company.Location;

import java.util.UUID;

public record Company(
        UUID companyId,
        String name,
        String cnpj,
        String email,
        boolean active,
        Address address,
        Location location,
        long activeEmployees,
        long inactiveEmployees
) {
    public Company(String name, String cnpj, String email, Address address,  Location location) {
        this(UUID.randomUUID(), name, cnpj, email, true, address, location, 0, 0);
    }

    public Company withActive(boolean active) {
        return new Company(
                companyId, name, cnpj, email,
                active, address, location,
                activeEmployees, inactiveEmployees
        );
    }
    public Company withEmployeeCounts(long employeActive, long employeeInactive) {
        return new Company(
                companyId, name, cnpj, email,
                active, address, location,
                employeActive, employeeInactive
        );
    }
}
