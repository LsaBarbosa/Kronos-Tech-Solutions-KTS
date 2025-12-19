package com.kts.kronos.application.port.in.usecase;

import java.io.OutputStream;
import java.time.LocalDate;
import java.util.UUID;
public interface AejUseCase {
    void generateAej(UUID companyId, LocalDate startDate, LocalDate endDate, OutputStream outputStream);
}
