package com.kts.kronos.application.port.in.usecase;

import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;

public interface BiometricTermPdfUseCase {
    byte[] generateConsentTerm(Employee employee, Company company, String ipAddress);
}
