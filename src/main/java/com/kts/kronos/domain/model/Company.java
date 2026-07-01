package com.kts.kronos.domain.model;

import com.kts.kronos.adapter.in.web.dto.company.Location;

import java.time.LocalDateTime;
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
        long inactiveEmployees,
        LocalDateTime deletedAt,
        UUID deletedBy,
        String deactivationReason,
        boolean terminalFlag
) {
    public Company(UUID companyId, String name, String cnpj, String email, boolean active, Address address, Location location, long activeEmployees, long inactiveEmployees) {
        this(companyId, name, cnpj, email, active, address, location, activeEmployees, inactiveEmployees, null, null, null, false);
    }

    public Company(String name, String cnpj, String email, Address address, Location location) {
        this(UUID.randomUUID(), name, cnpj, email, true, address, location, 0, 0, null, null, null, false);
    }

    public Company withActive(boolean active) {
        return new Company(
                companyId, name, cnpj, email,
                active, address, location,
                activeEmployees, inactiveEmployees,
                active ? null : deletedAt,
                active ? null : deletedBy,
                active ? null : deactivationReason,
                terminalFlag
        );
    }

    public Company deactivate(UUID deletedBy, String reason) {
        return new Company(
                companyId, name, cnpj, email,
                false, address, location,
                activeEmployees, inactiveEmployees,
                LocalDateTime.now(),
                deletedBy,
                reason,
                terminalFlag
        );
    }

    public Company withEmployeeCounts(long employeActive, long employeeInactive) {
        return new Company(
                companyId, name, cnpj, email,
                active, address, location,
                employeActive, employeeInactive,
                deletedAt, deletedBy, deactivationReason,
                terminalFlag
        );
    }

    public Company withTerminalFlag(boolean terminalFlag) {
        return new Company(
                companyId, name, cnpj, email,
                active, address, location,
                activeEmployees, inactiveEmployees,
                deletedAt, deletedBy, deactivationReason,
                terminalFlag
        );
    }
}
