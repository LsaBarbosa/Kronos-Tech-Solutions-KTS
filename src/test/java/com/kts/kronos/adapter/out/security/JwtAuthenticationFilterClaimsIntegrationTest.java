package com.kts.kronos.adapter.out.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
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
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterClaimsIntegrationTest {

    private static final String ISSUER = "kronos-test-api";
    private static final String AUDIENCE = "kronos-test-clients";
    private static final String CURRENT_KEY_ID = "test-current";
    private static final String CURRENT_SECRET = Base64.getEncoder()
            .encodeToString("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8));

    @Mock
    private UserDetailsService userDetailsService;

    private JwtUtils jwtUtils;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils(
                CURRENT_SECRET,
                60_000L,
                ISSUER,
                AUDIENCE,
                CURRENT_KEY_ID,
                "",
                30L,
                0L
        );
        filter = new JwtAuthenticationFilter(jwtUtils, userDetailsService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("deve autenticar token real com issuer e audience válidos")
    void shouldAuthenticateRealTokenWithValidIssuerAndAudience() throws Exception {
        String token = jwtUtils.generateToken(UUID.randomUUID(), "alice", "MANAGER", UUID.randomUUID(), true);
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        var response = new MockHttpServletResponse();

        when(userDetailsService.loadUserByUsername("alice"))
                .thenReturn(User.withUsername("alice").password("encoded").authorities("ROLE_MANAGER").build());

        filter.doFilter(request, response, new MockFilterChain());

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("deve rejeitar token real com audience inválida antes de carregar usuário")
    void shouldRejectRealTokenWithInvalidAudienceBeforeLoadingUser() throws Exception {
        String token = tokenWithIssuerAndAudience(ISSUER, "other-client");
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("deve rejeitar token real com issuer inválido antes de carregar usuário")
    void shouldRejectRealTokenWithInvalidIssuerBeforeLoadingUser() throws Exception {
        String token = tokenWithIssuerAndAudience("other-issuer", AUDIENCE);
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(userDetailsService);
    }

    private String tokenWithIssuerAndAudience(String issuer, String audience) {
        Date now = new Date();
        return Jwts.builder()
                .setHeaderParam("kid", CURRENT_KEY_ID)
                .setIssuer(issuer)
                .setAudience(audience)
                .setId(UUID.randomUUID().toString())
                .setSubject("alice")
                .setNotBefore(now)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + 60_000L))
                .signWith(
                        Keys.hmacShaKeyFor(Base64.getDecoder().decode(CURRENT_SECRET)),
                        SignatureAlgorithm.HS256
                )
                .compact();
    }
}
