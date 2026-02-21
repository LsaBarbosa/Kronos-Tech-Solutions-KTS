package com.kts.kronos.adapter.in.web.dto.timerecord;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import static com.kts.kronos.constants.Messages.DATE_PATTERN;
import static com.kts.kronos.constants.Messages.SAO_PAULO;
import static com.kts.kronos.constants.Messages.TIME_FORMATTER;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.enuns.StatusRecord;

@Schema(description = "Detalhes consolidados de um único registro de ponto, abono ou ausência")
public record TimeRecordResponse(
        @Schema(description = "ID do registro", example = "1050")
        Long timeRecordId,

        @Schema(description = "Data e hora exata do início", example = "2024-05-10T08:00:00")
        @JsonFormat(pattern = DATE_PATTERN)
        LocalDateTime startWork,

        @Schema(description = "Apenas a hora formatada do início", example = "08:00")
        String startHour,

        @Schema(description = "Data e hora exata do fim", example = "2024-05-10T17:00:00")
        @JsonFormat(pattern = DATE_PATTERN)
        LocalDateTime endWork,

        @Schema(description = "Apenas a hora formatada do fim", example = "17:00")
        String endHour,

        @Schema(description = "Total de horas apuradas no registro", example = "09:00")
        String hoursWork,

        @Schema(description = "Saldo positivo ou negativo gerado pelo registro", example = "+01:00")
        String balance,

        @Schema(description = "Status atual deste registro no ciclo de vida", example = "UPDATED")
        StatusRecord statusRecord,

        @Schema(description = "Indica se o registro sofreu edição manual", example = "true")
        boolean edited,

        @Schema(description = "Indica se o registro está ativo para cálculos", example = "true")
        boolean active,

        @Schema(description = "ID interno do funcionário", example = "123e4567...")
        UUID employeeId,

        @Schema(description = "Dados consolidados do funcionário e empresa associada")
        EmployeeData employeeData,

        @Schema(description = "Caminho do documento associado (ex: atestado médico, comprovativo)", example = "/docs/123.pdf")
        String documentDownloadPath,

        @Schema(description = "Latitude na entrada", example = "-22.9068")
        Double latitude,

        @Schema(description = "Longitude na entrada", example = "-43.1729")
        Double longitude,

        @Schema(description = "Latitude na saída", example = "-22.9068")
        Double endLatitude,

        @Schema(description = "Longitude na saída", example = "-43.1729")
        Double endLongitude
) {
     public static TimeRecordResponse fromDomain(TimeRecord timeRecord,
                                                Duration reference,
                                                EmployeeData employeeData,
                                                String documentDownloadPath,
                                                String dailyBalance) {

        var startDateTime = timeRecord.startWork()
                .atZone(SAO_PAULO).toLocalDateTime();
        var startHour = startDateTime.toLocalTime().format(TIME_FORMATTER);

        LocalDateTime endDateTime = null;
        String endHour = "";
        if (timeRecord.endWork() != null) {
            endDateTime = timeRecord.endWork()
                    .atZone(SAO_PAULO)
                    .toLocalDateTime();
            endHour = endDateTime.toLocalTime()
                    .format(TIME_FORMATTER);
        }

        String hoursWorked = "";
        String balanceString = "";

        if (endDateTime != null) {
            Duration worked = Duration.between(timeRecord.startWork(), timeRecord.endWork());
            hoursWorked = String.format("%02d:%02d",
                    worked.toHours(),
                    worked.toMinutesPart()
            );

             if (dailyBalance != null) {
                balanceString = dailyBalance;
            } else {
                 if (timeRecord.statusRecord() == StatusRecord.ABSENCE) {
                    balanceString = String.format("-%02d:%02d",
                            reference.toHours(),
                            reference.toMinutesPart()
                    );
                } else if (isBalanceZeroStatus(timeRecord.statusRecord())) {
                    balanceString = "+00:00";
                } else {
                    Duration balance = worked.minus(reference);
                    String sign = balance.isNegative() ? "-" : "+";
                    balanceString = sign + String.format("%02d:%02d",
                            Math.abs(balance.toHours()),
                            Math.abs(balance.toMinutesPart())
                    );
                }
            }
        }

        return new TimeRecordResponse(
                timeRecord.timeRecordId(),
                startDateTime,
                startHour,
                endDateTime,
                endHour,
                hoursWorked,
                balanceString,
                timeRecord.statusRecord(),
                timeRecord.edited(),
                timeRecord.active(),
                timeRecord.employeeId(),
                employeeData,
                documentDownloadPath,
                timeRecord.latitude(),
                timeRecord.longitude(),
                timeRecord.endLatitude(),
                timeRecord.endLongitude()
        );
    }

    private static boolean isBalanceZeroStatus(StatusRecord status) {
        return status == StatusRecord.DAY_OFF
                || status == StatusRecord.TIME_OFF
                || status == StatusRecord.IMPLICIT_BREAK
                || status == StatusRecord.REQUEST_VACATION
                || status == StatusRecord.VACATION
                || status == StatusRecord.VACATION_REJECTED;
    }
}