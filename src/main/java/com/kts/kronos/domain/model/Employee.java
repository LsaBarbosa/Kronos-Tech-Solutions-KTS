package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.enuns.WorkScheduleType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
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
        LocalTime breakEndTime,
        LocalTime weekendWorkStartTime,
        LocalTime weekendWorkEndTime,
        LocalTime weekendBreakStartTime,
        LocalTime weekendBreakEndTime,
        WorkScheduleType scheduleType,
        LocalDate scaleStartDate,
        DayOfWeek preferredDayOff,
        Integer weekendOffIndex,
        Set<DayOfWeek> fixedWorkDays,
        LocalDateTime deletedAt,
        UUID deletedBy,
        String deactivationReason
) {

    public Employee(
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
            LocalTime breakEndTime,
            WorkScheduleType scheduleType,
            LocalDate scaleStartDate,
            DayOfWeek preferredDayOff,
            Integer weekendOffIndex,
            Set<DayOfWeek> fixedWorkDays
    ) {
        this(
                employeeId,
                fullName,
                cpf,
                pis,
                jobPosition,
                email,
                salary,
                phone,
                active,
                address,
                companyId,
                lastSeenMessageTimestamp,
                homeOffice,
                faceS3ObjectKey,
                workStartTime,
                workEndTime,
                breakStartTime,
                breakEndTime,
                null,
                null,
                null,
                null,
                scheduleType,
                scaleStartDate,
                preferredDayOff,
                weekendOffIndex,
                fixedWorkDays,
                null,
                null,
                null
        );
    }

    public Employee(
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
            LocalTime workStartTime,
            LocalTime workEndTime,
            LocalTime breakStartTime,
            LocalTime breakEndTime,
            WorkScheduleType scheduleType,
            LocalDate scaleStartDate,
            DayOfWeek preferredDayOff,
            Integer weekendOffIndex,
            Set<DayOfWeek> fixedWorkDays
    ) {
        this(
                UUID.randomUUID(),
                fullName,
                cpf,
                pis,
                jobPosition,
                email,
                salary,
                phone,
                active,
                address,
                companyId,
                lastSeenMessageTimestamp,
                homeOffice,
                null,
                workStartTime,
                workEndTime,
                breakStartTime,
                breakEndTime,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public Employee withFaceS3ObjectKey(String faceS3ObjectKey) {
        return new Employee(
                employeeId, fullName, cpf, pis, jobPosition, email, salary, phone,
                active, address, companyId, lastSeenMessageTimestamp, homeOffice,
                faceS3ObjectKey, workStartTime, workEndTime, breakStartTime, breakEndTime,
                weekendWorkStartTime, weekendWorkEndTime, weekendBreakStartTime, weekendBreakEndTime,
                scheduleType, scaleStartDate, preferredDayOff, weekendOffIndex, fixedWorkDays,
                deletedAt, deletedBy, deactivationReason
        );
    }

    public Employee withActive(boolean active) {
        return new Employee(
                employeeId, fullName, cpf, pis, jobPosition, email, salary, phone,
                active, address, companyId, lastSeenMessageTimestamp, homeOffice,
                faceS3ObjectKey, workStartTime, workEndTime, breakStartTime, breakEndTime,
                weekendWorkStartTime, weekendWorkEndTime, weekendBreakStartTime, weekendBreakEndTime,
                scheduleType, scaleStartDate, preferredDayOff, weekendOffIndex, fixedWorkDays,
                active ? null : deletedAt,
                active ? null : deletedBy,
                active ? null : deactivationReason
        );
    }

    public Employee deactivate(UUID deletedBy, String reason) {
        return new Employee(
                employeeId, fullName, cpf, pis, jobPosition, email, salary, phone,
                false, address, companyId, lastSeenMessageTimestamp, homeOffice,
                faceS3ObjectKey, workStartTime, workEndTime, breakStartTime, breakEndTime,
                weekendWorkStartTime, weekendWorkEndTime, weekendBreakStartTime, weekendBreakEndTime,
                scheduleType, scaleStartDate, preferredDayOff, weekendOffIndex, fixedWorkDays,
                LocalDateTime.now(), deletedBy, reason
        );
    }

    public Employee withLastSeenMessageTimestamp(LocalDateTime lastSeenMessageTimestamp) {
        return new Employee(
                employeeId, fullName, cpf, pis, jobPosition, email, salary, phone,
                active, address, companyId, lastSeenMessageTimestamp, homeOffice,
                faceS3ObjectKey, workStartTime, workEndTime, breakStartTime, breakEndTime,
                weekendWorkStartTime, weekendWorkEndTime, weekendBreakStartTime, weekendBreakEndTime,
                scheduleType, scaleStartDate, preferredDayOff, weekendOffIndex, fixedWorkDays,
                deletedAt, deletedBy, deactivationReason
        );
    }

    public Employee withAddress(Address address) {
        return new Employee(
                employeeId, fullName, cpf, pis, jobPosition, email, salary, phone,
                active, address, companyId, lastSeenMessageTimestamp, homeOffice,
                faceS3ObjectKey, workStartTime, workEndTime, breakStartTime, breakEndTime,
                weekendWorkStartTime, weekendWorkEndTime, weekendBreakStartTime, weekendBreakEndTime,
                scheduleType, scaleStartDate, preferredDayOff, weekendOffIndex, fixedWorkDays,
                deletedAt, deletedBy, deactivationReason
        );
    }

    public Employee withEmail(String email) {
        return new Employee(
                employeeId, fullName, cpf, pis, jobPosition, email, salary, phone,
                active, address, companyId, lastSeenMessageTimestamp, homeOffice,
                faceS3ObjectKey, workStartTime, workEndTime, breakStartTime, breakEndTime,
                weekendWorkStartTime, weekendWorkEndTime, weekendBreakStartTime, weekendBreakEndTime,
                scheduleType, scaleStartDate, preferredDayOff, weekendOffIndex, fixedWorkDays,
                deletedAt, deletedBy, deactivationReason
        );
    }

    public Employee withPhone(String phone) {
        return new Employee(
                employeeId, fullName, cpf, pis, jobPosition, email, salary, phone,
                active, address, companyId, lastSeenMessageTimestamp, homeOffice,
                faceS3ObjectKey, workStartTime, workEndTime, breakStartTime, breakEndTime,
                weekendWorkStartTime, weekendWorkEndTime, weekendBreakStartTime, weekendBreakEndTime,
                scheduleType, scaleStartDate, preferredDayOff, weekendOffIndex, fixedWorkDays,
                deletedAt, deletedBy, deactivationReason
        );
    }

    public Employee anonymize(
            String anonymizedFullName,
            String anonymizedCpf,
            String anonymizedEmail,
            String anonymizedPis,
            java.util.UUID deletedBy,
            java.time.LocalDateTime deletedAt,
            String reason
    ) {
        return new Employee(
                employeeId,
                anonymizedFullName,
                anonymizedCpf,
                anonymizedPis,
                jobPosition,
                anonymizedEmail,
                salary,
                null,
                false,
                null,
                companyId,
                lastSeenMessageTimestamp,
                homeOffice,
                null,
                workStartTime,
                workEndTime,
                breakStartTime,
                breakEndTime,
                weekendWorkStartTime,
                weekendWorkEndTime,
                weekendBreakStartTime,
                weekendBreakEndTime,
                scheduleType,
                scaleStartDate,
                preferredDayOff,
                weekendOffIndex,
                fixedWorkDays,
                deletedAt,
                deletedBy,
                reason
        );
    }

    public boolean hasFaceImage() {
        return faceS3ObjectKey != null && !faceS3ObjectKey.isBlank();
    }

    public long getDailyWorkMinutes() {
        if (workStartTime == null || workEndTime == null) {
            return 480;
        }

        long totalMinutes = java.time.Duration.between(workStartTime, workEndTime).toMinutes();

        if (breakStartTime != null && breakEndTime != null) {
            long breakMinutes = java.time.Duration.between(breakStartTime, breakEndTime).toMinutes();
            totalMinutes -= breakMinutes;
        }
        return totalMinutes;
    }
}
