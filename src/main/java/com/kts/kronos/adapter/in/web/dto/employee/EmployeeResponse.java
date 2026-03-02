package com.kts.kronos.adapter.in.web.dto.employee;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kts.kronos.adapter.in.web.dto.address.AddressResponse;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.enuns.WorkScheduleType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = com.kts.kronos.constants.Swagger.DTO_SCHEMA_DESCRIPTION)
public record EmployeeResponse(
        UUID employeeId,
        String fullName,
        String maskedCpf,
        String jobPosition,
        String email,
        double salary,
        String phone,
        AddressResponse address,
        String companyName,
        LocalDateTime lastSeenMessageTimestamp,
        boolean homeOffice,
        String role,

        @JsonFormat(pattern = "HH:mm") LocalTime workStartTime,
        @JsonFormat(pattern = "HH:mm") LocalTime workEndTime,
        @JsonFormat(pattern = "HH:mm") LocalTime breakStartTime,
        @JsonFormat(pattern = "HH:mm") LocalTime breakEndTime,
        WorkScheduleType scheduleType,       // Ex: TRADITIONAL_5X2, SIX_BY_ONE...

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate scaleStartDate,            // Data base para cálculo de ciclos (6x1, 12x36)

        DayOfWeek preferredDayOff,           // Dia da folga fixa (se houver)

        Integer weekendOffIndex,             // Controle de qual FDS é folga (para 6x1 variantes)

        Set<DayOfWeek> fixedWorkDays         // Dias fixos de trabalho (para 5x2)
) {
    public static EmployeeResponse fromDomain(Employee employee, String companyName, String role) {
        return new EmployeeResponse(
                employee.employeeId(),
                employee.fullName(),
                employee.cpf(),
                employee.jobPosition(),
                employee.email(),
                employee.salary(),
                employee.phone(),
                AddressResponse.fromDomain(employee.address()),
                companyName,
                employee.lastSeenMessageTimestamp(),
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

//    private static String maskCpf(String cpf) {
//        return cpf.substring(0, 5) + "..." + cpf.substring(cpf.length() - 2);
//    }
}
