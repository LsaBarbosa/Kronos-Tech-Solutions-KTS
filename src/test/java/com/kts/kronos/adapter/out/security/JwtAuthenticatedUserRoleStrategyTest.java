package com.kts.kronos.adapter.out.security;

import com.kts.kronos.domain.model.enuns.Role;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;

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
}
