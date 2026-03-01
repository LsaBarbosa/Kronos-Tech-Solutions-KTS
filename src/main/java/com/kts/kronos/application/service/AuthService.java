package com.kts.kronos.application.service;


import com.kts.kronos.adapter.in.web.dto.employee.RecoverPasswordRequest;
import com.kts.kronos.adapter.in.web.dto.security.ResetPasswordRequest;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.adapter.out.security.PasswordPolicyValidator;
import com.kts.kronos.application.exceptions.*;
import com.kts.kronos.application.port.in.usecase.AuthUseCase;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.DocumentType;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.rekognition.model.RekognitionException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Locale;

import static com.kts.kronos.constants.Messages.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {
    public static final String FACE_NOT_RECOGNIZE = "Face não reconhecida ou não cadastrada.";
    public static final String NO_USER_LINKED_TO_THIS_EMPLOYEE = "Nenhum usuário vinculado a este colaborador.";
    public static final String INACTIVE_USER = "Usuário inativo.";
    public static final String INVALID_IMAGE = "Imagem inválida (Base64 malformado).";
    public static final String ERROR_FACIAL_AUTHENTICATION = "Erro inesperado na autenticação facial.";
    public static final String ERROR_FACIAL_AUTHENTICATION_UNAVAILABLE = "Serviço de autenticação facial indisponível. Tente novamente.";
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final Duration LOGIN_BLOCK_DURATION = Duration.ofMinutes(15);
    private static final int MAX_RECOVERY_ATTEMPTS = 3;
    private static final Duration RECOVERY_BLOCK_DURATION = Duration.ofMinutes(15);

    private final Map<String, AttemptWindow> loginAttempts = new ConcurrentHashMap<>();
    private final Map<String, AttemptWindow> recoveryAttempts = new ConcurrentHashMap<>();


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
    private final DocumentProvider documentProvider;
    private final MeterRegistry meterRegistry;
    private final PasswordPolicyValidator passwordPolicyValidator;

    @Override
    @Transactional(readOnly = true)
    public String login(String username, String password) {
        var timerSample = Timer.start(meterRegistry);

        var normalizedUsername = username.trim().toLowerCase(Locale.ROOT);
        enforceNotBlocked(loginAttempts, normalizedUsername, "Muitas tentativas de login. Tente novamente mais tarde.");

        try {
            authManager.authenticate(new UsernamePasswordAuthenticationToken(normalizedUsername, password));
            resetAttempts(loginAttempts, normalizedUsername);
        } catch (RuntimeException ex) {
            registerFailedAttempt(loginAttempts, normalizedUsername, MAX_LOGIN_ATTEMPTS, LOGIN_BLOCK_DURATION);
            throw ex;
        }

        var user = userProvider.findByUsername(normalizedUsername)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));

        var termsAccepted = documentProvider.existsByEmployeeIdAndType(
                user.employeeId(),
                DocumentType.BIOMETRIC_CONSENT_TERM
        );

        var token = jwtUtils.generateToken(user.employeeId(), normalizedUsername, user.role().name(), user.userId(), termsAccepted);
        timerSample.stop(meterRegistry.timer("auth.login.password.duration"));
        return token;
    }

    @Override
    @Transactional(readOnly = true)
    public String loginFace(String faceImageBase64) {
        var timerSample = Timer.start(meterRegistry);
        try {
            // 1. Decodifica a imagem Base64
            byte[] imageBytes = Base64.getDecoder().decode(faceImageBase64);
            var inputStream = new ByteArrayInputStream(imageBytes);

            // 2. Busca a face na AWS Rekognition
            // O provider já retorna o UUID do Employee se houver Match > 90%
            var employeeId = faceRecognitionProvider.searchFaceByImage(inputStream);

            if (employeeId == null) {
                throw new ForbiddenException(FACE_NOT_RECOGNIZE);
            }

            // 3. Busca o Usuário vinculado ao EmployeeId encontrado
            var user = userProvider.findByEmployeeId(employeeId)
                    .orElseThrow(() -> new ResourceNotFoundException(NO_USER_LINKED_TO_THIS_EMPLOYEE));

            if (!user.active()) {
                throw new BadRequestException(INACTIVE_USER);
            }

            var termsAccepted = documentProvider.existsByEmployeeIdAndType(
                    user.employeeId(),
                    DocumentType.BIOMETRIC_CONSENT_TERM
            );

            // 4. Gera o Token JWT (mesma lógica do login tradicional)
            var token = jwtUtils.generateToken(
                    user.employeeId(),
                    user.username(),
                    user.role().name(),
                    user.userId(),
                    termsAccepted
            );
            timerSample.stop(meterRegistry.timer("auth.login.face.duration", "status", "success"));
            return token;

        } catch (IllegalArgumentException e) {
            log.warn("errorCode=FACIAL_AUTH_INVALID_PAYLOAD operation=loginFace causeType={}", e.getClass().getSimpleName());
            timerSample.stop(meterRegistry.timer("auth.login.face.duration", "status", "invalid_payload"));
            throw new BadRequestException(INVALID_IMAGE);
        } catch (ForbiddenException | ResourceNotFoundException | BadRequestException e) {
            log.warn("errorCode=FACIAL_AUTH_BUSINESS_ERROR operation=loginFace causeType={}", e.getClass().getSimpleName());
            timerSample.stop(meterRegistry.timer("auth.login.face.duration", "status", "business_error"));
            throw e;
        } catch (ServiceUnavailableException e) {
            log.error("errorCode=FACIAL_AUTH_UNAVAILABLE operation=loginFace", e);
            timerSample.stop(meterRegistry.timer("auth.login.face.duration", "status", "unavailable"));
            throw e;
        } catch (Exception e) {
            log.error("errorCode=FACIAL_AUTH_UNEXPECTED operation=loginFace", e);
            timerSample.stop(meterRegistry.timer("auth.login.face.duration", "status", "unexpected_error"));
            throw new InternalServerException(ERROR_FACIAL_AUTHENTICATION);
        }
    }

    @Override
    @Transactional
    public void recoverPassword(RecoverPasswordRequest request, String originUrl) {
        var timerSample = Timer.start(meterRegistry);
        var recoveryKey = (request.cpf() + "|" + request.email()).trim().toLowerCase(Locale.ROOT);
        enforceNotBlocked(recoveryAttempts, recoveryKey, "Muitas tentativas de recuperação de senha. Tente novamente mais tarde.");
        var credentials = userProvider.findRecoverPasswordCredentialsByCpfAndEmail(request.cpf(), request.email())
                .orElse(null);

        if (credentials == null) {
            log.warn("Tentativa de recuperação de senha falhou: CPF ou Email inválido.");
            registerFailedAttempt(recoveryAttempts, recoveryKey, MAX_RECOVERY_ATTEMPTS, RECOVERY_BLOCK_DURATION);
            timerSample.stop(meterRegistry.timer("auth.password.recovery.duration", "status", "ignored"));
            return;
        }

        if (originUrl != null && !originUrl.isBlank()) {
            log.warn("Header Origin ignorado no recoverPassword por segurança: {}", originUrl);
        }
        var frontendUrl = defaultFrontendBaseUrl;

        var resetToken = tokenProvider.generateAndSaveToken(credentials.userId());

        emailSenderProvider.sendResetEmail(credentials.employeeEmail(), resetToken, credentials.username(), frontendUrl);
        resetAttempts(recoveryAttempts, recoveryKey);
        log.info("Processo de recuperação de senha iniciado para o usuário: {}", credentials.username());
        timerSample.stop(meterRegistry.timer("auth.password.recovery.duration", "status", "queued"));
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        var userId = tokenProvider.validateToken(request.token())
                .orElseThrow(() -> new ResourceNotFoundException(INVALID_PASSWORD_RESET_TOKEN));

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BadRequestException(INVALID_CONFIRM_PASSWORD);
        }
        passwordPolicyValidator.validate(request.newPassword());

        var user = userProvider.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));

        var hashed = passwordEncoder.encode(request.newPassword());

        // Cria um novo objeto User com a senha atualizada
        var updatedUser = new User(
                user.userId(),
                user.username(),
                hashed,
                user.role(),
                user.active(),
                user.employeeId()
        );
        userProvider.save(updatedUser);

        // 4. Limpa o token do Redis
        tokenProvider.deleteToken(request.token());
        log.info("Senha redefinida com sucesso para o usuário: {}", user.username());
    }

    private void enforceNotBlocked(Map<String, AttemptWindow> store, String key, String message) {
        var now = Instant.now();
        var window = store.get(key);

        if (window != null && window.blockedUntil != null && now.isBefore(window.blockedUntil)) {
            throw new TooManyRequestsException(message);
        }

        if (window != null && window.blockedUntil != null && !now.isBefore(window.blockedUntil)) {
            store.remove(key);
        }
    }

    private void registerFailedAttempt(Map<String, AttemptWindow> store, String key, int maxAttempts, Duration blockDuration) {
        var now = Instant.now();
        store.compute(key, (k, current) -> {
            var attempts = current == null ? 1 : current.attempts + 1;
            Instant blockedUntil = null;

            if (attempts >= maxAttempts) {
                blockedUntil = now.plus(blockDuration);
            }

            return new AttemptWindow(attempts, blockedUntil);
        });
    }

    private void resetAttempts(Map<String, AttemptWindow> store, String key) {
        store.remove(key);
    }

    private record AttemptWindow(int attempts, Instant blockedUntil) {}

}
