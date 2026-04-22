package com.kts.kronos.adapter.out.security;

import com.kts.kronos.application.security.TokenRevocationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private TokenRevocationService tokenRevocationService;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtUtils, userDetailsService, tokenRevocationService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldPopulateSecurityContextWithCurrentAuthorities() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        when(jwtUtils.validateToken("valid-token")).thenReturn(true);
        when(tokenRevocationService.isTokenCurrent("valid-token")).thenReturn(true);
        when(jwtUtils.getUsernameFromToken("valid-token")).thenReturn("manager.user");

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
    void shouldContinueWhenTokenWasRevoked() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer legacy-token");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        when(jwtUtils.validateToken("legacy-token")).thenReturn(true);
        when(tokenRevocationService.isTokenCurrent("legacy-token")).thenReturn(false);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtUtils, never()).getUsernameFromToken(anyString());
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void shouldContinueWhenAuthorizationHeaderIsMissing() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtUtils, tokenRevocationService, userDetailsService);
    }

    @Test
    void shouldContinueWhenAuthorizationHeaderIsNotBearer() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic abc123");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtUtils, tokenRevocationService, userDetailsService);
    }

    @Test
    void shouldContinueWhenTokenIsInvalid() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        when(jwtUtils.validateToken("invalid-token")).thenReturn(false);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtUtils).validateToken("invalid-token");
        verifyNoInteractions(tokenRevocationService);
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
        request.addHeader("Authorization", "Bearer valid-token");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        when(jwtUtils.validateToken("valid-token")).thenReturn(true);
        when(tokenRevocationService.isTokenCurrent("valid-token")).thenReturn(true);
        when(jwtUtils.getUsernameFromToken("valid-token")).thenReturn("manager.user");

        filter.doFilter(request, response, chain);

        assertEquals("existing-user", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }
}
