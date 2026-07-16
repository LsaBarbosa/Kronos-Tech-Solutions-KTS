package com.kts.kronos.adapter.out.security;

import com.kts.kronos.application.port.out.provider.TokenBlacklistProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtAuthenticationFilterCoverageTest {

    @Mock private JwtUtils jwtUtils;
    @Mock private UserDetailsService userDetailsService;
    @Mock private UserProvider userProvider;
    @Mock private TokenBlacklistProvider tokenBlacklistProvider;
    @Mock private AuthCookieService authCookieService;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtUtils, userDetailsService, userProvider, tokenBlacklistProvider, authCookieService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── L63-65: userId == null → chain.doFilter + return ────────────────────────

    @Test
    void doFilter_whenUserIdIsNull_continuesFilterChainWithoutAuth() throws Exception {
        when(authCookieService.extractToken(any())).thenReturn(Optional.of("valid-token"));
        when(jwtUtils.validateToken("valid-token")).thenReturn(true);
        when(tokenBlacklistProvider.isBlacklisted("valid-token")).thenReturn(false);
        when(jwtUtils.getUsernameFromToken("valid-token")).thenReturn("user@kts.com");
        when(jwtUtils.getUserIdFromToken("valid-token")).thenReturn(null);

        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        verify(userProvider, never()).findById(any());
    }

    // ── L69-71: currentUser == null → chain.doFilter + return ───────────────────

    @Test
    void doFilter_whenCurrentUserIsNull_continuesFilterChainWithoutAuth() throws Exception {
        UUID userId = UUID.randomUUID();
        when(authCookieService.extractToken(any())).thenReturn(Optional.of("valid-token"));
        when(jwtUtils.validateToken("valid-token")).thenReturn(true);
        when(tokenBlacklistProvider.isBlacklisted("valid-token")).thenReturn(false);
        when(jwtUtils.getUsernameFromToken("valid-token")).thenReturn("user@kts.com");
        when(jwtUtils.getUserIdFromToken("valid-token")).thenReturn(userId);
        when(jwtUtils.getSessionVersionFromToken("valid-token")).thenReturn(0L);
        when(userProvider.findById(userId)).thenReturn(Optional.empty());

        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        verify(userProvider).findById(userId);
    }

    // ── L86 branch: username == null → authentication block skipped ─────────────

    @Test
    void doFilter_whenUsernameIsNull_skipsAuthenticationPopulation() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "test", "hash", Role.PARTNER, true, UUID.randomUUID(), 1L, null, null, null);

        when(authCookieService.extractToken(any())).thenReturn(Optional.of("valid-token"));
        when(jwtUtils.validateToken("valid-token")).thenReturn(true);
        when(tokenBlacklistProvider.isBlacklisted("valid-token")).thenReturn(false);
        when(jwtUtils.getUsernameFromToken("valid-token")).thenReturn(null);
        when(jwtUtils.getUserIdFromToken("valid-token")).thenReturn(userId);
        when(jwtUtils.getSessionVersionFromToken("valid-token")).thenReturn(1L);
        when(userProvider.findById(userId)).thenReturn(Optional.of(user));

        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        verify(userDetailsService, never()).loadUserByUsername(any());
    }
}
