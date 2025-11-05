// NOVO ARQUIVO: lsabarbosa/kronos-tech-solutions-kts/Kronos-Tech-Solutions-KTS-2-feature-ferias/src/main/java/com/kts/kronos/adapter/in/web/dto/timerecord/VacationRequestResponse.java

package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.enuns.StatusRecord;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.kts.kronos.constants.Messages.DATE_PATTERN;

public record VacationRequestResponse(
        UUID employeeId,
        String employeeName,
        @JsonFormat(pattern = DATE_PATTERN)
        LocalDate startDate,
        @JsonFormat(pattern = DATE_PATTERN)
        LocalDate endDate,
        String status,
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