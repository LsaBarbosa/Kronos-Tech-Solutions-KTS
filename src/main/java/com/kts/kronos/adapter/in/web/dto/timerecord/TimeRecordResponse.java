package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.kts.kronos.domain.model.StatusRecord;
import com.kts.kronos.domain.model.TimeRecord;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

public record TimeRecordResponse(
        Long timeRecordId,
        LocalDateTime startWork,
        LocalDateTime  endWork,
        StatusRecord statusRecord,
        boolean edited,
        boolean active,
        UUID employeeId,
        String hoursWork,
        String balance
) {
    public static TimeRecordResponse fromDomain(TimeRecord tr, Duration reference) {

        var worked = Duration.between(tr.startWork(), tr.endWork());
        long hours = worked.toHours();
        long minutes = worked.toMinutes();
        String hoursWorked = String.format("%02d:%02d",hours,minutes);

        var balance = worked.minus(reference);
        long balanceHours = Math.abs(balance.toHours());
        long balanceMinutes = Math.abs(balance.toMinutesPart());
        String sign = balance.isNegative() ? "-" : "+";
        String balanceResult = sign + String.format("%02d:%02d", balanceHours, balanceMinutes);

        return new TimeRecordResponse(
                tr.timeRecordId(),
                tr.startWork(),
                tr.endWork(),
                tr.statusRecord(),
                tr.edited(),
                tr.active(),
                tr.employeeId(),
                hoursWorked,
                balanceResult
        );
    }
}