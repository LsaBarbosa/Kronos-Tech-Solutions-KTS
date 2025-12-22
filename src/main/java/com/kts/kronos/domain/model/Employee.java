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

        // --- NOVOS CAMPOS PARA REGRAS DE ESCALA ---
        WorkScheduleType scheduleType, // O tipo da escala (Enum)
        LocalDate scaleStartDate,      // Data de início (Para escalas rotativas 12x36, 24x72 e 6x1)
        DayOfWeek preferredDayOff,     // Dia da folga fixa na semana (Para 6x1)
        Integer weekendOffIndex,       // Índice do fim de semana de folga (1º, 2º...)
        Set<DayOfWeek> fixedWorkDays   // Lista de dias fixos (Para escala tradicional customizada)
) {

    // Construtor de conveniência (para criação inicial sem ID e sem escala definida)
    public Employee(
            String fullName,
            String cpf,
            String pis,
            String jobPosition,
            String email, double salary,
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
                null, // faceS3ObjectKey
                workStartTime,
                workEndTime,
                breakStartTime,
                breakEndTime,
                // Inicializa novos campos de escala como nulo (serão definidos depois ou no update)
                null,
                null,
                null,
                null,
                null
        );
    }

    // Wither para Face S3 (Mantendo imutabilidade)
    public Employee withFaceS3ObjectKey(String faceS3ObjectKey) {
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
                faceS3ObjectKey, // Campo atualizado
                this.workStartTime,
                this.workEndTime,
                this.breakStartTime,
                this.breakEndTime,
                this.scheduleType,
                this.scaleStartDate,
                this.preferredDayOff,
                this.weekendOffIndex,
                this.fixedWorkDays
        );
    }

    // Wither para Active (Usado no toggleActivate)
    public Employee withActive(boolean active) {
        return new Employee(
                this.employeeId,
                this.fullName,
                this.cpf,
                this.pis,
                this.jobPosition,
                this.email,
                this.salary,
                this.phone,
                active, // Campo atualizado
                this.address,
                this.companyId,
                this.lastSeenMessageTimestamp,
                this.homeOffice,
                this.faceS3ObjectKey,
                this.workStartTime,
                this.workEndTime,
                this.breakStartTime,
                this.breakEndTime,
                this.scheduleType,
                this.scaleStartDate,
                this.preferredDayOff,
                this.weekendOffIndex,
                this.fixedWorkDays
        );
    }

    // Wither para LastSeenMessage (Usado no markMessagesAsSeen)
    public Employee withLastSeenMessageTimestamp(LocalDateTime lastSeenMessageTimestamp) {
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
                lastSeenMessageTimestamp, // Campo atualizado
                this.homeOffice,
                this.faceS3ObjectKey,
                this.workStartTime,
                this.workEndTime,
                this.breakStartTime,
                this.breakEndTime,
                this.scheduleType,
                this.scaleStartDate,
                this.preferredDayOff,
                this.weekendOffIndex,
                this.fixedWorkDays
        );
    }

    // Wither para Endereço (Usado no update)
    public Employee withAddress(Address address) {
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
                address, // Campo atualizado
                this.companyId,
                this.lastSeenMessageTimestamp,
                this.homeOffice,
                this.faceS3ObjectKey,
                this.workStartTime,
                this.workEndTime,
                this.breakStartTime,
                this.breakEndTime,
                this.scheduleType,
                this.scaleStartDate,
                this.preferredDayOff,
                this.weekendOffIndex,
                this.fixedWorkDays
        );
    }

    // Wither para Email (Usado no updateOwnProfile)
    public Employee withEmail(String email) {
        return new Employee(
                this.employeeId,
                this.fullName,
                this.cpf,
                this.pis,
                this.jobPosition,
                email, // Campo atualizado
                this.salary,
                this.phone,
                this.active,
                this.address,
                this.companyId,
                this.lastSeenMessageTimestamp,
                this.homeOffice,
                this.faceS3ObjectKey,
                this.workStartTime,
                this.workEndTime,
                this.breakStartTime,
                this.breakEndTime,
                this.scheduleType,
                this.scaleStartDate,
                this.preferredDayOff,
                this.weekendOffIndex,
                this.fixedWorkDays
        );
    }

    // Wither para Phone (Usado no updateOwnProfile)
    public Employee withPhone(String phone) {
        return new Employee(
                this.employeeId,
                this.fullName,
                this.cpf,
                this.pis,
                this.jobPosition,
                this.email,
                this.salary,
                phone, // Campo atualizado
                this.active,
                this.address,
                this.companyId,
                this.lastSeenMessageTimestamp,
                this.homeOffice,
                this.faceS3ObjectKey,
                this.workStartTime,
                this.workEndTime,
                this.breakStartTime,
                this.breakEndTime,
                this.scheduleType,
                this.scaleStartDate,
                this.preferredDayOff,
                this.weekendOffIndex,
                this.fixedWorkDays
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