package com.kts.kronos.adapter.in.web.dto.employee;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kts.kronos.adapter.in.web.dto.address.AddressResponse;
import com.kts.kronos.application.util.SensitiveDataMasker;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.enuns.WorkScheduleType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

/**
 * SPEC-002: DTO detalhado para consulta individual e detalhe gerencial.
 * Inclui dados administrativos necessarios para gestor autorizado.
 */
public record EmployeeDetailResponse(
        UUID employeeId,
        String fullName,
        String maskedCpf,
        String jobPosition,
        String email,
        double salary,
        String phone,
        AddressResponse address,
        String companyName,
        boolean homeOffice,
        String role,
        boolean sandbox,
        boolean terminalFlag,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime lastSeenMessageTimestamp,

        @JsonFormat(pattern = "HH:mm") LocalTime workStartTime,
        @JsonFormat(pattern = "HH:mm") LocalTime workEndTime,
        @JsonFormat(pattern = "HH:mm") LocalTime breakStartTime,
        @JsonFormat(pattern = "HH:mm") LocalTime breakEndTime,
        @JsonFormat(pattern = "HH:mm") LocalTime weekendWorkStartTime,
        @JsonFormat(pattern = "HH:mm") LocalTime weekendWorkEndTime,
        @JsonFormat(pattern = "HH:mm") LocalTime weekendBreakStartTime,
        @JsonFormat(pattern = "HH:mm") LocalTime weekendBreakEndTime,
        WorkScheduleType scheduleType,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate scaleStartDate,

        DayOfWeek preferredDayOff,
        Integer weekendOffIndex,
        Set<DayOfWeek> fixedWorkDays
) {
    public static EmployeeDetailResponse fromDomain(Employee employee, String companyName, String role) {
        return fromDomain(employee, companyName, role, false, false);
    }

    public static EmployeeDetailResponse fromDomain(Employee employee, String companyName, String role, boolean sandbox) {
        return fromDomain(employee, companyName, role, sandbox, false);
    }

    public static EmployeeDetailResponse fromDomain(Employee employee, String companyName, String role, boolean sandbox, boolean terminalFlag) {
        return new EmployeeDetailResponse(
                employee.employeeId(),
                employee.fullName(),
                SensitiveDataMasker.maskCpf(employee.cpf()),
                employee.jobPosition(),
                employee.email(),
                employee.salary(),
                employee.phone(),
                AddressResponse.fromDomain(employee.address()),
                companyName,
                employee.homeOffice(),
                role,
                sandbox,
                terminalFlag,
                employee.lastSeenMessageTimestamp(),
                employee.workStartTime(),
                employee.workEndTime(),
                employee.breakStartTime(),
                employee.breakEndTime(),
                employee.weekendWorkStartTime(),
                employee.weekendWorkEndTime(),
                employee.weekendBreakStartTime(),
                employee.weekendBreakEndTime(),
                employee.scheduleType(),
                employee.scaleStartDate(),
                employee.preferredDayOff(),
                employee.weekendOffIndex(),
                employee.fixedWorkDays()
        );
    }
}
