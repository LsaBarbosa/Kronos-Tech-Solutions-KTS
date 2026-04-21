package com.kts.kronos.application.service;


import com.kts.kronos.adapter.in.web.dto.employee.RecoverPasswordRequest;
import com.kts.kronos.adapter.in.web.dto.security.ResetPasswordRequest;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.AuthUseCase;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.application.security.BiometricProtectionService;
import com.kts.kronos.domain.model.enuns.DocumentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.Base64;

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
    private final DocumentProvider documentProvider;
    private final BiometricProtectionService biometricProtectionService;

    @Override
    public String login(String username, String password) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(username.toLowerCase(), password));
        var user = userProvider.findByUsername(username.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
        var termsAccepted = documentProvider.existsByEmployeeIdAndType(
                user.employeeId(),
                DocumentType.BIOMETRIC_CONSENT_TERM
        );
        return jwtUtils.generateToken(
                user.employeeId(),
                username,
                user.role().name(),
                user.userId(),
                termsAccepted,
                user.tokenVersion()
        );
    }

    @Override
    public String loginFace(String faceImageBase64, Boolean livenessPassed) {
        biometricProtectionService.protectPublicLogin(faceImageBase64, livenessPassed);

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
            return jwtUtils.generateToken(
                    user.employeeId(),
                    user.username(),
                    user.role().name(),
                    user.userId(),
                    termsAccepted,
                    user.tokenVersion()
            );

        } catch (IllegalArgumentException e) {
            log.warn("Imagem inválida recebida no login facial. payloadLength={}",
                    faceImageBase64 == null ? 0 : faceImageBase64.length());
            throw new BadRequestException(INVALID_IMAGE);
        } catch (ForbiddenException | ResourceNotFoundException | BadRequestException e) {
            log.warn("Falha de autenticação facial. exceptionType={}, payloadLength={}, message={}",
                    e.getClass().getSimpleName(),
                    faceImageBase64 == null ? 0 : faceImageBase64.length(),
                    e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            log.error("Falha interna na autenticação facial. payloadLength={}",
                    faceImageBase64 == null ? 0 : faceImageBase64.length(),
                    e);
            throw new BadRequestException(ERROR_FACIAL_AUTHENTICATION);
        }
    }
    @Override
    public void recoverPassword(RecoverPasswordRequest request) {
        String normalizedCpf = request.cpf() == null ? null : request.cpf().trim();
        String normalizedEmail = request.email() == null ? null : request.email().trim();

        String maskedCpf = maskCpf(normalizedCpf);
        String maskedEmail = maskEmail(normalizedEmail);
        log.info("Iniciando recuperação de senha para cpf={} e email={}.", maskedCpf, maskedEmail);

        try {
            // 1. Encontra e valida o Employee pelo CPF e Email (validação de identidade)
            var employee = employeeProvider.findByCpf(normalizedCpf)
                    .filter(emp -> emp.email() != null && emp.email().equalsIgnoreCase(normalizedEmail))
                    .orElse(null);

            // Retorna sucesso (No Content) para evitar ataques de enumeração.
            if (employee == null) {
                log.info("Recuperação de senha processada sem envio de e-mail.");
                return;
            }

            // 2. Encontra o User associado
            var user = userProvider.findByEmployeeId(employee.employeeId()).orElse(null);

            if (user == null) {
                log.info("Recuperação de senha processada sem envio de e-mail.");
                return;
            }

            // 3. Gera e salva o token no Redis
            var resetToken = tokenProvider.generateAndSaveToken(user.userId());

            try {
                emailSenderProvider.sendResetEmail(
                        employee.email(),
                        resetToken,
                        user.username(),
                        defaultFrontendBaseUrl
                );
                log.info("Recuperação de senha processada com disparo assíncrono de e-mail.");
            } catch (RuntimeException e) {
                // Mantém resposta neutra (204) mesmo quando o executor assíncrono recusa a tarefa.
                log.error("Recuperação de senha processada sem envio de e-mail por falha interna. exceptionType={}",
                        e.getClass().getSimpleName(), e);
            }
        } catch (RuntimeException e) {
            // Em falhas de infraestrutura (ex.: bloqueio de query), mantém resposta neutra.
            log.error("Recuperação de senha processada sem envio de e-mail por falha de validação. exceptionType={}",
                    e.getClass().getSimpleName(), e);
        }
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        // 1. Valida e obtém o userId do Redis
        var userId = tokenProvider.validateToken(request.token())
                .orElseThrow(() -> new ResourceNotFoundException(INVALID_PASSWORD_RESET_TOKEN));

        // 2. Valida a nova senha e a confirmação
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BadRequestException(INVALID_CONFIRM_PASSWORD);
        }
        validatePasswordPolicy(request.newPassword());

        // 3. Atualiza a senha
        var user = userProvider.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND)); // Usuário deveria existir

        var hashed = passwordEncoder.encode(request.newPassword());

        var updatedUser = user.withPassword(hashed).withIncrementedTokenVersion();
        userProvider.save(updatedUser);

        // 4. Limpa o token do Redis
        tokenProvider.deleteToken(request.token());
        log.info("Senha redefinida com sucesso para o usuário: {}", user.username());
    }

    // Método auxiliar (copiado de UserService) para validar a política de senha
    private void validatePasswordPolicy(String raw) {
        if (raw == null || !raw.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")) {
            throw new BadRequestException(INVALID_PASSWORD_POLICY);
        }
    }

    private String maskCpf(String cpf) {
        if (cpf == null) {
            return "null";
        }
        String digits = cpf.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return "***";
        }
        String suffix = digits.length() <= 4 ? digits : digits.substring(digits.length() - 4);
        return "***" + suffix;
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1 || atIndex == email.length() - 1) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
