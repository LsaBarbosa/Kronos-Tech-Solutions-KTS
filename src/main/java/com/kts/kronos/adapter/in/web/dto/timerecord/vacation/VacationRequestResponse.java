package com.kts.kronos.adapter.in.web.dto.timerecord.vacation;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.kts.kronos.constants.Messages.DATE_PATTERN;

@Schema(description = "Resposta contendo o período de férias consolidado de um funcionário")
public record VacationRequestResponse(
        @Schema(description = "ID do funcionário", example = "123e4567-e89b-12d3-a456-426614174001")
        UUID employeeId,

        @Schema(description = "Nome do funcionário", example = "Carlos Almeida")
        String employeeName,

        @Schema(description = "Data de início do período de férias", example = "2024-12-01")
        @JsonFormat(pattern = DATE_PATTERN)
        LocalDate startDate,

        @Schema(description = "Data de fim do período de férias", example = "2024-12-30")
        @JsonFormat(pattern = DATE_PATTERN)
        LocalDate endDate,

        @Schema(description = "Status atual da solicitação", example = "REQUEST_VACATION")
        String status,

        @Schema(description = "Lista com os IDs individuais de cada dia incluído neste período")
        List<Long> timeRecordIdsForApproval
) {
    public static VacationRequestResponse fromConsolidatedPeriod(Employee employee, List<TimeRecord> period) {
        if (period.isEmpty()) {
            throw new IllegalArgumentException("O período consolidado não pode ser vazio.");
        }

        period.sort(Comparator.comparing(TimeRecord::startWork));

        LocalDate startDate = period.getFirst().startWork().toLocalDate();
        LocalDate endDate = period.getLast().startWork().toLocalDate();
        StatusRecord status = period.getFirst().statusRecord();

        List<Long> ids = period.stream()
                .map(TimeRecord::timeRecordId)
                .collect(Collectors.toList());

        return new VacationRequestResponse(
                employee.employeeId(),
                employee.fullName(),
                startDate,
                endDate,
                status.name(),
                ids
        );
    }
}