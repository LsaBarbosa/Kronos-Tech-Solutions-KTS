package com.kts.kronos.application.security;

import com.kts.kronos.application.config.ClientIpResolverProperties;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.TooManyRequestsException;
import com.kts.kronos.application.port.out.provider.LivenessVerificationProvider;
import com.kts.kronos.domain.model.LivenessVerificationResult;
import com.kts.kronos.domain.model.enuns.LivenessOperation;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BiometricProtectionServiceTest {

    private BiometricProtectionService service;
    private PrivacyLogReferenceService privacyLogReferenceService;
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(HttpServletRequest.class, MockHttpServletRequest::new)
            .withBean(ClientIpResolverProperties.class, ClientIpResolverProperties::new)
            .withBean(ClientIpResolver.class)
            .withBean(PrivacyLogReferenceService.class, () -> new PrivacyLogReferenceService("test-log-secret"))
            .withBean(BiometricProtectionService.class);

    @BeforeEach
    void setUp() {
        privacyLogReferenceService = new PrivacyLogReferenceService("test-log-secret");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.42");
        ClientIpResolverProperties properties = new ClientIpResolverProperties();
        LivenessVerificationProvider mockProvider = mock(LivenessVerificationProvider.class);
        when(mockProvider.verify(any(), any(), any()))
                .thenReturn(LivenessVerificationResult.passed("MOCK_PROVIDER", 0.9));
        service = new BiometricProtectionService(
                request,
                new ClientIpResolver(properties),
                mockProvider,
                privacyLogReferenceService
        );
        ReflectionTestUtils.setField(service, "maxBase64Chars", 10);
        ReflectionTestUtils.setField(service, "livenessRequired", false);
        ReflectionTestUtils.setField(service, "loginFaceLimit", 2);
        ReflectionTestUtils.setField(service, "loginFaceWindowSeconds", 60);
        ReflectionTestUtils.setField(service, "checkinLimit", 2);
        ReflectionTestUtils.setField(service, "checkinWindowSeconds", 60);
        ReflectionTestUtils.setField(service, "enrollmentLimit", 2);
        ReflectionTestUtils.setField(service, "enrollmentWindowSeconds", 60);
    }

    @Test
    @DisplayName("protectPublicLogin: aceita payload nulo ou vazio e liveness válido")
    void shouldAcceptNullAndBlankPayloads() {
        assertDoesNotThrow(() -> service.protectPublicLogin(null, true));
        assertDoesNotThrow(() -> service.protectPublicLogin("   ", true));
    }

    @Test
    @DisplayName("protectPublicLogin: aceita liveness desabilitado sem provider configurado")
    void shouldAcceptDisabledLivenessWithoutProvider() {
        BiometricProtectionService serviceWithoutProvider = new BiometricProtectionService(
                new MockHttpServletRequest(),
                new ClientIpResolver(new ClientIpResolverProperties()),
                null,
                privacyLogReferenceService
        );
        ReflectionTestUtils.setField(serviceWithoutProvider, "maxBase64Chars", 10);
        ReflectionTestUtils.setField(serviceWithoutProvider, "livenessRequired", false);
        ReflectionTestUtils.setField(serviceWithoutProvider, "loginFaceLimit", 1);
        ReflectionTestUtils.setField(serviceWithoutProvider, "loginFaceWindowSeconds", 60);

        assertDoesNotThrow(() -> serviceWithoutProvider.protectPublicLogin("abc", null));
    }

    @Test
    @DisplayName("contexto Spring: cria serviço com liveness desabilitado sem provider")
    void shouldCreateSpringBeanWhenLivenessDisabledAndProviderMissing() {
        contextRunner
                .withPropertyValues(
                        "biometric.max-base64-chars=10",
                        "biometric.liveness-required=false",
                        "biometric.login-face.limit=1",
                        "biometric.login-face.window-seconds=60"
                )
                .run(context -> {
                    BiometricProtectionService bean = context.getBean(BiometricProtectionService.class);

                    assertNotNull(bean);
                    assertDoesNotThrow(() -> bean.protectPublicLogin("abc", null));
                });
    }

    @Test
    @DisplayName("protectPublicLogin: deve aplicar rate limit por IP")
    void shouldRateLimitPublicLogin() {
        service.protectPublicLogin("abc", null);
        service.protectPublicLogin("abc", null);

        assertThrows(TooManyRequestsException.class, () -> service.protectPublicLogin("abc", null));
    }

    @Test
    @DisplayName("protectPublicLogin: expira entradas antigas da janela")
    void shouldExpireOldRateLimitEntries() {
        ReflectionTestUtils.setField(service, "loginFaceWindowSeconds", 0);

        assertDoesNotThrow(() -> service.protectPublicLogin("abc", null));
        assertDoesNotThrow(() -> service.protectPublicLogin("abc", null));
        assertDoesNotThrow(() -> service.protectPublicLogin("abc", null));
    }

    @Test
    @DisplayName("protectPublicLogin: usa unknown quando IP não vem na request")
    void shouldUseUnknownClientIpWhenRemoteAddrIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("");
        ClientIpResolverProperties properties = new ClientIpResolverProperties();
        LivenessVerificationProvider mockProvider = mock(LivenessVerificationProvider.class);
        when(mockProvider.verify(any(), any(), any()))
                .thenReturn(LivenessVerificationResult.passed("MOCK_PROVIDER", 0.9));
        BiometricProtectionService blankIpService = new BiometricProtectionService(
                request,
                new ClientIpResolver(properties),
                mockProvider,
                privacyLogReferenceService
        );
        ReflectionTestUtils.setField(blankIpService, "maxBase64Chars", 10);
        ReflectionTestUtils.setField(blankIpService, "livenessRequired", false);
        ReflectionTestUtils.setField(blankIpService, "loginFaceLimit", 1);
        ReflectionTestUtils.setField(blankIpService, "loginFaceWindowSeconds", 60);

        blankIpService.protectPublicLogin("abc", null);

        assertThrows(TooManyRequestsException.class, () -> blankIpService.protectPublicLogin("abc", null));
    }

    @Test
    @DisplayName("protectCheckIn: aplica rate limit por colaborador e IP")
    void shouldRateLimitCheckIn() {
        UUID employeeId = UUID.randomUUID();
        service.protectCheckIn(employeeId, "abc", true);
        service.protectCheckIn(employeeId, "abc", true);

        assertThrows(TooManyRequestsException.class, () -> service.protectCheckIn(employeeId, "abc", true));
    }

    @Test
    @DisplayName("protectEnrollment: aplica rate limit por colaborador e IP")
    void shouldRateLimitEnrollment() {
        UUID employeeId = UUID.randomUUID();
        service.protectEnrollment(employeeId, "abc", true);
        service.protectEnrollment(employeeId, "abc", true);

        assertThrows(TooManyRequestsException.class, () -> service.protectEnrollment(employeeId, "abc", true));
    }

    @Test
    @DisplayName("protectEnrollment: deve rejeitar payload biométrico acima do limite")
    void shouldRejectOversizedBiometricPayload() {
        assertThrows(BadRequestException.class,
                () -> service.protectEnrollment(UUID.randomUUID(), "abcdefghijk", true));
    }

    @Test
    @DisplayName("protectCheckIn: deve exigir liveness quando configurado")
    void shouldRequireLivenessWhenEnabled() {
        LivenessVerificationProvider mockProvider = mock(LivenessVerificationProvider.class);
        when(mockProvider.verify(any(), any(), any()))
                .thenReturn(LivenessVerificationResult.failed("MOCK_PROVIDER", "LIVENESS_FAILED"));
        service = new BiometricProtectionService(
                new MockHttpServletRequest(),
                new ClientIpResolver(new ClientIpResolverProperties()),
                mockProvider,
                privacyLogReferenceService
        );
        ReflectionTestUtils.setField(service, "maxBase64Chars", 10);
        ReflectionTestUtils.setField(service, "livenessRequired", true);

        assertThrows(ForbiddenException.class,
                () -> service.protectCheckIn(UUID.randomUUID(), "abc", false));
    }

    @Test
    @DisplayName("protectCheckIn: deve bloquear liveness obrigatório sem provider configurado")
    void shouldRejectEnabledLivenessWithoutProvider() {
        service = new BiometricProtectionService(
                new MockHttpServletRequest(),
                new ClientIpResolver(new ClientIpResolverProperties()),
                null,
                privacyLogReferenceService
        );
        ReflectionTestUtils.setField(service, "maxBase64Chars", 10);
        ReflectionTestUtils.setField(service, "livenessRequired", true);

        assertThrows(ForbiddenException.class,
                () -> service.protectCheckIn(UUID.randomUUID(), "abc", false));
    }

    @Test
    @DisplayName("protectPublicLogin: deve exigir liveness quando configurado para produção")
    void shouldRequireLivenessForPublicLoginWhenEnabled() {
        LivenessVerificationProvider mockProvider = mock(LivenessVerificationProvider.class);
        when(mockProvider.verify(any(), any(), any()))
                .thenReturn(LivenessVerificationResult.failed("MOCK_PROVIDER", "LIVENESS_FAILED"));
        service = new BiometricProtectionService(
                new MockHttpServletRequest(),
                new ClientIpResolver(new ClientIpResolverProperties()),
                mockProvider,
                privacyLogReferenceService
        );
        ReflectionTestUtils.setField(service, "maxBase64Chars", 10);
        ReflectionTestUtils.setField(service, "livenessRequired", true);

        assertThrows(ForbiddenException.class, () -> service.protectPublicLogin("abc", false));
    }

    @Test
    @DisplayName("protectEnrollment: deve exigir liveness quando configurado (LGPD-S01-03)")
    void shouldRequireLivenessForEnrollmentWhenEnabled() {
        LivenessVerificationProvider mockProvider = mock(LivenessVerificationProvider.class);
        when(mockProvider.verify(any(), any(), any()))
                .thenReturn(LivenessVerificationResult.failed("MOCK_PROVIDER", "LIVENESS_FAILED"));
        service = new BiometricProtectionService(
                new MockHttpServletRequest(),
                new ClientIpResolver(new ClientIpResolverProperties()),
                mockProvider,
                privacyLogReferenceService
        );
        ReflectionTestUtils.setField(service, "maxBase64Chars", 10);
        ReflectionTestUtils.setField(service, "livenessRequired", true);
        UUID employeeId = UUID.randomUUID();

        assertThrows(ForbiddenException.class,
                () -> service.protectEnrollment(employeeId, "abc", false));
    }

    @Test
    @DisplayName("protectEnrollment: aceita enrollment com liveness quando configurado (LGPD-S01-03)")
    void shouldAcceptEnrollmentWithLivenessWhenEnabled() {
        ReflectionTestUtils.setField(service, "livenessRequired", true);
        UUID employeeId = UUID.randomUUID();

        service.protectEnrollment(employeeId, "abc", true);
    }
}
