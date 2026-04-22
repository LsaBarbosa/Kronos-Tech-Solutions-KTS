package com.kts.kronos.application.security;

import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TokenRevocationService {

    private final UserProvider userProvider;
    private final JwtUtils jwtUtils;

    public boolean isTokenCurrent(String token) {
        try {
            UUID userId = jwtUtils.getUserIdFromToken(token);
            int tokenVersion = jwtUtils.getTokenVersionFromToken(token);

            return userProvider.findById(userId)
                    .filter(User::active)
                    .map(user -> user.tokenVersion() == tokenVersion)
                    .orElse(false);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public int revokeTokensForUser(UUID userId) {
        User user = userProvider.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado para revogacao de token."));
        User updated = user.withIncrementedTokenVersion();
        userProvider.save(updated);
        return updated.tokenVersion();
    }

    public Optional<Integer> revokeTokensByEmployeeId(UUID employeeId) {
        return userProvider.findByEmployeeId(employeeId)
                .map(user -> {
                    User updated = user.withIncrementedTokenVersion();
                    userProvider.save(updated);
                    return updated.tokenVersion();
                });
    }

    public int getCurrentTokenVersion(UUID userId) {
        return userProvider.findById(userId)
                .map(User::tokenVersion)
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado para versao de token."));
    }
}
