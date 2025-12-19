package com.kts.kronos.application.port.in.usecase;

import java.time.LocalDate;
import java.util.UUID;

public interface PointMirrorPdfUseCase {
    byte[] generateMirror(UUID employeeId, LocalDate startDate, LocalDate endDate);
}
