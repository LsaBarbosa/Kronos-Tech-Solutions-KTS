package com.kts.kronos.application.security;

import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenRevocationServiceTest {

    private static final String SECRET = Base64.getEncoder()
            .encodeToString("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8));

    @Mock
    private UserProvider userProvider;

    private UUID userId;
    private UUID employeeId;
    private JwtUtils jwtUtils;
    private TokenRevocationService service;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        jwtUtils = new JwtUtils(SECRET, 60_000L, "kronos-test-api", "kronos-test-clients", "current", "", 30L, 0L);
        service = new TokenRevocationService(userProvider, jwtUtils);
    }

    @Test
    @DisplayName("deve considerar token atual quando versao do JWT corresponde ao usuario ativo")
    void shouldAcceptCurrentTokenVersion() {
        String token = jwtUtils.generateToken(employeeId, "alice", "MANAGER", userId, true, 4);
        when(userProvider.findById(userId)).thenReturn(Optional.of(user(true, 4)));

        assertTrue(service.isTokenCurrent(token));
    }

    @Test
    @DisplayName("deve rejeitar token antigo quando versao do usuario foi incrementada")
    void shouldRejectStaleTokenVersion() {
        String token = jwtUtils.generateToken(employeeId, "alice", "MANAGER", userId, true, 4);
        when(userProvider.findById(userId)).thenReturn(Optional.of(user(true, 5)));

        assertFalse(service.isTokenCurrent(token));
    }

    @Test
    @DisplayName("deve incrementar tokenVersion ao revogar tokens do usuario")
    void shouldIncrementTokenVersionWhenRevokingUserTokens() {
        User user = user(true, 2);
        when(userProvider.findById(userId)).thenReturn(Optional.of(user));

        service.revokeTokensForUser(userId);

        verify(userProvider).save(user.withIncrementedTokenVersion());
    }

    private User user(boolean active, int tokenVersion) {
        return new User(userId, "alice", "encoded", Role.MANAGER, active, employeeId, tokenVersion);
    }
}
