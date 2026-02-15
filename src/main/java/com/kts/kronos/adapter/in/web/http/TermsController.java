package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.kts.kronos.constants.ApiPaths.*;
import static com.kts.kronos.constants.Swagger.*;


@RestController
@RequestMapping(TERMS)
@RequiredArgsConstructor
@Tag(name = SWAGGER_TERMS_TAG, description = SWAGGER_TERMS_DESC)
public class TermsController {

    private final AcceptTermsUseCase acceptanceUseCase;
    private final JwtAuthenticatedUser jwtAuthenticatedUser;

    @PostMapping(ACCEPT_BIOMETRIC)
    @Operation(summary = ACCEPT_BIO_SUMMARY, description = ACCEPT_BIO_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = ACCEPT_BIO_SUCCESS),
            @ApiResponse(responseCode = "404", description = ACCEPT_BIO_404),
            @ApiResponse(responseCode = "500", description = ACCEPT_BIO_500)
    })
    public ResponseEntity<Void> acceptBiometricTerms(HttpServletRequest request) {

        var employeeId = jwtAuthenticatedUser.getEmployeeId();

        var ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr();
        }
        // Em alguns casos o header vem como "ip1, ip2", pegamos o primeiro
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }

        // Identifica o dispositivo (Ex: Mozilla/5.0 (iPhone; CPU iPhone OS 16...))
        var userAgent = request.getHeader("User-Agent");
        if (userAgent == null) userAgent = "Desconhecido";

        // Passamos os dois dados para o serviço
        acceptanceUseCase.acceptBiometricTerms(employeeId, ipAddress, userAgent
        );
        return ResponseEntity.ok().build();
    }


    @GetMapping(STATUS)
    @Operation(summary = CHECK_TERMS_SUMMARY, description = CHECK_TERMS_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = CHECK_TERMS_SUCCESS),
            @ApiResponse(responseCode = "403", description = ACCESS_DENIED_TOKEN)
    })    public ResponseEntity<Boolean> checkTermsStatus() {
        var employeeId = jwtAuthenticatedUser.getEmployeeId();
        boolean hasAccepted = acceptanceUseCase.hasAcceptedBiometricTerm(employeeId);
        return ResponseEntity.ok(hasAccepted);
    }
}