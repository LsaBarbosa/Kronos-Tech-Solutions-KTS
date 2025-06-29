package com.kts.kronos.domain.model;

import java.util.UUID;

public record Employee(
        UUID employeeId,
        String fullName,
        String cpf,
        String jobPosition,
        String email,
        String password,
        double salary,
        String phone,
        boolean active,
        Address address,
        UUID companyId
) {
    public Employee(
            String fullName, String cpf, String jobPosition,
            String email, String password, double salary,
            String phone, Address address, UUID companyId
    ) {
        this(
                UUID.randomUUID(),
                fullName, cpf, jobPosition, email, password,
                salary, phone, true, address, companyId
        );
    }

    public Employee withEmail(String email) {
        return new Employee(
                employeeId, fullName, cpf, jobPosition,
                email, password, salary, phone, active, address, companyId
        );
    }

    public Employee withPassword(String password) {
        return new Employee(
                employeeId, fullName, cpf, jobPosition,
                email, password, salary, phone, active, address, companyId
        );
    }

    public Employee withPhone(String phone) {
        return new Employee(
                employeeId, fullName, cpf, jobPosition,
                email, password, salary, phone, active, address, companyId
        );
    }

    public Employee withAddress(Address address) {
        return new Employee(
                employeeId, fullName, cpf, jobPosition,
                email, password, salary, phone, active, address, companyId
        );
    }
}
