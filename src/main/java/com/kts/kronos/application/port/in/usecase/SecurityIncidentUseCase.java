package com.kts.kronos.application.port.in.usecase;

import com.kts.kronos.adapter.in.web.dto.security.CreateSecurityIncidentRequest;
import com.kts.kronos.adapter.in.web.dto.security.SecurityIncidentResponse;
import com.kts.kronos.adapter.in.web.dto.security.UpdateSecurityIncidentRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SecurityIncidentUseCase {
    SecurityIncidentResponse createIncident(CreateSecurityIncidentRequest request, String ipAddress, String userAgent);
    SecurityIncidentResponse getIncident(UUID incidentId);
    Page<SecurityIncidentResponse> listIncidents(Pageable pageable);
    SecurityIncidentResponse updateIncident(UUID incidentId, UpdateSecurityIncidentRequest request, String ipAddress, String userAgent);
}
