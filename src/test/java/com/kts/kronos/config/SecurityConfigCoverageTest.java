package com.kts.kronos.config;

import com.kts.kronos.adapter.in.web.exceptions.DelegatedAuthenticationEntryPoint;
import com.kts.kronos.adapter.in.web.exceptions.JsonAccessDeniedHandler;
import com.kts.kronos.adapter.out.security.AuthCookieService;
import com.kts.kronos.adapter.out.security.CustomUserDetailsService;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.port.out.provider.TokenBlacklistProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SecurityConfigCoverageTest {

    @Mock private JwtUtils jwtUtils;
    @Mock private CustomUserDetailsService customUserDetailsService;
    @Mock private DelegatedAuthenticationEntryPoint delegatedAuthenticationEntryPoint;
    @Mock private TokenBlacklistProvider tokenBlacklistProvider;
    @Mock private UserProvider userProvider;
    @Mock private AuthCookieService authCookieService;
    @Mock private JsonAccessDeniedHandler jsonAccessDeniedHandler;
    @Mock private HandlerExceptionResolver handlerExceptionResolver;

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(
                jwtUtils, customUserDetailsService, userProvider,
                delegatedAuthenticationEntryPoint, tokenBlacklistProvider,
                authCookieService, jsonAccessDeniedHandler, handlerExceptionResolver);
        ReflectionTestUtils.setField(securityConfig, "csrfHeaderName", "X-CSRF-TOKEN");
    }

    @SuppressWarnings("unchecked")
    private List<String> invokeResolveAllowedOrigins() throws Exception {
        Method m = SecurityConfig.class.getDeclaredMethod("resolveAllowedOrigins");
        m.setAccessible(true);
        return (List<String>) m.invoke(securityConfig);
    }

    private RequestMatcher invokePost(String path) throws Exception {
        Method m = SecurityConfig.class.getDeclaredMethod("post", String.class);
        m.setAccessible(true);
        return (RequestMatcher) m.invoke(securityConfig, path);
    }

    // ── resolveAllowedOrigins: allowedOriginsRaw with blank entries → filtered (BR L205 FALSE) ─

    @Test
    void resolveAllowedOrigins_withRawOriginsContainingBlanks_filtersBlankEntries() throws Exception {
        // "http://a.test,  ,http://b.test" → split → ["http://a.test", "  ", "http://b.test"]
        // trim → ["http://a.test", "", "http://b.test"]
        // filter !isBlank() → BR L205 FALSE on the empty string → filtered out
        ReflectionTestUtils.setField(securityConfig, "allowedOriginsRaw", "http://a.test,  ,http://b.test");

        List<String> origins = invokeResolveAllowedOrigins();

        assertEquals(List.of("http://a.test", "http://b.test"), origins);
    }

    // ── resolveAllowedOrigins: no allowedOriginsRaw; null URL field → filtered (BR L211 FALSE) ─

    @Test
    void resolveAllowedOrigins_withNullUrlField_filtersNullOrigins() throws Exception {
        // allowedOriginsRaw=null → second path; recordUrl=null → origin==null → filter FALSE → filtered
        ReflectionTestUtils.setField(securityConfig, "allowedOriginsRaw", null);
        ReflectionTestUtils.setField(securityConfig, "recordUrl", null);       // null → filtered
        ReflectionTestUtils.setField(securityConfig, "plataformUrl", "http://platform.local");
        ReflectionTestUtils.setField(securityConfig, "local", "http://local.test");
        ReflectionTestUtils.setField(securityConfig, "local_2", null);         // null → filtered

        List<String> origins = invokeResolveAllowedOrigins();

        assertEquals(List.of("http://platform.local", "http://local.test"), origins);
    }

    // ── resolveAllowedOrigins: no allowedOriginsRaw; blank URL field → filtered (BR L211 FALSE) ─

    @Test
    void resolveAllowedOrigins_withBlankUrlField_filtersBlankOrigins() throws Exception {
        ReflectionTestUtils.setField(securityConfig, "allowedOriginsRaw", null);
        ReflectionTestUtils.setField(securityConfig, "recordUrl", "");           // blank → filtered
        ReflectionTestUtils.setField(securityConfig, "plataformUrl", "http://p.local");
        ReflectionTestUtils.setField(securityConfig, "local", "");              // blank → filtered
        ReflectionTestUtils.setField(securityConfig, "local_2", "http://l2.local");

        List<String> origins = invokeResolveAllowedOrigins();

        assertEquals(List.of("http://p.local", "http://l2.local"), origins);
    }

    // ── post() RequestMatcher: GET request → !"POST".equalsIgnoreCase = TRUE → return false (BR L243 TRUE) ─

    @Test
    void post_requestMatcher_withGetRequest_returnsFalse() throws Exception {
        RequestMatcher matcher = invokePost("/auth/login");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/login");
        request.setServletPath("/auth/login");

        assertFalse(matcher.matches(request)); // L243: !"POST" = TRUE → return false
    }

    // ── post() RequestMatcher: POST + servletPath matches → return true (BR L243 FALSE, BR L246 TRUE via servletPath) ─

    @Test
    void post_requestMatcher_withPostAndMatchingServletPath_returnsTrue() throws Exception {
        RequestMatcher matcher = invokePost("/auth/login");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setServletPath("/auth/login"); // servletPath matches → return true

        assertTrue(matcher.matches(request)); // L243: !"POST" = FALSE; L246: servletPath match → true
    }

    // ── post() RequestMatcher: POST + servletPath mismatch but requestURI matches → return true (BR L246 via requestURI) ─

    @Test
    void post_requestMatcher_withPostAndMatchingRequestUri_returnsTrue() throws Exception {
        RequestMatcher matcher = invokePost("/auth/login");
        // MockHttpServletRequest: getServletPath() returns "" by default when not set; getRequestURI() = contextPath + servletPath
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setServletPath(""); // empty → won't match "/auth/login"
        // getRequestURI() = "/auth/login" (from constructor) → matches

        assertTrue(matcher.matches(request)); // path.equals(requestURI) → true
    }

    // ── post() RequestMatcher: POST + neither path matches → return false ─────

    @Test
    void post_requestMatcher_withPostAndNoPathMatch_returnsFalse() throws Exception {
        RequestMatcher matcher = invokePost("/auth/login");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/other/path");
        request.setServletPath("/other/path");
        // neither servletPath nor requestURI matches "/auth/login"

        assertFalse(matcher.matches(request)); // path.equals → false for both
    }
}
