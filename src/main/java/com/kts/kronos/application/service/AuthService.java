package com.kts.kronos.application.service;


import com.kts.kronos.adapter.in.web.dto.employee.RecoverPasswordRequest;
import com.kts.kronos.adapter.in.web.dto.security.ResetPasswordRequest;
import com.kts.kronos.adapter.in.web.dto.user.AccessibleCompanyResponse;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.exceptions.TermsNotAcceptedException;
import com.kts.kronos.application.exceptions.TooManyRequestsException;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.port.in.usecase.AuthUseCase;
import com.kts.kronos.application.port.out.provider.*;

import java.util.List;
import com.kts.kronos.application.security.AuthenticationRateLimitService;
import com.kts.kronos.application.security.BiometricProtectionService;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.AuditAction;
import com.kts.kronos.domain.model.enuns.ConsentType;
import com.kts.kronos.observability.application.KronosMetrics;
import com.kts.kronos.observability.application.KronosTracing;
import com.kts.kronos.observability.support.ObservabilityDefaults;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {
    public static final String FACE_NOT_RECOGNIZE = "Face não reconhecida ou não cadastrada.";
    public static final String NO_USER_LINKED_TO_THIS_EMPLOYEE = "Nenhum usuário vinculado a este colaborador.";
    public static final String INACTIVE_USER = "Usuário inativo.";
    public static final String INVALID_IMAGE = "Imagem inválida (Base64 malformado).";
    public static final String ERROR_FACIAL_AUTHENTICATION = "Erro na autenticação facial.";
    public static final String BIOMETRIC_CONSENT_REQUIRED_FOR_FACE_LOGIN =
            "Consentimento biométrico ativo é obrigatório para login facial.";
    @Value("${frontend.base-url-plataform}")
    private String defaultFrontendBaseUrl;

    private final AuthenticationManager authManager;
    private final JwtUtils jwtUtils;
    private final UserProvider userProvider;
    private final EmployeeProvider employeeProvider;
    private final PasswordResetTokenProvider tokenProvider;
    private final EmailSenderProvider emailSenderProvider;
    private final PasswordEncoder passwordEncoder;
    private final FaceRecognitionProvider faceRecognitionProvider;
    private final LegalConsentProvider legalConsentProvider;
    private final AcceptTermsUseCase acceptTermsUseCase;
    private final BiometricProtectionService biometricProtectionService;
    private final TokenBlacklistProvider tokenBlacklistProvider;
    private final AuthenticationRateLimitService authenticationRateLimitService;
    private final AuditService auditService;
    private final AuditRequestContextService auditRequestContextService;
    private final KronosMetrics kronosMetrics;
    private final KronosTracing kronosTracing;
    private final UserCompanyAccessProvider userCompanyAccessProvider;
    private final CompanyProvider companyProvider;

    @Override
    public String login(String username, String password) {
        var normalizedUsername = username.toLowerCase();
        authenticationRateLimitService.checkLoginAllowed(normalizedUsername);
        var auditContext = auditRequestContextService.extractContext();
        String ipAddress = auditContext.ipAddress();
        String userAgent = auditContext.userAgent();

        try {
            authManager.authenticate(new UsernamePasswordAuthenticationToken(normalizedUsername, password));
        } catch (AuthenticationException ex) {
            authenticationRateLimitService.onLoginFailure(normalizedUsername);
            metrics().authLoginFailure("invalid_credentials");
            log.warn("event=auth_login result=failure reason=invalid_credentials");

            // Auditoria de falha de login
            try {
                auditService.registerSecurity(
                        AuditAction.AUTH_LOGIN_FAILURE,
                        null,
                        null,
                        "MEDIUM",
                        "USER",
                        null,
                        "reason=invalid_credentials",
                        ipAddress,
                        userAgent
                );
            } catch (Exception auditEx) {
                log.debug("Falha ao registrar auditoria de login", auditEx);
            }
            throw ex;
        }
        var user = userProvider.findByUsername(normalizedUsername)
                .orElseThrow(() -> {
                    // Spring Security autenticou com sucesso mas o usuário não existe no provider —
                    // inconsistência interna; trata como falha para manter contadores corretos
                    authenticationRateLimitService.onLoginFailure(normalizedUsername);
                    metrics().authLoginFailure("user_not_found");
                    log.warn("event=auth_login result=failure reason=user_not_found");

                    try {
                        auditService.registerSecurity(
                                AuditAction.AUTH_LOGIN_FAILURE,
                                null,
                                null,
                                "MEDIUM",
                                "USER",
                                null,
                                "reason=user_not_found",
                                ipAddress,
                                userAgent
                        );
                    } catch (Exception auditEx) {
                        log.debug("Falha ao registrar auditoria de login", auditEx);
                    }
                    return new ResourceNotFoundException(USER_NOT_FOUND);
                });

        var consentStatus = acceptTermsUseCase.getBiometricConsentStatus(user.employeeId());
        authenticationRateLimitService.onLoginSuccess(normalizedUsername);
        metrics().authLoginSuccess();
        log.info("event=auth_login result=success");

        // Auditoria de sucesso de login
        try {
            auditService.registerSecurity(
                    AuditAction.AUTH_LOGIN_SUCCESS,
                    user.userId(),
                    user.employeeId(),
                    "LOW",
                    "USER",
                    user.userId().toString(),
                    "method=password",
                    ipAddress,
                    userAgent
            );
        } catch (Exception auditEx) {
            log.debug("Falha ao registrar auditoria de login bem-sucedido", auditEx);
        }

        UUID activeCompanyId = resolveActiveCompanyId(user.userId(), user.employeeId());

        return jwtUtils.generateToken(
                user.employeeId(),
                user.username(),
                user.role().name(),
                user.userId(),
                consentStatus,
                user.sessionVersion(),
                activeCompanyId
        );
    }

    @Override
    public String loginFace(String faceImageBase64, Boolean livenessPassed) {
        biometricProtectionService.protectPublicLogin(faceImageBase64, livenessPassed);
        var auditContext = auditRequestContextService.extractContext();
        String ipAddress = auditContext.ipAddress();
        String userAgent = auditContext.userAgent();
        User[] authenticatedFaceUser = new User[1];

        try {
            String token = tracing().observe("kronos.auth.face_login", () -> {
                byte[] imageBytes = Base64.getDecoder().decode(faceImageBase64);
                var inputStream = new ByteArrayInputStream(imageBytes);

                var employeeId = faceRecognitionProvider.searchFaceByImage(inputStream);

                if (employeeId == null) {
                    throw new ForbiddenException(FACE_NOT_RECOGNIZE);
                }

                var user = userProvider.findByEmployeeId(employeeId)
                        .orElseThrow(() -> new ResourceNotFoundException(NO_USER_LINKED_TO_THIS_EMPLOYEE));
                authenticatedFaceUser[0] = user;

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

                UUID activeCompanyIdFace = resolveActiveCompanyId(user.userId(), user.employeeId());

                return jwtUtils.generateToken(
                        user.employeeId(),
                        user.username(),
                        user.role().name(),
                        user.userId(),
                        consentStatus,
                        user.sessionVersion(),
                        activeCompanyIdFace
                );
            });

            metrics().authFaceLoginSuccess();
            log.info("event=auth_face_login result=success");

            // Auditoria de sucesso de login facial
            try {
                auditService.registerSecurity(
                        AuditAction.AUTH_FACE_LOGIN_SUCCESS,
                        authenticatedFaceUser[0] != null ? authenticatedFaceUser[0].userId() : null,
                        authenticatedFaceUser[0] != null ? authenticatedFaceUser[0].employeeId() : null,
                        "MEDIUM",
                        "USER",
                        authenticatedFaceUser[0] != null ? authenticatedFaceUser[0].userId().toString() : null,
                        "method=face",
                        ipAddress,
                        userAgent
                );
            } catch (Exception auditEx) {
                log.debug("Falha ao registrar auditoria de login facial bem-sucedido", auditEx);
            }

            return token;
        } catch (IllegalArgumentException e) {
            metrics().authFaceLoginFailure("invalid_image");
            log.warn("event=auth_face_login result=failure reason=invalid_image");

            // Auditoria de falha - imagem inválida
            try {
                auditService.registerSecurity(
                        AuditAction.AUTH_FACE_LOGIN_FAILURE,
                        null,
                        null,
                        "HIGH",
                        "USER",
                        null,
                        "reason=invalid_image",
                        ipAddress,
                        userAgent
                );
            } catch (Exception auditEx) {
                log.debug("Falha ao registrar auditoria de login facial", auditEx);
            }

            throw new BadRequestException(INVALID_IMAGE);
        } catch (ForbiddenException | ResourceNotFoundException | BadRequestException e) {
            String reason = resolveFaceLoginFailureReason(e);
            metrics().authFaceLoginFailure(reason);
            log.warn("event=auth_face_login result=failure reason={}", reason);

            // Auditoria de falha de login facial
            try {
                String riskLevel = "biometric_consent_missing".equals(reason) ? "HIGH" :
                                 "face_not_recognized".equals(reason) ? "HIGH" : "MEDIUM";
                auditService.registerSecurity(
                        AuditAction.AUTH_FACE_LOGIN_FAILURE,
                        null,
                        null,
                        riskLevel,
                        "USER",
                        null,
                        "reason=" + reason,
                        ipAddress,
                        userAgent
                );
            } catch (Exception auditEx) {
                log.debug("Falha ao registrar auditoria de login facial", auditEx);
            }

            throw e;
        } catch (RuntimeException e) {
            metrics().authFaceLoginFailure("unknown");
            log.error("event=auth_face_login result=failure reason=unknown exception_type={}",
                    e.getClass().getSimpleName());

            // Auditoria de erro desconhecido
            try {
                auditService.registerSecurity(
                        AuditAction.AUTH_FACE_LOGIN_FAILURE,
                        null,
                        null,
                        "HIGH",
                        "USER",
                        null,
                        "reason=unknown",
                        ipAddress,
                        userAgent
                );
            } catch (Exception auditEx) {
                log.debug("Falha ao registrar auditoria de login facial", auditEx);
            }

            throw new BadRequestException(ERROR_FACIAL_AUTHENTICATION);
        }
    }

    @Override
    public void recoverPassword(RecoverPasswordRequest request) {
        String normalizedCpf = request.cpf() == null ? null : request.cpf().trim();
        String normalizedEmail = request.email() == null ? null : request.email().trim();
        metrics().passwordRecoveryRequested();
        log.info("event=password_recovery result=accepted");

        try {
            try {
                authenticationRateLimitService.checkPasswordRecoveryAllowed(normalizedCpf, normalizedEmail);
            } catch (TooManyRequestsException ex) {
                metrics().passwordRecoveryFailure("rate_limited");
                log.warn("event=password_recovery result=failure reason=rate_limited");
                return;
            }

            var employee = employeeProvider.findByCpf(normalizedCpf)
                    .filter(emp -> emp.email() != null
                            && emp.email().trim().equalsIgnoreCase(normalizedEmail))
                    .orElse(null);

            if (employee == null) {
                metrics().passwordRecoveryFailure("identity_not_matched");
                log.info("event=password_recovery result=accepted reason=identity_not_matched");
                return;
            }

            var user = userProvider.findByEmployeeId(employee.employeeId()).orElse(null);

            if (user == null) {
                metrics().passwordRecoveryFailure("user_not_found");
                log.info("event=password_recovery result=accepted reason=user_not_found");
                return;
            }

            var resetToken = tokenProvider.generateAndSaveToken(user.userId());

            log.info(
                    "event=password_recovery_token_created userId={} employeeId={}",
                    user.userId(),
                    employee.employeeId()
            );

            try {
                emailSenderProvider.sendResetEmail(
                        employee.email(),
                        resetToken,
                        user.username(),
                        defaultFrontendBaseUrl
                );
                metrics().passwordRecoveryEmailSent();
                log.info("event=password_recovery result=success reason=email_sent");
            } catch (RuntimeException e) {
                metrics().passwordRecoveryFailure("email_dispatch");
                log.error("event=password_recovery result=failure reason=email_dispatch exception_type={}",
                        e.getClass().getSimpleName());
            }
        } catch (RuntimeException e) {
            metrics().passwordRecoveryFailure("unknown");
            log.error("event=password_recovery result=failure reason=unknown exception_type={}",
                    e.getClass().getSimpleName());
        }
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        var auditContext = auditRequestContextService.extractContext();
        String ipAddress = auditContext.ipAddress();
        String userAgent = auditContext.userAgent();

        try {
            var userId = tokenProvider.validateToken(request.token())
                    .orElseThrow(() -> new ResourceNotFoundException(INVALID_PASSWORD_RESET_TOKEN));

            if (!request.newPassword().equals(request.confirmPassword())) {
                throw new BadRequestException(INVALID_CONFIRM_PASSWORD);
            }
            validatePasswordPolicy(request.newPassword());

            var user = userProvider.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));

            var hashed = passwordEncoder.encode(request.newPassword());

            userProvider.save(user.withPassword(hashed).incrementSessionVersion());

            tokenProvider.deleteToken(request.token());
            metrics().passwordResetSuccess();
            log.info("event=password_reset result=success");

            // Auditoria de sucesso de reset de senha
            try {
                auditService.registerSecurity(
                        AuditAction.AUTH_PASSWORD_RESET,
                        user.userId(),
                        user.employeeId(),
                        "HIGH",
                        "USER",
                        user.userId().toString(),
                        "password_reset=true,sessions_revoked=true",
                        ipAddress,
                        userAgent
                );
            } catch (Exception auditEx) {
                log.debug("Falha ao registrar auditoria de reset de senha", auditEx);
            }
        } catch (BadRequestException e) {
            metrics().passwordResetFailure("validation");
            log.warn("event=password_reset result=failure reason=validation");
            throw e;
        } catch (ResourceNotFoundException e) {
            metrics().passwordResetFailure("token_or_user_not_found");
            log.warn("event=password_reset result=failure reason=token_or_user_not_found");
            throw e;
        } catch (RuntimeException e) {
            metrics().passwordResetFailure("unknown");
            log.error("event=password_reset result=failure reason=unknown exception_type={}",
                    e.getClass().getSimpleName());
            throw e;
        }
    }

    @Override
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank() || !jwtUtils.validateToken(rawToken)) {
            return;
        }
        Date expiration = jwtUtils.getExpirationFromToken(rawToken);
        tokenBlacklistProvider.addToBlacklist(rawToken, expiration);
    }

    @Override
    public String refreshToken(String rawToken) {
        var auditContext = auditRequestContextService.extractContext();
        String ipAddress = auditContext.ipAddress();
        String userAgent = auditContext.userAgent();

        try {
            if (rawToken == null || rawToken.isBlank()) {
                throw new BadRequestException("Token não fornecido.");
            }

            var claims = jwtUtils.getClaimsFromExpiredToken(rawToken);
            if (claims == null) {
                throw new BadRequestException("Token ainda é válido. Nenhuma renovação necessária.");
            }

            if (tokenBlacklistProvider.isBlacklisted(rawToken)) {
                throw new BadRequestException("Token foi revogado.");
            }

            var userId = java.util.UUID.fromString(claims.get("userId", String.class));
            var user = userProvider.findById(userId)
                    .orElseThrow(() -> new BadRequestException("Usuário não encontrado."));

            if (!user.active()) {
                throw new ForbiddenException("Conta de usuário inativa.");
            }

            long tokenSessionVersion = claims.get("session_version", Long.class);
            if (user.sessionVersion() != tokenSessionVersion) {
                throw new BadRequestException("Sessão invalidada. Faça login novamente.");
            }

            var consentStatus = acceptTermsUseCase.getBiometricConsentStatus(user.employeeId());

            String activeCompanyIdStr = claims.get("activeCompanyId", String.class);
            UUID activeCompanyId = (activeCompanyIdStr != null && !activeCompanyIdStr.isBlank())
                    ? UUID.fromString(activeCompanyIdStr)
                    : resolveActiveCompanyId(user.userId(), user.employeeId());

            // Revalida que o usuário ainda tem acesso à empresa ativa
            if (activeCompanyId != null && !userCompanyAccessProvider.existsActiveByUserIdAndCompanyId(user.userId(), activeCompanyId)) {
                activeCompanyId = resolveActiveCompanyId(user.userId(), user.employeeId());
            }

            String newToken = jwtUtils.generateToken(
                    user.employeeId(),
                    user.username(),
                    user.role().name(),
                    user.userId(),
                    consentStatus,
                    user.sessionVersion(),
                    activeCompanyId
            );

            Date oldExpiration = claims.getExpiration();
            tokenBlacklistProvider.addToBlacklist(rawToken, oldExpiration);
            metrics().recordTokenRefresh("success", "none");

            try {
                auditService.registerSecurity(
                        AuditAction.AUTH_TOKEN_REFRESH,
                        user.userId(),
                        user.employeeId(),
                        "LOW",
                        "USER",
                        null,
                        "token_refresh_success",
                        ipAddress,
                        userAgent
                );
            } catch (Exception auditEx) {
                log.debug("Falha ao registrar auditoria de refresh de token", auditEx);
            }

            return newToken;
        } catch (BadRequestException e) {
            metrics().recordTokenRefresh("failure", "validation");
            throw e;
        } catch (ForbiddenException e) {
            metrics().recordTokenRefresh("failure", "inactive_user");
            throw e;
        } catch (RuntimeException e) {
            metrics().recordTokenRefresh("failure", "unknown");
            throw e;
        }
    }

    @Override
    public String switchCompany(UUID userId, UUID targetCompanyId) {
        var auditContext = auditRequestContextService.extractContext();
        String ipAddress = auditContext.ipAddress();
        String userAgent = auditContext.userAgent();

        var user = userProvider.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));

        if (!user.active()) {
            throw new ForbiddenException(INACTIVE_USER);
        }

        var access = userCompanyAccessProvider.findActiveByUserIdAndCompanyId(userId, targetCompanyId)
                .orElseThrow(() -> new ForbiddenException(COMPANY_ACCESS_DENIED));

        var company = companyProvider.findById(targetCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND + targetCompanyId));

        if (!company.active()) {
            throw new ForbiddenException("Empresa inativa.");
        }

        UUID employeeIdForCompany = access.employeeId();

        var consentStatus = employeeIdForCompany != null
                ? acceptTermsUseCase.getBiometricConsentStatus(employeeIdForCompany)
                : acceptTermsUseCase.getBiometricConsentStatus(user.employeeId());

        String newToken = jwtUtils.generateToken(
                employeeIdForCompany != null ? employeeIdForCompany : user.employeeId(),
                user.username(),
                access.role(),
                user.userId(),
                consentStatus,
                user.sessionVersion(),
                targetCompanyId
        );

        try {
            auditService.registerSecurity(
                    AuditAction.COMPANY_SWITCH_SUCCESS,
                    user.userId(),
                    employeeIdForCompany,
                    "LOW",
                    "USER",
                    user.userId().toString(),
                    "targetCompanyId=" + targetCompanyId,
                    ipAddress,
                    userAgent
            );
        } catch (Exception auditEx) {
            log.debug("Falha ao registrar auditoria de troca de empresa", auditEx);
        }

        return newToken;
    }

    @Override
    public List<AccessibleCompanyResponse> getAccessibleCompanies(UUID userId) {
        var accesses = userCompanyAccessProvider.findActiveByUserId(userId);
        return accesses.stream()
                .map(access -> {
                    var company = companyProvider.findById(access.companyId()).orElse(null);
                    if (company == null) return null;
                    return new AccessibleCompanyResponse(
                            access.companyId(),
                            company.name(),
                            company.cnpj(),
                            access.role(),
                            access.defaultCompany(),
                            access.active()
                    );
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private UUID resolveActiveCompanyId(UUID userId, UUID employeeId) {
        var defaultAccess = userCompanyAccessProvider.findDefaultActiveByUserId(userId);
        if (defaultAccess.isPresent()) {
            return defaultAccess.get().companyId();
        }
        var activeAccesses = userCompanyAccessProvider.findActiveByUserId(userId);
        if (!activeAccesses.isEmpty()) {
            return activeAccesses.get(0).companyId();
        }
        if (employeeId != null) {
            return employeeProvider.findById(employeeId)
                    .map(e -> e.companyId())
                    .orElse(null);
        }
        return null;
    }

    // Método auxiliar (copiado de UserService) para validar a política de senha
    private void validatePasswordPolicy(String raw) {
        if (raw == null || !raw.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")) {
            throw new BadRequestException(INVALID_PASSWORD_POLICY);
        }
    }

    private String resolveFaceLoginFailureReason(RuntimeException exception) {
        String message = exception.getMessage();
        if (INVALID_IMAGE.equals(message)) {
            return "invalid_image";
        }
        if (FACE_NOT_RECOGNIZE.equals(message)) {
            return "face_not_recognized";
        }
        if (NO_USER_LINKED_TO_THIS_EMPLOYEE.equals(message)) {
            return "user_not_found";
        }
        if (INACTIVE_USER.equals(message)) {
            return "inactive_user";
        }
        return "unknown";
    }

    private KronosMetrics metrics() {
        return kronosMetrics != null ? kronosMetrics : ObservabilityDefaults.metrics();
    }

    private KronosTracing tracing() {
        return kronosTracing != null ? kronosTracing : ObservabilityDefaults.tracing();
    }

}
