package com.kts.kronos.app.services;

import com.kts.kronos.domain.exceptions.TooManyAttemptsException;
import com.kts.kronos.domain.exceptions.UserDisabledException;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.dto.AuthResponse;
import com.kts.kronos.domain.model.dto.LoginRequest;
import com.kts.kronos.domain.model.dto.RefreshToken;
import com.kts.kronos.domain.model.roles.CtoRole;
import com.kts.kronos.domain.model.roles.ManagerRole;
import com.kts.kronos.domain.port.in.AuthenticateUseCase;
import com.kts.kronos.domain.port.in.RefreshUseCase;
import com.kts.kronos.domain.port.out.LoginAttemptPort;
import com.kts.kronos.domain.port.out.PasswordEncoderPort;
import com.kts.kronos.domain.port.out.TokenPort;
import com.kts.kronos.domain.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthenticationService implements AuthenticateUseCase, RefreshUseCase {
    private final UserRepositoryPort userRepo;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenPort tokenPort;
    private final LoginAttemptPort loginAttempt;
    @Override
    public AuthResponse authenticate(LoginRequest request) {
        String key = request.username();

        if (loginAttempt.isBlocked(key)) {
            throw new TooManyAttemptsException();
        }

        User user = userRepo.findByUsername(request.username());
        if (!user.enabled()) {
            throw new UserDisabledException();
        }
        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            loginAttempt.recordFailure(key);
            throw new BadCredentialsException("Credenciais inválidas");
        }

        loginAttempt.recordSuccess(key);

        // mapeia suas Roles em GrantedAuthority
        var authorities = getSimpleGrantedAuthorities(user);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                user.username(), null, authorities
        );

        String token   = tokenPort.generateToken(auth);
        String refresh = tokenPort.generateRefreshToken(auth);
        return new AuthResponse(token, refresh);
    }


    @Override
    public AuthResponse refresh(RefreshToken request) {
        Authentication auth = tokenPort.parseToken(request.refreshToken());
        String token = tokenPort.generateToken(auth);
        String refresh = tokenPort.generateRefreshToken(auth);
        return new AuthResponse(token, refresh);
    }
    private static List<SimpleGrantedAuthority> getSimpleGrantedAuthorities(User user) {
        var authorities = user.roles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toString()))
                .collect(Collectors.toList());
        if (user.roles().stream()
                .anyMatch(role -> role instanceof ManagerRole)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_PARTNER"));
        }
        return authorities;
    }
}
