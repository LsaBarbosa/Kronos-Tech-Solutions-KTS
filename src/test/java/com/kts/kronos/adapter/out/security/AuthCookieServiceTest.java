package com.kts.kronos.adapter.out.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthCookieServiceTest {

    @Test
    void shouldRejectSameSiteNoneWithoutSecure() {
        var service = baseService();
        ReflectionTestUtils.setField(service, "sameSite", "None");
        ReflectionTestUtils.setField(service, "secure", false);
        ReflectionTestUtils.setField(service, "activeProfiles", "dev");

        assertThatThrownBy(service::validateCookieSecurityConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AUTH_COOKIE_SAME_SITE=None");
    }

    @Test
    void shouldRejectInsecureCookieInProduction() {
        var service = baseService();
        ReflectionTestUtils.setField(service, "secure", false);
        ReflectionTestUtils.setField(service, "activeProfiles", "prod");

        assertThatThrownBy(service::validateCookieSecurityConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AUTH_COOKIE_SECURE");
    }

    @Test
    void shouldRejectSameSiteNoneWhenTopologyIsNotCrossSite() {
        var service = baseService();
        ReflectionTestUtils.setField(service, "sameSite", "None");
        ReflectionTestUtils.setField(service, "cookieDomain", "example.com");
        ReflectionTestUtils.setField(service, "platformFrontendUrl", "https://app.example.com");

        assertThatThrownBy(service::validateCookieSecurityConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cross-site real");
    }

    @Test
    void shouldRejectCookieDomainNotMatchingFrontendHosts() {
        var service = baseService();
        ReflectionTestUtils.setField(service, "cookieDomain", "example.com");
        ReflectionTestUtils.setField(service, "platformFrontendUrl", "https://frontend.kronos.com");

        assertThatThrownBy(service::validateCookieSecurityConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AUTH_COOKIE_DOMAIN");
    }

    @Test
    void shouldRejectCookiePathWithoutLeadingSlash() {
        var service = baseService();
        ReflectionTestUtils.setField(service, "cookiePath", "auth");

        assertThatThrownBy(service::validateCookieSecurityConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AUTH_COOKIE_PATH");
    }

    @Test
    void shouldRejectInvalidHostPrefixConfiguration() {
        var service = baseService();
        ReflectionTestUtils.setField(service, "cookieName", "__Host-KTS_SESSION");
        ReflectionTestUtils.setField(service, "cookieDomain", "example.com");

        assertThatThrownBy(service::validateCookieSecurityConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("__Host-");
    }

    @Test
    void shouldAllowSecureHostPrefixConfiguration() {
        var service = baseService();
        ReflectionTestUtils.setField(service, "cookieName", "__Host-KTS_SESSION");
        ReflectionTestUtils.setField(service, "secure", true);
        ReflectionTestUtils.setField(service, "cookiePath", "/");
        ReflectionTestUtils.setField(service, "cookieDomain", "");

        assertThatCode(service::validateCookieSecurityConfiguration).doesNotThrowAnyException();
    }


    private AuthCookieService baseService() {
        var service = new AuthCookieService();
        ReflectionTestUtils.setField(service, "cookieName", "KTS_SESSION");
        ReflectionTestUtils.setField(service, "secure", true);
        ReflectionTestUtils.setField(service, "httpOnly", true);
        ReflectionTestUtils.setField(service, "sameSite", "Lax");
        ReflectionTestUtils.setField(service, "cookiePath", "/");
        ReflectionTestUtils.setField(service, "cookieDomain", "");
        ReflectionTestUtils.setField(service, "recordFrontendUrl", "");
        ReflectionTestUtils.setField(service, "platformFrontendUrl", "");
        ReflectionTestUtils.setField(service, "localFrontendUrl", "");
        ReflectionTestUtils.setField(service, "local2FrontendUrl", "");
        ReflectionTestUtils.setField(service, "activeProfiles", "");
        ReflectionTestUtils.setField(service, "defaultProfiles", "prod");
        ReflectionTestUtils.setField(service, "jwtExpirationMs", 3600000L);
        return service;
    }
}
