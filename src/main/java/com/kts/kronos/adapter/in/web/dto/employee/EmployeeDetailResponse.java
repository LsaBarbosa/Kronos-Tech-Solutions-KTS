package com.kts.kronos.adapter.in.web.dto.employee;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kts.kronos.adapter.in.web.dto.address.AddressResponse;
import com.kts.kronos.application.util.SensitiveDataMasker;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.enuns.WorkScheduleType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

/**
 * SPEC-002: DTO detalhado para consulta individual e detalhe gerencial.
 * Inclui dados administrativos necessários para gestor autorizado.
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

        @JsonFormat(pattern = "HH:mm") LocalTime workStartTime,
        @JsonFormat(pattern = "HH:mm") LocalTime workEndTime,
        @JsonFormat(pattern = "HH:mm") LocalTime breakStartTime,
        @JsonFormat(pattern = "HH:mm") LocalTime breakEndTime,
        WorkScheduleType scheduleType,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate scaleStartDate,

        DayOfWeek preferredDayOff,
        Integer weekendOffIndex,
        Set<DayOfWeek> fixedWorkDays
) {
    public static EmployeeDetailResponse fromDomain(Employee employee, String companyName, String role) {
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
                employee.workStartTime(),
                employee.workEndTime(),
                employee.breakStartTime(),
                employee.breakEndTime(),
                employee.scheduleType(),
                employee.scaleStartDate(),
                employee.preferredDayOff(),
                employee.weekendOffIndex(),
                employee.fixedWorkDays()
        );
    }
}
