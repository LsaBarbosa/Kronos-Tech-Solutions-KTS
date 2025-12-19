package com.kts.kronos.application.port.in.usecase;

import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;

import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.UUID;

public interface AdfUseCase {
    void logMarking(Company company, Employee employee, LocalDateTime date, Long nsr);
    void writeAfdToStream(UUID companyId, OutputStream outputStream);
}
