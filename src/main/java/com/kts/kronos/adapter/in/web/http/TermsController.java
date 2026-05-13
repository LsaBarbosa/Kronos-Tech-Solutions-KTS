package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.out.security.AuthCookieService;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.security.ClientIpResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import java.util.UUID;

import static com.kts.kronos.constants.Messages.ANY_EMPLOYEE;

@RestController
@RequestMapping("terms")
@RequiredArgsConstructor
@Tag(name = "Legal - Termos de Uso", description = "Gestão de aceites e termos legais")
public class TermsController {

    private final AcceptTermsUseCase acceptanceUseCase;
    private final JwtAuthenticatedUser jwtAuthenticatedUser;
    private final JwtUtils jwtUtils;
    private final AuthCookieService authCookieService;
    private final ClientIpResolver clientIpResolver;

    @PostMapping("/accept-biometric")
    @PreAuthorize(ANY_EMPLOYEE)
    @Operation(summary = "Registrar Aceite do Termo de Biometria",
            description = "Gera um PDF assinado com IP e data, e salva nos documentos do usuário.")
    public ResponseEntity<Void> acceptBiometricTerms(HttpServletRequest request) {
        UUID employeeId = jwtAuthenticatedUser.getEmployeeId();
        UUID userId = jwtAuthenticatedUser.getuserId();
        String username = jwtAuthenticatedUser.getUsername();
        String role = jwtAuthenticatedUser.getCurrentRole().name();

        String ipAddress = clientIpResolver.resolve(request);

        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isBlank()) {
            userAgent = "Desconhecido";
        }

        acceptanceUseCase.acceptBiometricTerms(employeeId, ipAddress, userAgent);

        String newToken = jwtUtils.generateToken(employeeId, username, role, userId, true);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookieService.createAccessTokenCookie(newToken).toString())
                .build();
    }

    @DeleteMapping("/revoke-biometric")
    @PreAuthorize(ANY_EMPLOYEE)
    @Operation(summary = "Revogar Consentimento Biométrico",
            description = "Revoga o consentimento, remove imagem/template biométrico e invalida a flag de aceite no JWT.")
    public ResponseEntity<Void> revokeBiometricTerms(HttpServletRequest request) {
        UUID employeeId = jwtAuthenticatedUser.getEmployeeId();
        UUID userId = jwtAuthenticatedUser.getuserId();
        String username = jwtAuthenticatedUser.getUsername();
        String role = jwtAuthenticatedUser.getCurrentRole().name();

        String ipAddress = clientIpResolver.resolve(request);

        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isBlank()) {
            userAgent = "Desconhecido";
        }

        acceptanceUseCase.revokeBiometricTerms(employeeId, ipAddress, userAgent);

        String newToken = jwtUtils.generateToken(employeeId, username, role, userId, false);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookieService.createAccessTokenCookie(newToken).toString())
                .build();
    }

    @GetMapping("/status")
    @PreAuthorize(ANY_EMPLOYEE)
    @Operation(summary = "Verificar Status do Aceite", description = "Retorna true se o usuário já aceitou os termos.")
    public ResponseEntity<Map<String, Boolean>> checkTermsStatus() {
        UUID employeeId = jwtAuthenticatedUser.getEmployeeId();
        boolean hasAccepted = acceptanceUseCase.hasAcceptedBiometricTerm(employeeId);

        return ResponseEntity.ok(Map.of("accepted", hasAccepted));
    }
}
