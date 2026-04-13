package com.kts.kronos.application.port.out.projection;

import java.time.LocalDate;
import java.util.UUID;

public interface VacationRequestPeriodProjection {
    UUID getEmployeeId();
    String getEmployeeName();
    LocalDate getStartDate();
    LocalDate getEndDate();
    String getStatus();
    String getTimeRecordIdsCsv();
}