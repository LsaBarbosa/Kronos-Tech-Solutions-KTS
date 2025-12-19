package com.kts.kronos.domain.model;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record Employee(
        UUID employeeId,
        String fullName,
        String cpf,
        String pis,
        String jobPosition,
        String email,
        double salary,
        String phone,
        boolean active,
        Address address,
        UUID companyId,
        LocalDateTime lastSeenMessageTimestamp,
        boolean homeOffice,
        String faceS3ObjectKey,
        LocalTime workStartTime,
        LocalTime workEndTime,
        LocalTime breakStartTime,
        LocalTime breakEndTime
) {
    public Employee(
            String fullName,
            String cpf,
            String pis,
            String jobPosition,
            String email, double salary,
            String phone,
            Address address,
            UUID companyId,
            LocalDateTime lastSeenMessageTimestamp,
            boolean homeOffice,
            LocalTime workStartTime,
            LocalTime workEndTime,
            LocalTime breakStartTime,
            LocalTime breakEndTime
    ) {
        this(
                UUID.randomUUID(),
                fullName, cpf, pis, jobPosition, email,
                salary, phone, true, address, companyId, lastSeenMessageTimestamp, false, null,
                workStartTime,
                workEndTime,
                breakStartTime,
                breakEndTime

        );
    }

    public Employee withEmail(String email) {
        return new Employee(
                employeeId, fullName, cpf, pis, jobPosition,
                email, salary, phone, active, address, companyId, lastSeenMessageTimestamp, homeOffice, null, workStartTime,
                workEndTime,
                breakStartTime,
                breakEndTime
        );
    }

    public Employee withActive(boolean active) {
        return new Employee(
                employeeId, fullName, cpf, pis, jobPosition,
                email, salary, phone, active, address, companyId, lastSeenMessageTimestamp, homeOffice, null, workStartTime,
                workEndTime,
                breakStartTime,
                breakEndTime
        );
    }


    public Employee withPhone(String phone) {
        return new Employee(
                employeeId, fullName, cpf, pis, jobPosition,
                email, salary, phone, active, address, companyId, lastSeenMessageTimestamp, homeOffice, null, workStartTime,
                workEndTime,
                breakStartTime,
                breakEndTime
        );
    }

    public Employee withAddress(Address address) {
        return new Employee(
                employeeId, fullName, cpf, pis, jobPosition,
                email, salary, phone, active, address, companyId, lastSeenMessageTimestamp, homeOffice, null, workStartTime,
                workEndTime,
                breakStartTime,
                breakEndTime
        );
    }

    public Employee withPis(String pis) {
        return new Employee(
                employeeId, fullName, cpf, pis, jobPosition,
                email, salary, phone, active, address, companyId, lastSeenMessageTimestamp, homeOffice, null, workStartTime,
                workEndTime,
                breakStartTime,
                breakEndTime
        );
    }

    public Employee withHomeOffice(boolean homeOffice) {
        return new Employee(
                this.employeeId,
                this.fullName,
                this.cpf,
                this.pis,
                this.jobPosition,
                this.email,
                this.salary,
                this.phone,
                this.active,
                this.address,
                this.companyId,
                this.lastSeenMessageTimestamp,
                homeOffice, null, workStartTime,
                workEndTime,
                breakStartTime,
                breakEndTime
        );
    }

    public Employee withLastSeenMessageTimestamp(LocalDateTime timestamp) {
        return new Employee(
                this.employeeId,
                this.fullName,
                this.cpf,
                this.pis,
                this.jobPosition,
                this.email,
                this.salary,
                this.phone,
                this.active,
                this.address,
                this.companyId,
                timestamp,
                this.homeOffice, null, workStartTime,
                workEndTime,
                breakStartTime,
                breakEndTime
        );
    }

    public Employee withFaceS3ObjectKey(String faceS3ObjectKey) { // NOVO Wither para imutabilidade
        return new Employee(
                this.employeeId,
                this.fullName,
                this.cpf,
                this.pis,
                this.jobPosition,
                this.email,
                this.salary,
                this.phone,
                this.active,
                this.address,
                this.companyId,
                this.lastSeenMessageTimestamp,
                this.homeOffice,
                faceS3ObjectKey, workStartTime,
                workEndTime,
                breakStartTime,
                breakEndTime
        );
    }

    public long getDailyWorkMinutes() {
        if (workStartTime == null || workEndTime == null) return 480; // Default 8h se nulo

        long totalMinutes = java.time.Duration.between(workStartTime, workEndTime).toMinutes();

        if (breakStartTime != null && breakEndTime != null) {
            long breakMinutes = java.time.Duration.between(breakStartTime, breakEndTime).toMinutes();
            totalMinutes -= breakMinutes;
        }
        return totalMinutes;
    }
}

