package com.kts.kronos.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record Employee(
        UUID employeeId,
        String fullName,
        String cpf,
        String jobPosition,
        String email,
        double salary,
        String phone,
        boolean active,
        Address address,
        UUID companyId,
        LocalDateTime lastSeenMessageTimestamp,
        boolean homeOffice,
        String faceS3ObjectKey
) {
    public Employee(
            String fullName, String cpf, String jobPosition,
            String email, double salary,
            String phone, Address address, UUID companyId, LocalDateTime lastSeenMessageTimestamp, boolean homeOffice
    ) {
        this(
                UUID.randomUUID(),
                fullName, cpf, jobPosition, email,
                salary, phone, true, address, companyId, lastSeenMessageTimestamp, false,null
        );
    }

    public Employee withEmail(String email) {
        return new Employee(
                employeeId, fullName, cpf, jobPosition,
                email, salary, phone, active, address, companyId, lastSeenMessageTimestamp, homeOffice,null
        );
    }

    public Employee withActive(boolean active) {
        return new Employee(
                employeeId, fullName, cpf, jobPosition,
                email, salary, phone, active, address, companyId, lastSeenMessageTimestamp, homeOffice,null
        );
    }


    public Employee withPhone(String phone) {
        return new Employee(
                employeeId, fullName, cpf, jobPosition,
                email, salary, phone, active, address, companyId, lastSeenMessageTimestamp, homeOffice,null
        );
    }

    public Employee withAddress(Address address) {
        return new Employee(
                employeeId, fullName, cpf, jobPosition,
                email, salary, phone, active, address, companyId, lastSeenMessageTimestamp, homeOffice,null
        );
    }

    public Employee withHomeOffice(boolean homeOffice) {
        return new Employee(
                this.employeeId,
                this.fullName,
                this.cpf,
                this.jobPosition,
                this.email,
                this.salary,
                this.phone,
                this.active,
                this.address,
                this.companyId,
                this.lastSeenMessageTimestamp,
                homeOffice,null
        );
    }

    public Employee withLastSeenMessageTimestamp(LocalDateTime timestamp) {
        return new Employee(
                this.employeeId,
                this.fullName,
                this.cpf,
                this.jobPosition,
                this.email,
                this.salary,
                this.phone,
                this.active,
                this.address,
                this.companyId,
                timestamp,
                this.homeOffice,null
        );
    }

    public Employee withFaceS3ObjectKey(String faceS3ObjectKey) { // NOVO Wither para imutabilidade
        return new Employee(
                this.employeeId,
                this.fullName,
                this.cpf,
                this.jobPosition,
                this.email,
                this.salary,
                this.phone,
                this.active,
                this.address,
                this.companyId,
                this.lastSeenMessageTimestamp,
                this.homeOffice,
                faceS3ObjectKey
        );
    }
}
