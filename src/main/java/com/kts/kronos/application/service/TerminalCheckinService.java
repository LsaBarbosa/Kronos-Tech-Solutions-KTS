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
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.AuditAction;
import com.kts.kronos.observability.application.KronosMetrics;
import com.kts.kronos.observability.application.KronosTracing;
import com.kts.kronos.observability.support.ObservabilityDefaults;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.kts.kronos.application.service.AuthService.BIOMETRIC_CONSENT_REQUIRED_FOR_FACE_LOGIN;
import static com.kts.kronos.application.service.AuthService.FACE_NOT_RECOGNIZE;
import static com.kts.kronos.application.service.AuthService.INVALID_IMAGE;
import static com.kts.kronos.application.service.AuthService.NO_USER_LINKED_TO_THIS_EMPLOYEE;
import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider.FaceMatchCandidate;

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

                var faceMatches = faceRecognitionProvider.searchFacesByImage(inputStream);
                if (faceMatches == null || faceMatches.isEmpty()) {
                    throw new ForbiddenException(FACE_NOT_RECOGNIZE);
                }

                var resolved = resolveUserAndTerminalTarget(faceMatches, request.latitude(), request.longitude())
                        .orElseThrow(() -> new ForbiddenException(FACE_NOT_RECOGNIZE));
                var user = resolved.user();
                var resolution = resolved.resolution();
                var employeeId = user.employeeId();
                if (employeeId == null) {
                    throw new ResourceNotFoundException(NO_USER_LINKED_TO_THIS_EMPLOYEE);
                }

                var consentStatus = acceptTermsUseCase.getBiometricConsentStatus(user.employeeId());
                if (!consentStatus.accepted()) {
                    throw new TermsNotAcceptedException(
                            BIOMETRIC_CONSENT_REQUIRED_FOR_FACE_LOGIN,
                            "https://termo.kronossolutions.tech/"
                    );
                }

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

    private record ResolvedTerminalUser(User user, TerminalResolution resolution) {}

    private record NearbyTerminalTarget(TerminalResolution resolution, double distanceMeters) {}

    private Optional<ResolvedTerminalUser> resolveUserAndTerminalTarget(
            List<FaceMatchCandidate> faceMatches, double latitude, double longitude) {
        ResolvedTerminalUser fallback = null;
        double nearestDistance = Double.MAX_VALUE;

        for (var faceMatch : faceMatches) {
            var userOpt = userProvider.findByEmployeeId(faceMatch.employeeId());
            if (userOpt.isEmpty() || !userOpt.get().active()) {
                continue;
            }

            var user = userOpt.get();
            if (fallback == null) {
                fallback = new ResolvedTerminalUser(user,
                        resolveTerminalTargetFallback(user.employeeId(), user.role().name()));
            }

            var nearby = resolveNearbyTerminalTarget(user.userId(), user.employeeId(), user.role().name(), latitude, longitude);
            if (nearby.isPresent() && nearby.get().distanceMeters() < nearestDistance) {
                nearestDistance = nearby.get().distanceMeters();
                fallback = new ResolvedTerminalUser(user, nearby.get().resolution());
            }
        }

        return Optional.ofNullable(fallback);
    }

    private Optional<NearbyTerminalTarget> resolveNearbyTerminalTarget(
            UUID userId, UUID fallbackEmployeeId, String fallbackRole,
            double latitude, double longitude) {
        NearbyTerminalTarget nearest = null;
        for (var access : userCompanyAccessProvider.findActiveByUserId(userId)) {
            if (access.employeeId() == null) continue;
            var companyOpt = companyProvider.findById(access.companyId());
            if (companyOpt.isEmpty() || companyOpt.get().location() == null) continue;
            var location = companyOpt.get().location();
            double distance = geoDistanceMeters(location.latitude(), location.longitude(), latitude, longitude);
            if (distance <= 80.0 && (nearest == null || distance < nearest.distanceMeters())) {
                String role = access.role() != null ? access.role() : fallbackRole;
                nearest = new NearbyTerminalTarget(
                        new TerminalResolution(access.employeeId(), access.companyId(), role), distance);
            }
        }
        if (nearest != null) {
            log.info("event=terminal_company_resolved companyId={} distance_m={}",
                    nearest.resolution().companyId(), (int) nearest.distanceMeters());
        }
        return Optional.ofNullable(nearest);
    }

    private TerminalResolution resolveTerminalTargetFallback(UUID fallbackEmployeeId, String fallbackRole) {
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
