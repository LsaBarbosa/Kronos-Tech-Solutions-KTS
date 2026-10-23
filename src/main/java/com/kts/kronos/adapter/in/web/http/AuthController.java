package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.auth.SwitchCompanyRequest;
import com.kts.kronos.adapter.in.web.dto.employee.RecoverPasswordRequest;
import com.kts.kronos.adapter.in.web.dto.security.CsrfTokenResponse;
import com.kts.kronos.adapter.in.web.dto.security.FaceCheckinRequest;
import com.kts.kronos.adapter.in.web.dto.security.FaceCheckinResponse;
import com.kts.kronos.adapter.in.web.dto.security.FaceLoginRequest;
import com.kts.kronos.adapter.in.web.dto.security.LoginRequest;
import com.kts.kronos.adapter.in.web.dto.security.ResetPasswordRequest;
import com.kts.kronos.adapter.out.security.AuthCookieService;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.port.in.usecase.AuthUseCase;
import com.kts.kronos.application.port.in.usecase.PasswordlessCheckinUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.kts.kronos.constants.ApiPaths.*;
import static com.kts.kronos.constants.Messages.*;

@RestController
@RequestMapping(AUTH)
@RequiredArgsConstructor
public class AuthController {
    private final AuthUseCase authUseCase;
    private final PasswordlessCheckinUseCase passwordlessCheckinUseCase;
    private final AuthCookieService authCookieService;
    private final JwtUtils jwtUtils;

    @PostMapping(LOGIN)
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest req) {
        String token = authUseCase.login(req.username(), req.password());
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookieService.createAccessTokenCookie(token).toString())
                .build();
    }
    @PostMapping(RECOVER_PASSWORD)
    public ResponseEntity<Void> recoverPassword(@Valid @RequestBody RecoverPasswordRequest req) {
        authUseCase.recoverPassword(req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(LOGIN_FACE)
    public ResponseEntity<Void> loginFace(@Valid @RequestBody FaceLoginRequest req) {
        String token = authUseCase.loginFace(req.faceImageBase64(), req.livenessPassed());
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookieService.createAccessTokenCookie(token).toString())
                .build();
    }

    @PostMapping(CHECKIN_FACE)
    public ResponseEntity<FaceCheckinResponse> checkinFace(@Valid @RequestBody FaceCheckinRequest req) {
        return ResponseEntity.ok(passwordlessCheckinUseCase.checkinFace(req));
    }

    @PostMapping(RESET_PASSWORD)
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authUseCase.resetPassword(req);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/csrf")
    public ResponseEntity<CsrfTokenResponse> csrf(CsrfToken csrfToken) {
        return ResponseEntity.ok(new CsrfTokenResponse(
                csrfToken.getHeaderName(),
                csrfToken.getParameterName(),
                csrfToken.getToken()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        authCookieService.extractToken(request).ifPresent(authUseCase::logout);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookieService.expireAccessTokenCookie().toString())
                .build();
    }

    @PostMapping(REFRESH)
    public ResponseEntity<Void> refresh(HttpServletRequest request) {
        var tokenOpt = authCookieService.extractToken(request);
        if (tokenOpt.isEmpty()) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
        String newToken = authUseCase.refreshToken(tokenOpt.get());
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookieService.createAccessTokenCookie(newToken).toString())
                .build();
    }

    @PostMapping(SWITCH_COMPANY)
    public ResponseEntity<Void> switchCompany(
            @Valid @RequestBody SwitchCompanyRequest req,
            HttpServletRequest request
    ) {
        String currentToken = authCookieService.extractToken(request)
                .orElseThrow(() -> new IllegalArgumentException(HEADER_AUTHORIZATION_NOT_FOUND));
        UUID userId = jwtUtils.getUserIdFromToken(currentToken);
        String newToken = authUseCase.switchCompany(userId, req.companyId());
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookieService.createAccessTokenCookie(newToken).toString())
                .build();
    }
}
