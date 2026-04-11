package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.employee.RecoverPasswordRequest;
import com.kts.kronos.adapter.in.web.dto.security.FaceLoginRequest;
import com.kts.kronos.adapter.in.web.dto.security.LoginRequest;
import com.kts.kronos.adapter.in.web.dto.security.LoginResponse;
import com.kts.kronos.adapter.in.web.dto.security.ResetPasswordRequest;
import com.kts.kronos.application.port.in.usecase.AuthUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.kts.kronos.constants.ApiPaths.*;

@RestController
@RequestMapping(AUTH)
@RequiredArgsConstructor
public class AuthController {
    private final AuthUseCase authUseCase;

    @PostMapping(LOGIN)
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
        String token = authUseCase.login(req.username(), req.password());
        return ResponseEntity.ok(new LoginResponse(token));
    }
    @PostMapping(RECOVER_PASSWORD)
    public ResponseEntity<Void> recoverPassword(@Valid @RequestBody RecoverPasswordRequest req) {
        authUseCase.recoverPassword(req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(LOGIN_FACE)
    public ResponseEntity<LoginResponse> loginFace(@Valid @RequestBody FaceLoginRequest req) {
        String token = authUseCase.loginFace(req.faceImageBase64());
        return ResponseEntity.ok(new LoginResponse(token));
    }

    @PostMapping(RESET_PASSWORD)
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authUseCase.resetPassword(req);
        return ResponseEntity.noContent().build();
    }
}
