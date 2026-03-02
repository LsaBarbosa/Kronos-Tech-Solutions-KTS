package com.kts.kronos.adapter.out.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldContinueChainWhenAuthorizationHeaderIsMissing() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var filter = new JwtAuthenticationFilter(jwtUtils, userDetailsService);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtils, userDetailsService);
    }

    @Test
    void shouldReturnUnauthorizedWhenTokenIsInvalid() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");
        var response = new MockHttpServletResponse();
        var filter = new JwtAuthenticationFilter(jwtUtils, userDetailsService);

        when(jwtUtils.getValidClaims("invalid-token")).thenReturn(Optional.empty());

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Token JWT inválido ou expirado.");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void shouldSetAuthenticationWhenTokenIsValidAndUserExists() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-ok");
        var response = new MockHttpServletResponse();
        var filter = new JwtAuthenticationFilter(jwtUtils, userDetailsService);

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("john");

        var user = User.withUsername("john").password("hash").authorities("ROLE_MANAGER").build();

        when(jwtUtils.getValidClaims("token-ok")).thenReturn(Optional.of(claims));
        when(userDetailsService.loadUserByUsername("john")).thenReturn(user);

        filter.doFilter(request, response, filterChain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("john");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldReturnUnauthorizedWhenUserFromTokenDoesNotExist() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-ok");
        var response = new MockHttpServletResponse();
        var filter = new JwtAuthenticationFilter(jwtUtils, userDetailsService);

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("ghost");

        when(jwtUtils.getValidClaims("token-ok")).thenReturn(Optional.of(claims));
        when(userDetailsService.loadUserByUsername("ghost")).thenThrow(new UsernameNotFoundException("not found"));

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Usuário do token não encontrado.");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, never()).doFilter(request, response);
    }
}
