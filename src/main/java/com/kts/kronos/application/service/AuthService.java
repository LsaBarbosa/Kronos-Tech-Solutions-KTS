package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.security.LoginResponse;
import com.kts.kronos.application.port.in.usecase.AuthUseCase;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.port.in.usecase.UserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {
    private final AuthenticationManager authManager;
    private final UserUseCase userCase;
    private final JwtUtils jwtUtils;
    @Override
    public String login(String username, String password) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        var user = userCase.getUserByUsername(username);
        return jwtUtils.generateToken(user.userId(), user.username(), user.role().name());

    }
}
