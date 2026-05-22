package com.kts.kronos.application.service;

import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.application.port.out.provider.FaceStorageProvider;
import com.kts.kronos.application.port.out.provider.LegalConsentProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.domain.model.enuns.AuditAction;
import com.kts.kronos.domain.model.enuns.ConsentType;
import com.kts.kronos.domain.model.enuns.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.ERROR_TO_GENERATE_HASH;
import static com.kts.kronos.constants.Messages.UNAUTHORIZED_ROLE_OPERATION;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeAnonymizationService {
    private static final HexFormat HEX = HexFormat.of();
    private static final String ANONYMIZATION_REASON = "LGPD_ANONYMIZATION";

    private final JwtAuthenticatedUser jwtAuthenticatedUser;
    private final DomainAuthorizationService domainAuthorizationService;
    private final EmployeeProvider employeeProvider;
    private final UserProvider userProvider;
    private final FaceStorageProvider faceStorageProvider;
    private final FaceRecognitionProvider faceRecognitionProvider;
    private final LegalConsentProvider legalConsentProvider;
    private final AuditService auditService;

    public void anonymize(UUID employeeId, String ipAddress, String userAgent, UUID actorUserId) {
        requireAdministrativeRole();
        var employee = domainAuthorizationService.authorizeEmployeeAccess(employeeId);
        var deletedAt = LocalDateTime.now();
        var linkedUser = userProvider.findByEmployeeId(employee.employeeId());

        if (employee.faceS3ObjectKey() != null && !employee.faceS3ObjectKey().isBlank()) {
            faceStorageProvider.deleteFaceImage(employee.faceS3ObjectKey());
        }
        faceRecognitionProvider.deleteFacesByExternalImageId(employee.employeeId());

        legalConsentProvider.findActive(employee.employeeId(), ConsentType.BIOMETRIC_AUTHENTICATION)
                .ifPresent(consent -> legalConsentProvider.save(consent.revoke(Instant.now())));

        var anonymizedEmployee = employee.anonymize(
                "ANONYMIZED-" + employee.employeeId(),
                buildAnonymizedCpf(employee.employeeId(), employee.cpf()),
                "anon-" + employee.employeeId() + "@deleted.local",
                null,
                actorUserId,
                deletedAt,
                ANONYMIZATION_REASON
        );
        employeeProvider.save(anonymizedEmployee);

        linkedUser
                .map(user -> user.deactivate(actorUserId, ANONYMIZATION_REASON))
                .ifPresent(userProvider::save);

        auditService.registerLgpd(
                AuditAction.LGPD_DATA_ANONYMIZED,
                employee.employeeId(),
                employee.companyId(),
                "EMPLOYEE",
                employee.employeeId().toString(),
                "HIGH",
                String.format(
                        "Anonimizacao LGPD concluida. employeeId=%s, linkedUserPresent=%s",
                        employee.employeeId(),
                        linkedUser.isPresent()
                ),
                ipAddress,
                userAgent
        );
    }

    private void requireAdministrativeRole() {
        var currentRole = jwtAuthenticatedUser.getCurrentRole();
        if (currentRole != Role.MANAGER && currentRole != Role.CTO) {
            throw new ForbiddenException(UNAUTHORIZED_ROLE_OPERATION);
        }
    }

    private String buildAnonymizedCpf(UUID employeeId, String cpf) {
        String hash = sha256(employeeId + ":" + cpf);
        return "ANON" + hash.substring(0, 10);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HEX.formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(ERROR_TO_GENERATE_HASH, e);
        }
    }
}
