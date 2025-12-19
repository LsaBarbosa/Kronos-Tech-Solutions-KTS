package com.kts.kronos.adapter.in.web.dto.employee;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kts.kronos.adapter.in.web.dto.address.AddressResponse;
import com.kts.kronos.domain.model.Employee;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

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
        @JsonFormat(pattern = "HH:mm") LocalTime breakEndTime
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
                employee.breakEndTime()
        );
    }

//    private static String maskCpf(String cpf) {
//        return cpf.substring(0, 5) + "..." + cpf.substring(cpf.length() - 2);
//    }
}
