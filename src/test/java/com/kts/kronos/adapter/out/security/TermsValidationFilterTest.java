package com.kts.kronos.adapter.out.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TermsValidationFilterTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private FilterChain filterChain;

    @Mock
    private AuthCookieService authCookieService;

    @Test
    void shouldSkipValidationForPublicAndOptionsRequests() throws Exception {
        var filter = new TermsValidationFilter(jwtUtils, authCookieService);

        var publicRequest = new MockHttpServletRequest("GET", "/auth/login");
        var publicResponse = new MockHttpServletResponse();
        filter.doFilter(publicRequest, publicResponse, filterChain);

        var swaggerRequest = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        var swaggerResponse = new MockHttpServletResponse();
        filter.doFilter(swaggerRequest, swaggerResponse, filterChain);

        var optionsRequest = new MockHttpServletRequest("OPTIONS", "/private");
        var optionsResponse = new MockHttpServletResponse();
        filter.doFilter(optionsRequest, optionsResponse, filterChain);

        var logoutRequest = new MockHttpServletRequest("POST", "/auth/logout");
        logoutRequest.setServletPath("/auth/logout");
        logoutRequest.addHeader("Authorization", "Bearer token");
        var logoutResponse = new MockHttpServletResponse();
        filter.doFilter(logoutRequest, logoutResponse, filterChain);

        verify(filterChain, times(4)).doFilter(any(), any());
        verifyNoInteractions(jwtUtils);
    }

    @Test
    void shouldContinueWhenNoBearerTokenOrClaimsAreInvalid() throws Exception {
        var filter = new TermsValidationFilter(jwtUtils, authCookieService);

        var requestWithoutAuth = new MockHttpServletRequest("GET", "/private");
        var responseWithoutAuth = new MockHttpServletResponse();
        filter.doFilter(requestWithoutAuth, responseWithoutAuth, filterChain);

        var requestWithInvalidToken = new MockHttpServletRequest("GET", "/private");
        requestWithInvalidToken.addHeader("Authorization", "Bearer invalid");
        var responseWithInvalidToken = new MockHttpServletResponse();
        when(jwtUtils.getValidClaims("invalid")).thenReturn(Optional.empty());

        filter.doFilter(requestWithInvalidToken, responseWithInvalidToken, filterChain);

        verify(filterChain, times(2)).doFilter(any(), any());
    }

    @Test
    void shouldBlockWhenTermsAreNotAccepted() throws Exception {
        var filter = new TermsValidationFilter(jwtUtils, authCookieService);
        var request = new MockHttpServletRequest("GET", "/private");
        request.addHeader("Authorization", "Bearer token");
        var response = new MockHttpServletResponse();

        Claims claims = mock(Claims.class);
        when(claims.get("terms_accepted")).thenReturn(false);
        when(jwtUtils.getValidClaims("token")).thenReturn(Optional.of(claims));

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString())
                .contains("TERMS_NOT_ACCEPTED")
                .contains("https://termo.kronossolutions.tech/")
                .contains("Aceite os termos para continuar.");
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void shouldContinueWhenTermsAreAccepted() throws Exception {
        var filter = new TermsValidationFilter(jwtUtils, authCookieService);
        var request = new MockHttpServletRequest("GET", "/private");
        request.addHeader("Authorization", "Bearer token");
        var response = new MockHttpServletResponse();

        Claims claims = mock(Claims.class);
        when(claims.get("terms_accepted")).thenReturn(true);
        when(jwtUtils.getValidClaims("token")).thenReturn(Optional.of(claims));

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
