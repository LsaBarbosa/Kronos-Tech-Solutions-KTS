package com.kts.kronos.adapter.out.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.HEADER_AUTHORIZATION_NOT_FOUND;
import static com.kts.kronos.constants.Messages.JWT_EMPLOYEE_ID_NOT_FOUND;
import static com.kts.kronos.constants.Messages.JWT_USER_ID_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticatedUserTest {

    @Test
    void shouldReturnDataExtractedFromToken() {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        AuthCookieService authCookieService = mock(AuthCookieService.class);
        var employeeId = UUID.randomUUID();
        var userId = UUID.randomUUID();

        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtils.getEmployeeIdFromToken("token")).thenReturn(employeeId);
        when(jwtUtils.getUserIdFromToken("token")).thenReturn(userId);
        when(jwtUtils.getUsernameFromToken("token")).thenReturn("john");
        when(jwtUtils.getRoleFromToken("token")).thenReturn("MANAGER");

        var authenticatedUser = new JwtAuthenticatedUser(jwtUtils, request, authCookieService);

        assertThat(authenticatedUser.getEmployeeId()).isEqualTo(employeeId);
        assertThat(authenticatedUser.getuserId()).isEqualTo(userId);
        assertThat(authenticatedUser.getUsername()).isEqualTo("john");
        assertThat(authenticatedUser.getRoleFromToken()).isEqualTo("MANAGER");
    }

    @Test
    void shouldReadTokenFromCookieWhenAuthorizationHeaderIsMissing() {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        AuthCookieService authCookieService = mock(AuthCookieService.class);

        when(request.getHeader("Authorization")).thenReturn(null);
        when(authCookieService.extractTokenFromCookie(request)).thenReturn(Optional.of("cookie-token"));
        when(jwtUtils.getUsernameFromToken("cookie-token")).thenReturn("john");

        var authenticatedUser = new JwtAuthenticatedUser(jwtUtils, request, authCookieService);

        assertThat(authenticatedUser.getUsername()).isEqualTo("john");
    }

    @Test
    void shouldThrowWhenAuthorizationHeaderAndCookieAreMissing() {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        AuthCookieService authCookieService = mock(AuthCookieService.class);
        when(request.getHeader("Authorization")).thenReturn(null);
        when(authCookieService.extractTokenFromCookie(request)).thenReturn(Optional.empty());

        var authenticatedUser = new JwtAuthenticatedUser(jwtUtils, request, authCookieService);

        assertThatThrownBy(authenticatedUser::getUsername)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(HEADER_AUTHORIZATION_NOT_FOUND);
    }

    @Test
    void shouldThrowWhenEmployeeIdOrUserIdAreNotPresentInToken() {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        AuthCookieService authCookieService = mock(AuthCookieService.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtils.getEmployeeIdFromToken("token")).thenReturn(null);
        when(jwtUtils.getUserIdFromToken("token")).thenReturn(null);

        var authenticatedUser = new JwtAuthenticatedUser(jwtUtils, request, authCookieService);

        assertThatThrownBy(authenticatedUser::getEmployeeId)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(JWT_EMPLOYEE_ID_NOT_FOUND);

        assertThatThrownBy(authenticatedUser::getuserId)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(JWT_USER_ID_NOT_FOUND);
    }

    @Test
    void shouldResolveEmployeeIdByRoleRules() {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        AuthCookieService authCookieService = mock(AuthCookieService.class);
        var loggedEmployeeId = UUID.randomUUID();
        var requestedEmployeeId = UUID.randomUUID();

        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtils.getEmployeeIdFromToken("token")).thenReturn(loggedEmployeeId);

        var authenticatedUser = new JwtAuthenticatedUser(jwtUtils, request, authCookieService);

        when(jwtUtils.getRoleFromToken("token")).thenReturn("PARTNER");
        assertThat(authenticatedUser.isWithEmployeeId(requestedEmployeeId)).isEqualTo(loggedEmployeeId);

        when(jwtUtils.getRoleFromToken("token")).thenReturn("MANAGER");
        assertThat(authenticatedUser.isWithEmployeeId(requestedEmployeeId)).isEqualTo(requestedEmployeeId);
        assertThat(authenticatedUser.isWithEmployeeId(null)).isEqualTo(loggedEmployeeId);

        when(jwtUtils.getRoleFromToken("token")).thenReturn("CTO");
        assertThat(authenticatedUser.isWithEmployeeId(requestedEmployeeId)).isEqualTo(requestedEmployeeId);
        assertThat(authenticatedUser.isWithEmployeeId(null)).isEqualTo(loggedEmployeeId);
    }
}
