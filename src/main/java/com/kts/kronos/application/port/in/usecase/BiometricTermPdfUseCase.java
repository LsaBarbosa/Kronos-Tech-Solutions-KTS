package com.kts.kronos.application.port.in.usecase;

import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.LegalText;

public interface BiometricTermPdfUseCase {
    byte[] generateConsentTerm(Employee employee, Company company, String ipAddress, String userAgent, LegalText legalText);
}
