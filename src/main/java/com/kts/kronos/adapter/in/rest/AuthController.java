package com.kts.kronos.adapter.in.rest;

import com.kts.kronos.domain.model.dto.AuthResponse;
import com.kts.kronos.domain.model.dto.LoginRequest;
import com.kts.kronos.domain.model.dto.RefreshToken;
import com.kts.kronos.domain.port.in.AuthenticateUseCase;
import com.kts.kronos.domain.port.in.RefreshUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticateUseCase authUseCase;
    private final RefreshUseCase refreshUseCase;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authUseCase.authenticate(request));
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshToken request) {
        return ResponseEntity.ok(refreshUseCase.refresh(request));
    }
}
