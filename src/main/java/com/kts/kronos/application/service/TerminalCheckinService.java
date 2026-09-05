package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.terminal.TerminalCheckinRequest;
import com.kts.kronos.adapter.in.web.dto.terminal.TerminalCheckinResponse;
import com.kts.kronos.adapter.in.web.dto.timerecord.GeolocationRequest;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.exceptions.TermsNotAcceptedException;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.port.in.usecase.TerminalCheckinUseCase;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.application.port.out.provider.UserCompanyAccessProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.application.service.AuditRequestContextService;
import com.kts.kronos.application.security.BiometricProtectionService;
import com.kts.kronos.domain.model.enuns.AuditAction;
import com.kts.kronos.observability.application.KronosMetrics;
import com.kts.kronos.observability.application.KronosTracing;
import com.kts.kronos.observability.support.ObservabilityDefaults;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.kts.kronos.domain.model.UserCompanyAccess;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static com.kts.kronos.application.service.AuthService.BIOMETRIC_CONSENT_REQUIRED_FOR_FACE_LOGIN;
import static com.kts.kronos.application.service.AuthService.FACE_NOT_RECOGNIZE;
import static com.kts.kronos.application.service.AuthService.INACTIVE_USER;
import static com.kts.kronos.application.service.AuthService.INVALID_IMAGE;
import static com.kts.kronos.application.service.AuthService.NO_USER_LINKED_TO_THIS_EMPLOYEE;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TerminalCheckinService implements TerminalCheckinUseCase {

    private final BiometricProtectionService biometricProtectionService;
    private final FaceRecognitionProvider faceRecognitionProvider;
    private final UserProvider userProvider;
    private final EmployeeProvider employeeProvider;
    private final CompanyProvider companyProvider;
    private final AcceptTermsUseCase acceptTermsUseCase;
    private final TimeRecordService timeRecordService;
    private final JwtUtils jwtUtils;
    private final UserCompanyAccessProvider userCompanyAccessProvider;
    private final AuditService auditService;
    private final AuditRequestContextService auditRequestContextService;
    private final KronosMetrics kronosMetrics;
    private final KronosTracing kronosTracing;

    @Override
    public TerminalCheckinResult checkinByFace(TerminalCheckinRequest request) {
        var auditContext = auditRequestContextService.extractContext();
        String ipAddress = auditContext.ipAddress();
        String userAgent = auditContext.userAgent();

        try {
            return tracing().observe("kronos.terminal.checkin", () -> {
                biometricProtectionService.protectTerminalCheckin(request.faceImageBase64(), request.livenessPassed());

                byte[] imageBytes = Base64.getDecoder().decode(request.faceImageBase64());
                var inputStream = new ByteArrayInputStream(imageBytes);

                // Única chamada ao Rekognition — resultado reaproveitado para auth e checkin
                var employeeId = faceRecognitionProvider.searchFaceByImage(inputStream);
                if (employeeId == null) {
                    throw new ForbiddenException(FACE_NOT_RECOGNIZE);
                }

                var user = userProvider.findByEmployeeId(employeeId)
                        .orElseThrow(() -> new ResourceNotFoundException(NO_USER_LINKED_TO_THIS_EMPLOYEE));

                if (!user.active()) {
                    throw new BadRequestException(INACTIVE_USER);
                }

                var consentStatus = acceptTermsUseCase.getBiometricConsentStatus(user.employeeId());
                if (!consentStatus.accepted()) {
                    throw new TermsNotAcceptedException(
                            BIOMETRIC_CONSENT_REQUIRED_FOR_FACE_LOGIN,
                            "https://termo.kronossolutions.tech/"
                    );
                }

                // Resolve qual empresa registrar com base na geolocalização do terminal
                var resolution = resolveTerminalTarget(
                        user.userId(), employeeId, user.role().name(),
                        request.latitude(), request.longitude()
                );

                var geoRequest = new GeolocationRequest(
                        request.latitude(),
                        request.longitude(),
                        request.faceImageBase64(),
                        request.livenessPassed()
                );
                var actionResponse = timeRecordService.registerTimeFromTerminal(resolution.employeeId(), geoRequest);

                String jwtToken = jwtUtils.generateToken(
                        resolution.employeeId(),
                        user.username(),
                        resolution.role(),
                        user.userId(),
                        consentStatus,
                        user.sessionVersion(),
                        resolution.companyId()
                );

                metrics().authFaceLoginSuccess();
                log.info("event=terminal_checkin result=success action={}", actionResponse.actionType());

                try {
                    auditService.registerSecurity(
                            AuditAction.AUTH_FACE_LOGIN_SUCCESS,
                            user.userId(),
                            user.employeeId(),
                            "MEDIUM",
                            "USER",
                            user.userId().toString(),
                            "method=terminal_checkin action=" + actionResponse.actionType(),
                            ipAddress,
                            userAgent
                    );
                } catch (Exception auditEx) {
                    log.debug("Falha ao registrar auditoria de terminal checkin", auditEx);
                }

                return new TerminalCheckinResult(
                        jwtToken,
                        new TerminalCheckinResponse(actionResponse.actionType(), actionResponse.message())
                );
            });
        } catch (IllegalArgumentException e) {
            metrics().authFaceLoginFailure("invalid_image");
            log.warn("event=terminal_checkin result=failure reason=invalid_image");
            throw new BadRequestException(INVALID_IMAGE);
        } catch (ForbiddenException | ResourceNotFoundException | BadRequestException e) {
            log.warn("event=terminal_checkin result=failure reason={}", e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            metrics().authFaceLoginFailure("unknown");
            log.error("event=terminal_checkin result=failure reason=unknown exception_type={}", e.getClass().getSimpleName());
            throw e;
        }
    }

    private record TerminalResolution(UUID employeeId, UUID companyId, String role) {}

    private TerminalResolution resolveTerminalTarget(
            UUID userId, UUID fallbackEmployeeId, String fallbackRole,
            double latitude, double longitude
    ) {
        List<UserCompanyAccess> accesses =
                userCompanyAccessProvider.findActiveByUserId(userId);

        for (var access : accesses) {
            if (access.employeeId() == null) continue;
            var companyOpt = companyProvider.findById(access.companyId());
            if (companyOpt.isEmpty()) continue;
            var location = companyOpt.get().location();
            if (location == null) continue;
            double dist = geoDistanceMeters(location.latitude(), location.longitude(), latitude, longitude);
            if (dist <= 80.0) {
                String role = access.role() != null ? access.role() : fallbackRole;
                log.info("event=terminal_company_resolved companyId={} distance_m={}", access.companyId(), (int) dist);
                return new TerminalResolution(access.employeeId(), access.companyId(), role);
            }
        }

        // Fallback: empresa do employee original
        UUID companyId = employeeProvider.findById(fallbackEmployeeId)
                .map(e -> e.companyId())
                .orElse(null);
        log.info("event=terminal_company_resolved_fallback companyId={}", companyId);
        return new TerminalResolution(fallbackEmployeeId, companyId, fallbackRole);
    }

    private double geoDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)) * 1000;
    }

    private KronosMetrics metrics() {
        return kronosMetrics != null ? kronosMetrics : ObservabilityDefaults.metrics();
    }

    private KronosTracing tracing() {
        return kronosTracing != null ? kronosTracing : ObservabilityDefaults.tracing();
    }
}
