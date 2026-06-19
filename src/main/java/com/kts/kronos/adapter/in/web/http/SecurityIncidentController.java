package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.security.*;
import com.kts.kronos.application.port.in.usecase.SecurityIncidentUseCase;
import com.kts.kronos.application.security.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.kts.kronos.constants.ApiPaths.*;
import static com.kts.kronos.constants.Messages.KRONOS;

@RestController
@RequestMapping(SECURITY_INCIDENTS)
@RequiredArgsConstructor
public class SecurityIncidentController {

    private final SecurityIncidentUseCase securityIncidentUseCase;
    private final ClientIpResolver clientIpResolver;

    @PostMapping
    @PreAuthorize(KRONOS)
    public ResponseEntity<SecurityIncidentResponse> createIncident(
            @Valid @RequestBody CreateSecurityIncidentRequest request,
            HttpServletRequest httpRequest
    ) {
        String ipAddress = clientIpResolver.resolve(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        if (userAgent == null) {
            userAgent = "unknown";
        }

        var response = securityIncidentUseCase.createIncident(request, ipAddress, userAgent);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize(KRONOS)
    public ResponseEntity<Page<SecurityIncidentResponse>> listIncidents(Pageable pageable) {
        var response = securityIncidentUseCase.listIncidents(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping(SECURITY_INCIDENT_ID)
    @PreAuthorize(KRONOS)
    public ResponseEntity<SecurityIncidentResponse> getIncident(@PathVariable UUID incidentId) {
        var response = securityIncidentUseCase.getIncident(incidentId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping(SECURITY_INCIDENT_ID)
    @PreAuthorize(KRONOS)
    public ResponseEntity<SecurityIncidentResponse> updateIncident(
            @PathVariable UUID incidentId,
            @Valid @RequestBody UpdateSecurityIncidentRequest request,
            HttpServletRequest httpRequest
    ) {
        String ipAddress = clientIpResolver.resolve(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        if (userAgent == null) {
            userAgent = "unknown";
        }

        var response = securityIncidentUseCase.updateIncident(incidentId, request, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }

    @PostMapping(SECURITY_INCIDENT_ID + "/evaluate-risk")
    @PreAuthorize(KRONOS)
    public ResponseEntity<SecurityIncidentResponse> evaluateRisk(
            @PathVariable UUID incidentId,
            @Valid @RequestBody SecurityIncidentRiskAssessmentRequest request,
            HttpServletRequest httpRequest
    ) {
        String ipAddress = clientIpResolver.resolve(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        if (userAgent == null) {
            userAgent = "unknown";
        }

        var response = securityIncidentUseCase.evaluateRisk(incidentId, request, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }

    @PostMapping(SECURITY_INCIDENT_ID + "/correction-plan")
    @PreAuthorize(KRONOS)
    public ResponseEntity<SecurityIncidentResponse> submitCorrectionPlan(
            @PathVariable UUID incidentId,
            @Valid @RequestBody SecurityIncidentCorrectionPlanRequest request,
            HttpServletRequest httpRequest
    ) {
        String ipAddress = clientIpResolver.resolve(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        if (userAgent == null) {
            userAgent = "unknown";
        }

        var response = securityIncidentUseCase.submitCorrectionPlan(incidentId, request, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }

    @GetMapping(SECURITY_INCIDENT_ID + "/report")
    @PreAuthorize(KRONOS)
    public ResponseEntity<SecurityIncidentReportResponse> generateReport(
            @PathVariable UUID incidentId
    ) {
        var response = securityIncidentUseCase.generateReport(incidentId);
        return ResponseEntity.ok(response);
    }
}
