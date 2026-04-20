package com.kts.kronos.adapter.out.security;

import com.kts.kronos.domain.model.enuns.Role;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticatedUserRoleStrategyTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private HttpServletRequest request;

    private JwtAuthenticatedUser jwtAuthenticatedUser;

    @BeforeEach
    void setUp() {
        jwtAuthenticatedUser = new JwtAuthenticatedUser(jwtUtils, request);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldResolveCurrentRoleFromSecurityContextInsteadOfTokenClaim() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "partner.user",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_PARTNER"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Role currentRole = jwtAuthenticatedUser.getCurrentRole();

        assertEquals(Role.PARTNER, currentRole);
        assertFalse(jwtAuthenticatedUser.hasAnyRole(Role.MANAGER, Role.CTO));
        assertTrue(jwtAuthenticatedUser.hasAnyRole(Role.PARTNER));
        verifyNoInteractions(jwtUtils);
    }

    @Test
    void shouldExtractEmployeeIdFromBearerToken() {
        UUID employeeId = UUID.randomUUID();
        when(request.getHeader("Authorization")).thenReturn("Bearer jwt-token");
        when(jwtUtils.getEmployeeIdFromToken("jwt-token")).thenReturn(employeeId);

        assertEquals(employeeId, jwtAuthenticatedUser.getEmployeeId());
    }

    @Test
    void shouldFailWhenEmployeeIdClaimIsMissing() {
        when(request.getHeader("Authorization")).thenReturn("Bearer jwt-token");
        when(jwtUtils.getEmployeeIdFromToken("jwt-token")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> jwtAuthenticatedUser.getEmployeeId());
    }

    @Test
    void shouldExtractUserIdFromBearerToken() {
        UUID userId = UUID.randomUUID();
        when(request.getHeader("Authorization")).thenReturn("Bearer jwt-token");
        when(jwtUtils.getUserIdFromToken("jwt-token")).thenReturn(userId);

        assertEquals(userId, jwtAuthenticatedUser.getuserId());
    }

    @Test
    void shouldFailWhenUserIdClaimIsMissing() {
        when(request.getHeader("Authorization")).thenReturn("Bearer jwt-token");
        when(jwtUtils.getUserIdFromToken("jwt-token")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> jwtAuthenticatedUser.getuserId());
    }

    @Test
    void shouldExtractUsernameFromBearerToken() {
        when(request.getHeader("Authorization")).thenReturn("Bearer jwt-token");
        when(jwtUtils.getUsernameFromToken("jwt-token")).thenReturn("alice");

        assertEquals("alice", jwtAuthenticatedUser.getUsername());
    }

    @Test
    void shouldFailWhenAuthorizationHeaderIsMissing() {
        when(request.getHeader("Authorization")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> jwtAuthenticatedUser.getUsername());
    }

    @Test
    void shouldFailWhenAuthorizationHeaderIsNotBearer() {
        when(request.getHeader("Authorization")).thenReturn("Basic abc");

        assertThrows(IllegalArgumentException.class, () -> jwtAuthenticatedUser.getUsername());
    }

    @Test
    void shouldFailWhenAuthenticationIsMissing() {
        assertThrows(IllegalArgumentException.class, () -> jwtAuthenticatedUser.getCurrentRole());
    }

    @Test
    void shouldFailForAnonymousAuthentication() {
        var authentication = new AnonymousAuthenticationToken(
                "key",
                "anonymous",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThrows(IllegalArgumentException.class, () -> jwtAuthenticatedUser.getCurrentRole());
    }

    @Test
    void shouldFailWhenAuthenticatedContextHasNoRoleAuthority() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "user",
                null,
                List.of(new SimpleGrantedAuthority("SCOPE_read"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThrows(IllegalArgumentException.class, () -> jwtAuthenticatedUser.getCurrentRole());
    }
}
