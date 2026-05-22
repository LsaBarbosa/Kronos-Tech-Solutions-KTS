package com.kts.kronos.application.service;


import com.kts.kronos.adapter.in.web.dto.employee.RecoverPasswordRequest;
import com.kts.kronos.adapter.in.web.dto.security.ResetPasswordRequest;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.exceptions.TooManyRequestsException;
import com.kts.kronos.application.port.in.usecase.AuthUseCase;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.application.security.AuthenticationRateLimitService;
import com.kts.kronos.application.security.BiometricProtectionService;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.ConsentType;
import com.kts.kronos.observability.application.KronosMetrics;
import com.kts.kronos.observability.application.KronosTracing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Date;

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
    private final BiometricProtectionService biometricProtectionService;
    private final TokenBlacklistProvider tokenBlacklistProvider;
    private final AuthenticationRateLimitService authenticationRateLimitService;
    @Autowired
    private KronosMetrics kronosMetrics = new KronosMetrics();
    @Autowired
    private KronosTracing kronosTracing = new KronosTracing();

    @Override
    public String login(String username, String password) {
        var normalizedUsername = username.toLowerCase();
        authenticationRateLimitService.checkLoginAllowed(normalizedUsername);
        try {
            authManager.authenticate(new UsernamePasswordAuthenticationToken(normalizedUsername, password));
        } catch (AuthenticationException ex) {
            authenticationRateLimitService.onLoginFailure(normalizedUsername);
            kronosMetrics.authLoginFailure("invalid_credentials");
            log.warn("event=auth_login result=failure reason=invalid_credentials");
            throw ex;
        }
        var user = userProvider.findByUsername(normalizedUsername)
                .orElseThrow(() -> {
                    kronosMetrics.authLoginFailure("user_not_found");
                    log.warn("event=auth_login result=failure reason=user_not_found");
                    return new ResourceNotFoundException(USER_NOT_FOUND);
                });
        var termsAccepted = legalConsentProvider.existsActive(
                user.employeeId(),
                ConsentType.BIOMETRIC_AUTHENTICATION
        );
        authenticationRateLimitService.onLoginSuccess(normalizedUsername);
        kronosMetrics.authLoginSuccess();
        log.info("event=auth_login result=success");
        return jwtUtils.generateToken(user.employeeId(), user.username(), user.role().name(), user.userId(), termsAccepted);
    }

    @Override
    public String loginFace(String faceImageBase64, Boolean livenessPassed) {
        biometricProtectionService.protectPublicLogin(faceImageBase64, livenessPassed);

        try {
            String token = kronosTracing.observe("kronos.auth.face_login", () -> {
                byte[] imageBytes = Base64.getDecoder().decode(faceImageBase64);
                var inputStream = new ByteArrayInputStream(imageBytes);

                var employeeId = faceRecognitionProvider.searchFaceByImage(inputStream);

                if (employeeId == null) {
                    throw new ForbiddenException(FACE_NOT_RECOGNIZE);
                }

                var user = userProvider.findByEmployeeId(employeeId)
                        .orElseThrow(() -> new ResourceNotFoundException(NO_USER_LINKED_TO_THIS_EMPLOYEE));

                if (!user.active()) {
                    throw new BadRequestException(INACTIVE_USER);
                }

                var termsAccepted = legalConsentProvider.existsActive(
                        user.employeeId(),
                        ConsentType.BIOMETRIC_AUTHENTICATION
                );

                return jwtUtils.generateToken(
                        user.employeeId(),
                        user.username(),
                        user.role().name(),
                        user.userId(),
                        termsAccepted
                );
            });

            kronosMetrics.authFaceLoginSuccess();
            log.info("event=auth_face_login result=success");
            return token;
        } catch (IllegalArgumentException e) {
            kronosMetrics.authFaceLoginFailure("invalid_image");
            log.warn("event=auth_face_login result=failure reason=invalid_image");
            throw new BadRequestException(INVALID_IMAGE);
        } catch (ForbiddenException | ResourceNotFoundException | BadRequestException e) {
            String reason = resolveFaceLoginFailureReason(e);
            kronosMetrics.authFaceLoginFailure(reason);
            log.warn("event=auth_face_login result=failure reason={}", reason);
            throw e;
        } catch (RuntimeException e) {
            kronosMetrics.authFaceLoginFailure("unknown");
            log.error("event=auth_face_login result=failure reason=unknown exception_type={}",
                    e.getClass().getSimpleName());
            throw new BadRequestException(ERROR_FACIAL_AUTHENTICATION);
        }
    }

    @Override
    public void recoverPassword(RecoverPasswordRequest request) {
        String normalizedCpf = request.cpf() == null ? null : request.cpf().trim();
        String normalizedEmail = request.email() == null ? null : request.email().trim();
        kronosMetrics.passwordRecoveryRequested();
        log.info("event=password_recovery result=accepted");

        try {
            try {
                authenticationRateLimitService.checkPasswordRecoveryAllowed(normalizedCpf, normalizedEmail);
            } catch (TooManyRequestsException ex) {
                kronosMetrics.passwordRecoveryFailure("rate_limited");
                log.warn("event=password_recovery result=failure reason=rate_limited");
                return;
            }

            var employee = employeeProvider.findByCpf(normalizedCpf)
                    .filter(emp -> emp.email() != null && emp.email().equalsIgnoreCase(normalizedEmail))
                    .orElse(null);

            if (employee == null) {
                kronosMetrics.passwordRecoveryFailure("identity_not_matched");
                log.info("event=password_recovery result=accepted reason=identity_not_matched");
                return;
            }

            var user = userProvider.findByEmployeeId(employee.employeeId()).orElse(null);

            if (user == null) {
                kronosMetrics.passwordRecoveryFailure("user_not_found");
                log.info("event=password_recovery result=accepted reason=user_not_found");
                return;
            }

            var resetToken = tokenProvider.generateAndSaveToken(user.userId());

            try {
                emailSenderProvider.sendResetEmail(
                        employee.email(),
                        resetToken,
                        user.username(),
                        defaultFrontendBaseUrl
                );
                kronosMetrics.passwordRecoveryEmailSent();
                log.info("event=password_recovery result=success reason=email_sent");
            } catch (RuntimeException e) {
                kronosMetrics.passwordRecoveryFailure("email_dispatch");
                log.error("event=password_recovery result=failure reason=email_dispatch exception_type={}",
                        e.getClass().getSimpleName());
            }
        } catch (RuntimeException e) {
            kronosMetrics.passwordRecoveryFailure("unknown");
            log.error("event=password_recovery result=failure reason=unknown exception_type={}",
                    e.getClass().getSimpleName());
        }
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
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

            var updatedUser = new User(
                    user.userId(),
                    user.username(),
                    hashed,
                    user.role(),
                    user.active(),
                    user.employeeId()
            );
            userProvider.save(updatedUser);

            tokenProvider.deleteToken(request.token());
            kronosMetrics.passwordResetSuccess();
            log.info("event=password_reset result=success");
        } catch (BadRequestException e) {
            kronosMetrics.passwordResetFailure("validation");
            log.warn("event=password_reset result=failure reason=validation");
            throw e;
        } catch (ResourceNotFoundException e) {
            kronosMetrics.passwordResetFailure("token_or_user_not_found");
            log.warn("event=password_reset result=failure reason=token_or_user_not_found");
            throw e;
        } catch (RuntimeException e) {
            kronosMetrics.passwordResetFailure("unknown");
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
}
