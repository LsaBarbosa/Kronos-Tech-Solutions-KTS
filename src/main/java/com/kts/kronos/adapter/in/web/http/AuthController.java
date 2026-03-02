package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.employee.RecoverPasswordRequest;
import com.kts.kronos.adapter.in.web.dto.security.FaceLoginRequest;
import com.kts.kronos.adapter.in.web.dto.security.LoginRequest;
import com.kts.kronos.adapter.in.web.dto.security.LoginResponse;
import com.kts.kronos.adapter.in.web.dto.security.ResetPasswordRequest;
import com.kts.kronos.application.port.in.usecase.AuthUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.kts.kronos.constants.ApiPaths.*;
import static com.kts.kronos.constants.Swagger.*;

@RestController
@RequestMapping(AUTH)
@RequiredArgsConstructor
@Tag(name = SWAGGER_AUTH_TAG, description = SWAGGER_AUTH_DESC)
public class AuthController {
    private final AuthUseCase authUseCase;

    @Operation(summary = LOGIN_SUMMARY, description = LOGIN_DESC, security = @SecurityRequirement(name = ""))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = LOGIN_SUCCESS),
            @ApiResponse(responseCode = "401", description = CREDENTIALS_INVALID)
    })
    @PostMapping(LOGIN)
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        var token = authUseCase.login(req.username(), req.password());
        return ResponseEntity.ok(new LoginResponse(token));
    }

    @Operation(summary = RECOVER_PASS_SUMMARY, description = RECOVER_PASS_DESC, security = @SecurityRequirement(name = ""))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = RECOVER_PASS_204),
            @ApiResponse(responseCode = "400", description = RECOVER_PASS_400),
            @ApiResponse(responseCode = "404", description = RECOVER_PASS_404)
    })
    @PostMapping(RECOVER_PASSWORD)
    public ResponseEntity<Void> recoverPassword(@Valid @RequestBody RecoverPasswordRequest req,
                                                @RequestHeader(name = "Origin", required = false) String originUrl) {
        authUseCase.recoverPassword(req, originUrl);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = LOGIN_FACE_SUMMARY, description = LOGIN_FACE_DESC, security = @SecurityRequirement(name = ""))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = LOGIN_SUCCESS),
            @ApiResponse(responseCode = "400", description = LOGIN_FACE_400),
            @ApiResponse(responseCode = "403", description = LOGIN_FACE_403),
            @ApiResponse(responseCode = "404", description = LOGIN_FACE_404)
    })
    @PostMapping(LOGIN_FACE)
    public ResponseEntity<LoginResponse> loginFace(@Valid @RequestBody FaceLoginRequest req) {
        var token = authUseCase.loginFace(req.faceImageBase64());
        return ResponseEntity.ok(new LoginResponse(token));
    }

    @Operation(summary = RESET_PASS_SUMMARY, description = RESET_PASS_DESC, security = @SecurityRequirement(name = ""))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = RESET_PASS_204),
            @ApiResponse(responseCode = "400", description = RESET_PASS_400),
            @ApiResponse(responseCode = "404", description = RESET_PASS_404),
    })
    @PostMapping(RESET_PASSWORD)
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authUseCase.resetPassword(req);
        return ResponseEntity.noContent().build();
    }
}
