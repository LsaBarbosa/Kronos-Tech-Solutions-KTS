package com.kts.kronos.application.service;

import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.AuthUseCase;
import com.kts.kronos.application.port.out.provider.UserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import static com.kts.kronos.constants.Messages.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {
    private final AuthenticationManager authManager;
    private final JwtUtils jwtUtils;
    private final UserProvider userProvider;

    @Override
    public String login(String username, String password) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        var user = userProvider.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));
        return jwtUtils.generateToken(user.employeeId(), username,  user.role().name(),user.userId());
    }
}
