package com.kts.kronos.application.cache;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class CacheScopesCoverageTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // ── L17: authentication.getName().isBlank() = TRUE → "anonymous" ──────────
    // When authentication exists but getName() returns blank → principal = "anonymous"

    @Test
    void authenticatedScope_withBlankPrincipalName_useAnonymousPrincipal() {
        var auth = new UsernamePasswordAuthenticationToken(
            "   ", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        String scope = CacheScopes.authenticatedScope("test");
        // getName()="   " → isBlank=TRUE → principal="anonymous"
        assertTrue(scope.startsWith("anonymous|"));
    }

    // ── L17: authentication.getName() == null → "anonymous" ──────────────────
    // authentication exists but getName() returns null

    @Test
    void authenticatedScope_withNullPrincipalName_useAnonymousPrincipal() {
        var auth = new UsernamePasswordAuthenticationToken(
            (Object) null, null, List.of()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        String scope = CacheScopes.authenticatedScope("part1");
        assertTrue(scope.startsWith("anonymous|"));
    }

    // ── L34: scopeFromPrincipalAndFilters → calls authenticatedScope (L34 covered) ─

    @Test
    void scopeFromPrincipalAndFilters_delegatesToAuthenticatedScope() {
        SecurityContextHolder.clearContext();
        String result = CacheScopes.scopeFromPrincipalAndFilters("a", "b");
        assertNotNull(result);
    }

    // ── L41: parts != null && parts.length > 0 = FALSE (no parts) ─────────────

    @Test
    void authenticatedScope_withNoParts_skipPartsBlock() {
        SecurityContextHolder.clearContext();
        String result = CacheScopes.publicScope(); // no parts → parts.length=0
        assertEquals("public|anonymous", result);
    }

    // ── L43: value == null → "unknown" in lambda ─────────────────────────────

    @Test
    void authenticatedScope_withNullPart_substitutesUnknown() {
        SecurityContextHolder.clearContext();
        String result = CacheScopes.publicScope(null, "valid");
        assertTrue(result.contains("unknown"));
        assertTrue(result.contains("valid"));
    }

    // ── L43: value.isBlank() = TRUE → "unknown" in lambda ─────────────────────

    @Test
    void authenticatedScope_withBlankPart_substitutesUnknown() {
        SecurityContextHolder.clearContext();
        String result = CacheScopes.publicScope("   ", "val");
        assertTrue(result.contains("unknown"));
    }

    // ── getName() == null = TRUE → "anonymous" principal (dead code branch) ────

    @Test
    void authenticatedScope_withAuthGetNameReturningNull_usesAnonymous() {
        // Mock Authentication.getName() to return null
        Authentication mockAuth = Mockito.mock(Authentication.class);
        when(mockAuth.getName()).thenReturn(null);
        // doReturn bypasses wildcard type-check on Collection<? extends GrantedAuthority>
        Mockito.doReturn(Collections.emptyList()).when(mockAuth).getAuthorities();
        SecurityContextHolder.getContext().setAuthentication(mockAuth);

        String scope = CacheScopes.authenticatedScope("part");
        // getName()==null=TRUE → principal = "anonymous"
        assertTrue(scope.startsWith("anonymous|"));
    }

    // ── join: parts == null = TRUE → skip addAll (dead code branch via reflection) ─

    @Test
    void join_withNullParts_skipsParts() throws Exception {
        // Call private join(String, String, String[]) with null parts
        Method join = CacheScopes.class.getDeclaredMethod("join", String.class, String.class, String[].class);
        join.setAccessible(true);
        // null varargs → parts == null = TRUE → condition FALSE → no parts added
        String result = (String) join.invoke(null, "principal", "roles", (Object) null);
        assertEquals("principal|roles", result);
    }
}
