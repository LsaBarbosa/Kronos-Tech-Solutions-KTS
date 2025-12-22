package com.kts.kronos.adapter.in.web.dto.employee;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kts.kronos.adapter.in.web.dto.address.AddressRequest;
import com.kts.kronos.domain.model.enuns.WorkScheduleType;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.br.CPF;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.*;


public record CreateEmployeeRequest(
        @NotBlank(message = EMPLOYEE_NAME_NOT_BLANK)
        @Size(max = 200, message = MUST_HAVE_200_CHARACTERES)
        String fullName,

        @CPF
        @NotBlank(message = CPF_NOT_BLANK)
        @Pattern(regexp = "\\d{11}", message = MUST_HAVE_11_CHARACTERES)
        String cpf,

        @Nullable
        @Pattern(regexp = "\\d{11}", message = MUST_HAVE_11_CHARACTERES)
        String pis,

        @NotBlank(message = JOB_POSITION_NOT_BLANK)
        @Size(max = 50)
        String jobPosition,

        @NotBlank(message = EMAIL_NOT_BLANK)
        @Email(message = INVALID_EMAIL_FORMAT)
        @Size(max = 50)
        String email,

        @Positive(message = SALARY_MUST_BE_POSITIVE)
        Double salary,

        String phone,
        @Valid AddressRequest
        address, UUID companyId,
        boolean homeOffice,
        String faceImageBase64,
        @JsonFormat(pattern = "HH:mm") LocalTime workStartTime,
        @JsonFormat(pattern = "HH:mm") LocalTime workEndTime,
        @JsonFormat(pattern = "HH:mm") LocalTime breakStartTime,
        @JsonFormat(pattern = "HH:mm") LocalTime breakEndTime,
        WorkScheduleType scheduleType,       // Ex: SIX_BY_ONE_TWO_WEEKENDS

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate scaleStartDate,            // Data início (para cálculo de 6x1 ou 12x36)

        DayOfWeek preferredDayOff,           // Dia fixo da folga (Ex: THURSDAY)

        Integer weekendOffIndex,             // Índice do fim de semana (1, 2, 3...)

        Set<DayOfWeek> fixedWorkDays         // Lista de dias fixos (Para escala tradicional)
) {
}