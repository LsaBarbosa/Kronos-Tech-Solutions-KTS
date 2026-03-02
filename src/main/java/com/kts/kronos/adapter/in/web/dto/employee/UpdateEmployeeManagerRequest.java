package com.kts.kronos.adapter.in.web.dto.employee;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kts.kronos.adapter.in.web.dto.address.UpdateAddressRequest;
import com.kts.kronos.domain.model.enuns.WorkScheduleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import static com.kts.kronos.constants.Messages.*;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = com.kts.kronos.constants.Swagger.DTO_SCHEMA_DESCRIPTION)
public record UpdateEmployeeManagerRequest(
        String fullName,

        @CPF
        @Pattern(regexp = "\\d{11}", message = MUST_HAVE_11_CHARACTERES)
        String cpf,

        @Pattern(regexp = "\\d{11}", message = MUST_HAVE_11_CHARACTERES)
        String pis,

        String jobPosition,
        @Email(message = INVALID_EMAIL_FORMAT)
        @Size(max = 50)
        String email,

        @Positive(message = SALARY_MUST_BE_POSITIVE)
        Double salary,

        String phone,
        Boolean homeOffice,
        @Valid UpdateAddressRequest address,
        String faceImageBase64,
        @JsonFormat(pattern = "HH:mm") LocalTime workStartTime,
        @JsonFormat(pattern = "HH:mm") LocalTime workEndTime,
        @JsonFormat(pattern = "HH:mm") LocalTime breakStartTime,
        @JsonFormat(pattern = "HH:mm") LocalTime breakEndTime,

        // --- NOVOS CAMPOS DE ATUALIZAÇÃO DE ESCALA ---
        WorkScheduleType scheduleType,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate scaleStartDate,
        DayOfWeek preferredDayOff,
        Integer weekendOffIndex,
        Set<DayOfWeek> fixedWorkDays
) {
}
