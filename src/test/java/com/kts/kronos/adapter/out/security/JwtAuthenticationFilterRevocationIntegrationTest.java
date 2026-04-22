package com.kts.kronos.adapter.out.security;

import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.application.security.TokenRevocationService;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterRevocationIntegrationTest {

    private static final String ISSUER = "kronos-test-api";
    private static final String AUDIENCE = "kronos-test-clients";
    private static final String CURRENT_KEY_ID = "test-current";
    private static final String CURRENT_SECRET = Base64.getEncoder()
            .encodeToString("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8));

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private UserProvider userProvider;

    private UUID userId;
    private UUID employeeId;
    private JwtUtils jwtUtils;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        jwtUtils = new JwtUtils(CURRENT_SECRET, 60_000L, ISSUER, AUDIENCE, CURRENT_KEY_ID, "", 30L, 0L);
        var revocationService = new TokenRevocationService(userProvider, jwtUtils);
        filter = new JwtAuthenticationFilter(jwtUtils, userDetailsService, revocationService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("deve autenticar token quando token_version corresponde ao usuario")
    void shouldAuthenticateWhenTokenVersionMatchesUserVersion() throws Exception {
        String token = token(2);
        when(userProvider.findById(userId))
                .thenReturn(Optional.of(user(true, 2)));
        when(userDetailsService.loadUserByUsername("alice"))
                .thenReturn(org.springframework.security.core.userdetails.User
                        .withUsername("alice")
                        .password("encoded")
                        .authorities("ROLE_MANAGER")
                        .build());

        filter.doFilter(request(token), new MockHttpServletResponse(), new MockFilterChain());

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("deve rejeitar token antigo quando token_version do usuario foi incrementado")
    void shouldRejectStaleTokenWhenUserTokenVersionWasIncremented() throws Exception {
        String token = token(1);
        when(userProvider.findById(userId))
                .thenReturn(Optional.of(user(true, 2)));

        filter.doFilter(request(token), new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("deve rejeitar token de usuario desativado antes de carregar UserDetails")
    void shouldRejectTokenForDisabledUserBeforeLoadingUserDetails() throws Exception {
        String token = token(1);
        when(userProvider.findById(userId))
                .thenReturn(Optional.of(user(false, 1)));

        filter.doFilter(request(token), new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(userDetailsService);
    }

    private String token(int tokenVersion) {
        return jwtUtils.generateToken(employeeId, "alice", "MANAGER", userId, true, tokenVersion);
    }

    private MockHttpServletRequest request(String token) {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    private User user(boolean active, int tokenVersion) {
        return new User(userId, "alice", "encoded", Role.MANAGER, active, employeeId, tokenVersion);
    }
}
