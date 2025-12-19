package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.service.AcceptTermsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("terms")
@RequiredArgsConstructor
@Tag(name = "Legal - Termos de Uso", description = "Gestão de aceites e termos legais")
public class TermsController {

    private final AcceptTermsUseCase acceptanceUseCase;
    private final JwtAuthenticatedUser jwtAuthenticatedUser;

    @PostMapping("/accept-biometric")
    @Operation(summary = "Registrar Aceite do Termo de Biometria",
            description = "Gera um PDF assinado com IP e data, e salva nos documentos do usuário.")
    public ResponseEntity<Void> acceptBiometricTerms(HttpServletRequest request) {

        UUID employeeId = jwtAuthenticatedUser.getEmployeeId();

        // Captura o IP real do cliente (considerando proxies/load balancers)
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr();
        }

        acceptanceUseCase.acceptBiometricTerms(employeeId, ipAddress);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/status")
    @Operation(summary = "Verificar Status do Aceite", description = "Retorna true se o usuário já aceitou os termos.")
    public ResponseEntity<Boolean> checkTermsStatus() {
        UUID employeeId = jwtAuthenticatedUser.getEmployeeId();

        // O Controller chama o Caso de Uso
        boolean hasAccepted = acceptanceUseCase.hasAcceptedBiometricTerm(employeeId);

        return ResponseEntity.ok(hasAccepted);
    }
}