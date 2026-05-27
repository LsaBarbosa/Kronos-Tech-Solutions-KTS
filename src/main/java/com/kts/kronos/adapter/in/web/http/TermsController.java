package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.legal.AcceptBiometricTermsRequest;
import com.kts.kronos.adapter.in.web.dto.legal.BiometricConsentStatusResponse;
import com.kts.kronos.adapter.in.web.dto.legal.CurrentLegalTextResponse;
import com.kts.kronos.adapter.out.security.AuthCookieService;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.security.ClientIpResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
    public ResponseEntity<BiometricConsentStatusResponse> acceptBiometricTerms(
            @Valid @RequestBody AcceptBiometricTermsRequest payload,
            HttpServletRequest request
    ) {
        UUID employeeId = jwtAuthenticatedUser.getEmployeeId();
        UUID userId = jwtAuthenticatedUser.getuserId();
        String username = jwtAuthenticatedUser.getUsername();
        String role = jwtAuthenticatedUser.getCurrentRole().name();

        String ipAddress = clientIpResolver.resolve(request);

        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isBlank()) {
            userAgent = "Desconhecido";
        }

        acceptanceUseCase.acceptBiometricTerms(
                employeeId,
                userId,
                ipAddress,
                userAgent,
                payload.version(),
                payload.contentHashSha256()
        );

        var consentStatus = acceptanceUseCase.getBiometricConsentStatus(employeeId);
        String newToken = jwtUtils.generateToken(employeeId, username, role, userId, consentStatus, 0L);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookieService.createAccessTokenCookie(newToken).toString())
                .body(BiometricConsentStatusResponse.fromDomain(consentStatus));
    }

    @DeleteMapping("/revoke-biometric")
    @PreAuthorize(ANY_EMPLOYEE)
    @Operation(summary = "Revogar Consentimento Biométrico",
            description = "Revoga o consentimento, remove imagem/template biométrico e invalida a flag de aceite no JWT.")
    public ResponseEntity<BiometricConsentStatusResponse> revokeBiometricTerms(HttpServletRequest request) {
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

        var consentStatus = acceptanceUseCase.getBiometricConsentStatus(employeeId);
        String newToken = jwtUtils.generateToken(employeeId, username, role, userId, consentStatus, 0L);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookieService.createAccessTokenCookie(newToken).toString())
                .body(BiometricConsentStatusResponse.fromDomain(consentStatus));
    }

    @GetMapping("/status")
    @PreAuthorize(ANY_EMPLOYEE)
    @Operation(summary = "Verificar Status do Aceite", description = "Retorna o status versionado do consentimento biométrico.")
    public ResponseEntity<BiometricConsentStatusResponse> checkTermsStatus() {
        UUID employeeId = jwtAuthenticatedUser.getEmployeeId();
        var consentStatus = acceptanceUseCase.getBiometricConsentStatus(employeeId);

        return ResponseEntity.ok(BiometricConsentStatusResponse.fromDomain(consentStatus));
    }

    @GetMapping("/biometric/current")
    @PreAuthorize(ANY_EMPLOYEE)
    @Operation(summary = "Consultar termo biométrico atual", description = "Retorna a versão ativa do termo de consentimento biométrico.")
    public ResponseEntity<CurrentLegalTextResponse> getCurrentBiometricTerm() {
        var legalText = acceptanceUseCase.getCurrentBiometricTerm();
        return ResponseEntity.ok(new CurrentLegalTextResponse(
                legalText.documentType(),
                legalText.version(),
                legalText.title(),
                legalText.content(),
                legalText.contentHashSha256(),
                legalText.active()
        ));
    }

    @GetMapping("/consents/history")
    @PreAuthorize(ANY_EMPLOYEE)
    @Operation(summary = "Histórico de Consentimentos", description = "Retorna o histórico completo de consentimentos do usuário autenticado com status atual de cada um.")
    public ResponseEntity<java.util.List<com.kts.kronos.adapter.in.web.dto.legal.ConsentHistoryResponse>> getConsentHistory() {
        UUID employeeId = jwtAuthenticatedUser.getEmployeeId();
        var consents = acceptanceUseCase.getConsentHistory(employeeId);
        var responses = consents.stream()
                .map(com.kts.kronos.adapter.in.web.dto.legal.ConsentHistoryResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(responses);
    }
}
