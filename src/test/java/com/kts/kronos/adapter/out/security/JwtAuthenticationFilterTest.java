package com.kts.kronos.adapter.out.security;

import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.application.port.out.provider.TokenBlacklistProvider;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private UserProvider userProvider;

    @Mock
    private TokenBlacklistProvider tokenBlacklistProvider;

    @Mock
    private AuthCookieService authCookieService;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(
                jwtUtils,
                userDetailsService,
                userProvider,
                tokenBlacklistProvider,
                authCookieService
        );
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldPopulateSecurityContextWithCurrentAuthorities() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        when(authCookieService.extractToken(request)).thenReturn(Optional.of("valid-token"));
        when(jwtUtils.validateToken("valid-token")).thenReturn(true);
        when(tokenBlacklistProvider.isBlacklisted("valid-token")).thenReturn(false);
        when(jwtUtils.getUsernameFromToken("valid-token")).thenReturn("manager.user");
        UUID userId = UUID.randomUUID();
        when(jwtUtils.getUserIdFromToken("valid-token")).thenReturn(userId);
        when(jwtUtils.getSessionVersionFromToken("valid-token")).thenReturn(1L);
        when(userProvider.findById(userId)).thenReturn(Optional.of(
                new com.kts.kronos.domain.model.User(
                        userId,
                        "manager.user",
                        "encoded",
                        Role.MANAGER,
                        true,
                        UUID.randomUUID(),
                        1L,
                        null,
                        null,
                        null
                )
        ));

        UserDetails userDetails = User.withUsername("manager.user")
                .password("encoded")
                .authorities("ROLE_MANAGER")
                .build();
        when(userDetailsService.loadUserByUsername("manager.user")).thenReturn(userDetails);

        filter.doFilter(request, response, chain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_MANAGER".equals(a.getAuthority())));
    }

    @Test
    void shouldBlockDisabledUserEvenWithValidToken() {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        when(authCookieService.extractToken(request)).thenReturn(Optional.of("legacy-token"));
        when(jwtUtils.validateToken("legacy-token")).thenReturn(true);
        when(tokenBlacklistProvider.isBlacklisted("legacy-token")).thenReturn(false);
        when(jwtUtils.getUsernameFromToken("legacy-token")).thenReturn("disabled.user");
        UUID userId = UUID.randomUUID();
        when(jwtUtils.getUserIdFromToken("legacy-token")).thenReturn(userId);
        when(jwtUtils.getSessionVersionFromToken("legacy-token")).thenReturn(0L);
        when(userProvider.findById(userId)).thenReturn(Optional.of(
                new com.kts.kronos.domain.model.User(
                        userId,
                        "disabled.user",
                        "encoded",
                        Role.MANAGER,
                        true,
                        UUID.randomUUID(),
                        0L,
                        null,
                        null,
                        null
                )
        ));
        when(userDetailsService.loadUserByUsername("disabled.user"))
                .thenThrow(new DisabledException("Conta desativada"));

        assertThrows(DisabledException.class, () -> filter.doFilter(request, response, chain));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldContinueWhenAuthorizationHeaderIsMissing() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        when(authCookieService.extractToken(request)).thenReturn(Optional.empty());
        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtUtils, userDetailsService);
    }

    @Test
    void shouldIgnoreAuthorizationHeaderWhenCookieIsMissing() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic abc123");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        when(authCookieService.extractToken(request)).thenReturn(Optional.empty());
        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtUtils, userDetailsService);
    }

    @Test
    void shouldContinueWhenTokenIsInvalid() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        when(authCookieService.extractToken(request)).thenReturn(Optional.of("invalid-token"));
        when(jwtUtils.validateToken("invalid-token")).thenReturn(false);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtUtils).validateToken("invalid-token");
        verify(jwtUtils, never()).getUsernameFromToken(anyString());
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void shouldNotOverrideExistingAuthentication() throws Exception {
        var existingAuthentication = new UsernamePasswordAuthenticationToken(
                "existing-user",
                null,
                AuthorityUtils.createAuthorityList("ROLE_PARTNER")
        );
        SecurityContextHolder.getContext().setAuthentication(existingAuthentication);

        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        when(authCookieService.extractToken(request)).thenReturn(Optional.of("valid-token"));
        when(jwtUtils.validateToken("valid-token")).thenReturn(true);
        when(tokenBlacklistProvider.isBlacklisted("valid-token")).thenReturn(false);
        when(jwtUtils.getUsernameFromToken("valid-token")).thenReturn("manager.user");
        UUID userId = UUID.randomUUID();
        when(jwtUtils.getUserIdFromToken("valid-token")).thenReturn(userId);
        when(jwtUtils.getSessionVersionFromToken("valid-token")).thenReturn(1L);
        when(userProvider.findById(userId)).thenReturn(Optional.of(
                new com.kts.kronos.domain.model.User(
                        userId,
                        "manager.user",
                        "encoded",
                        Role.MANAGER,
                        true,
                        UUID.randomUUID(),
                        1L,
                        null,
                        null,
                        null
                )
        ));

        filter.doFilter(request, response, chain);

        assertEquals("existing-user", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    void shouldContinueWithoutAuthenticationWhenTokenIsBlacklisted() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        when(authCookieService.extractToken(request)).thenReturn(Optional.of("revoked-token"));
        when(jwtUtils.validateToken("revoked-token")).thenReturn(true);
        when(tokenBlacklistProvider.isBlacklisted("revoked-token")).thenReturn(true);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtUtils).validateToken("revoked-token");
        verify(tokenBlacklistProvider).isBlacklisted("revoked-token");
        verify(jwtUtils, never()).getUsernameFromToken(anyString());
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void filter_shouldRejectTokenWhenSessionVersionIsOutdated() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();
        UUID userId = UUID.randomUUID();

        when(authCookieService.extractToken(request)).thenReturn(Optional.of("outdated-token"));
        when(jwtUtils.validateToken("outdated-token")).thenReturn(true);
        when(tokenBlacklistProvider.isBlacklisted("outdated-token")).thenReturn(false);
        when(jwtUtils.getUsernameFromToken("outdated-token")).thenReturn("manager.user");
        when(jwtUtils.getUserIdFromToken("outdated-token")).thenReturn(userId);
        when(jwtUtils.getSessionVersionFromToken("outdated-token")).thenReturn(0L);
        when(userProvider.findById(userId)).thenReturn(Optional.of(
                new com.kts.kronos.domain.model.User(
                        userId,
                        "manager.user",
                        "encoded",
                        Role.MANAGER,
                        true,
                        UUID.randomUUID(),
                        1L,
                        null,
                        null,
                        null
                )
        ));

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    void filter_shouldAuthenticateWhenSessionVersionMatches() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();
        UUID userId = UUID.randomUUID();

        when(authCookieService.extractToken(request)).thenReturn(Optional.of("current-token"));
        when(jwtUtils.validateToken("current-token")).thenReturn(true);
        when(tokenBlacklistProvider.isBlacklisted("current-token")).thenReturn(false);
        when(jwtUtils.getUsernameFromToken("current-token")).thenReturn("manager.user");
        when(jwtUtils.getUserIdFromToken("current-token")).thenReturn(userId);
        when(jwtUtils.getSessionVersionFromToken("current-token")).thenReturn(1L);
        when(userProvider.findById(userId)).thenReturn(Optional.of(
                new com.kts.kronos.domain.model.User(
                        userId,
                        "manager.user",
                        "encoded",
                        Role.MANAGER,
                        true,
                        UUID.randomUUID(),
                        1L,
                        null,
                        null,
                        null
                )
        ));
        UserDetails userDetails = User.withUsername("manager.user")
                .password("encoded")
                .authorities("ROLE_MANAGER")
                .build();
        when(userDetailsService.loadUserByUsername("manager.user")).thenReturn(userDetails);

        filter.doFilter(request, response, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
