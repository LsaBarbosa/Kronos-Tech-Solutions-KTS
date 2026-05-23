package com.kts.kronos.application.port.in.usecase;

import com.kts.kronos.adapter.in.web.dto.security.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SecurityIncidentUseCase {
    SecurityIncidentResponse createIncident(CreateSecurityIncidentRequest request, String ipAddress, String userAgent);
    SecurityIncidentResponse getIncident(UUID incidentId);
    Page<SecurityIncidentResponse> listIncidents(Pageable pageable);
    SecurityIncidentResponse updateIncident(UUID incidentId, UpdateSecurityIncidentRequest request, String ipAddress, String userAgent);
    SecurityIncidentResponse evaluateRisk(UUID incidentId, SecurityIncidentRiskAssessmentRequest request, String ipAddress, String userAgent);
    SecurityIncidentResponse submitCorrectionPlan(UUID incidentId, SecurityIncidentCorrectionPlanRequest request, String ipAddress, String userAgent);
    SecurityIncidentReportResponse generateReport(UUID incidentId);
}
