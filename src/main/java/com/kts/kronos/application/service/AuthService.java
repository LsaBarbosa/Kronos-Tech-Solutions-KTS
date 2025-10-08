package com.kts.kronos.application.service;


import com.kts.kronos.adapter.in.web.dto.employee.RecoverPasswordRequest;
import com.kts.kronos.adapter.in.web.dto.security.ResetPasswordRequest;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.AuthUseCase;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.domain.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static com.kts.kronos.constants.Messages.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {
    @Value("${frontend.base-url-plataform}")
    private String defaultFrontendBaseUrl;

    private final AuthenticationManager authManager;
    private final JwtUtils jwtUtils;
    private final UserProvider userProvider;
    private final EmployeeProvider employeeProvider;
    private final PasswordResetTokenProvider tokenProvider;
    private final EmailSenderProvider emailSenderProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    public String login(String username, String password) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(username.toLowerCase(), password));
        var user = userProvider.findByUsername(username.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
        return jwtUtils.generateToken(user.employeeId(), username,  user.role().name(),user.userId());
    }

    @Override
    public void recoverPassword(RecoverPasswordRequest request, String originUrl) {
        // 1. Encontra e valida o Employee pelo CPF e Email (validação de identidade)
        var employee = employeeProvider.findByCpf(request.cpf())
                .filter(emp -> emp.email().equalsIgnoreCase(request.email()))
                .orElse(null);

        // Retorna sucesso (No Content) para evitar ataques de enumeração.
        if (employee == null) {
            log.warn("Tentativa de recuperação de senha falhou: CPF ou Email inválido.");
            return;
        }

        // 2. Encontra o User associado
        var user = userProvider.findByEmployeeId(employee.employeeId()).orElse(null);

        if (user == null) {
            log.warn("Tentativa de recuperação de senha: Colaborador sem usuário. EmployeeId: {}", employee.employeeId());
            return;
        }
        String frontendUrl = (originUrl != null && !originUrl.isBlank()) ? originUrl : defaultFrontendBaseUrl;
        // 3. Gera e salva o token no Redis
        String resetToken = tokenProvider.generateAndSaveToken(user.userId());

        emailSenderProvider.sendResetEmail(
                employee.email(),
                resetToken,
                user.username(),
                frontendUrl
        );
        log.info("Processo de recuperação de senha iniciado para o usuário: {}", user.username());
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

        String hashed = passwordEncoder.encode(request.newPassword());

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

    // Método auxiliar (copiado de UserService) para validar a política de senha
    private void validatePasswordPolicy(String raw) {
        if (raw == null || !raw.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")) {
            throw new BadRequestException(INVALID_PASSWORD_POLICY);
        }
    }
}
