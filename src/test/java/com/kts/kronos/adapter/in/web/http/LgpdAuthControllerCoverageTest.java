package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.lgpd.*;
import com.kts.kronos.adapter.out.security.AuthCookieService;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.legal.DataProcessingCatalog;
import com.kts.kronos.application.port.in.usecase.AuthUseCase;
import com.kts.kronos.application.port.in.usecase.LgpdUseCase;
import com.kts.kronos.application.security.ClientIpResolver;
import com.kts.kronos.application.service.retention.RetentionExecutionService;
import com.kts.kronos.domain.model.AnonymizationConsolidatedResult;
import com.kts.kronos.domain.model.LgpdRequest;
import com.kts.kronos.domain.model.enuns.*;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LgpdAuthControllerCoverageTest {

    // ── LgpdController ────────────────────────────────────────────────────────

    @Mock private LgpdUseCase lgpdUseCase;
    @Mock private ClientIpResolver clientIpResolver;
    @Mock private DataProcessingCatalog dataProcessingCatalog;
    @Mock private RetentionExecutionService retentionExecutionService;
    @Mock private HttpServletRequest httpServletRequest;

    @Test
    void lgpdController_getRequest_returnsOk() {
        var controller = new LgpdController(lgpdUseCase, clientIpResolver, dataProcessingCatalog, retentionExecutionService);
        UUID requestId = UUID.randomUUID();
        var req = buildRequest(requestId);
        when(lgpdUseCase.getRequest(requestId)).thenReturn(req);

        var response = controller.getRequest(requestId);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }

    @Test
    void lgpdController_transitionStatus_returnsOk() {
        var controller = new LgpdController(lgpdUseCase, clientIpResolver, dataProcessingCatalog, retentionExecutionService);
        UUID requestId = UUID.randomUUID();
        var req = buildRequest(requestId);
        var transitionReq = new LgpdRequestTransitionRequest(LgpdRequestStatus.IN_ANALYSIS, "notes", "internal", null);
        when(lgpdUseCase.transitionStatus(eq(requestId), any(), any(), any(), any())).thenReturn(req);

        var response = controller.transitionStatus(requestId, transitionReq);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void lgpdController_requestComplement_returnsOk() {
        var controller = new LgpdController(lgpdUseCase, clientIpResolver, dataProcessingCatalog, retentionExecutionService);
        UUID requestId = UUID.randomUUID();
        var req = buildRequest(requestId);
        var complementReq = new RequestComplementRequest("please provide more info");
        when(lgpdUseCase.requestDataSubjectComplement(requestId, "please provide more info")).thenReturn(req);

        var response = controller.requestComplement(requestId, complementReq);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void lgpdController_cancelRequest_returnsOk() {
        var controller = new LgpdController(lgpdUseCase, clientIpResolver, dataProcessingCatalog, retentionExecutionService);
        UUID requestId = UUID.randomUUID();
        var req = buildRequest(requestId);
        var cancelReq = new CancelRequestRequest("motivo");
        when(lgpdUseCase.cancelRequest(requestId, "motivo")).thenReturn(req);

        var response = controller.cancelRequest(requestId, cancelReq);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void lgpdController_getAnonymizationResult_nonNull_returnsOk() {
        var controller = new LgpdController(lgpdUseCase, clientIpResolver, dataProcessingCatalog, retentionExecutionService);
        UUID requestId = UUID.randomUUID();
        var result = buildAnonymizationResult();
        when(lgpdUseCase.getAnonymizationResult(requestId)).thenReturn(result);

        var response = controller.getAnonymizationResult(requestId);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }

    @Test
    void lgpdController_getAnonymizationResult_null_returnsNoContent() {
        var controller = new LgpdController(lgpdUseCase, clientIpResolver, dataProcessingCatalog, retentionExecutionService);
        UUID requestId = UUID.randomUUID();
        when(lgpdUseCase.getAnonymizationResult(requestId)).thenReturn(null);

        var response = controller.getAnonymizationResult(requestId);

        assertEquals(204, response.getStatusCode().value());
    }

    @Test
    void lgpdController_dryRunAnonymizationForRequest_returnsOk() {
        var controller = new LgpdController(lgpdUseCase, clientIpResolver, dataProcessingCatalog, retentionExecutionService);
        UUID requestId = UUID.randomUUID();
        var dryRunResp = mock(AnonymizationDryRunWithTokenResponse.class);
        when(lgpdUseCase.executeDryRunAnonymizationForRequest(requestId)).thenReturn(dryRunResp);

        var response = controller.dryRunAnonymizationForRequest(requestId);

        assertEquals(200, response.getStatusCode().value());
        assertSame(dryRunResp, response.getBody());
    }

    @Test
    void lgpdController_applyAnonymizationForRequest_returnsOk() {
        var controller = new LgpdController(lgpdUseCase, clientIpResolver, dataProcessingCatalog, retentionExecutionService);
        UUID requestId = UUID.randomUUID();
        var applyReq = new AnonymizationApplyRequest("justificativa válida", true, UUID.randomUUID());
        var result = buildAnonymizationResult();
        when(clientIpResolver.resolve(any())).thenReturn("127.0.0.1");
        when(lgpdUseCase.applyAnonymizationForRequest(eq(requestId), anyString(), eq(true), any(), anyString(), any())).thenReturn(result);

        var response = controller.applyAnonymizationForRequest(requestId, applyReq, "TestAgent", httpServletRequest);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }

    // ── AuthController ────────────────────────────────────────────────────────

    @Mock private AuthUseCase authUseCase;
    @Mock private AuthCookieService authCookieService;
    @Mock private JwtUtils jwtUtils;

    @Test
    void authController_logout_withToken_callsLogout() {
        var controller = new AuthController(authUseCase, authCookieService, jwtUtils);
        var mockCookie = ResponseCookie.from("access_token", "").maxAge(0).build();
        when(authCookieService.extractToken(any())).thenReturn(Optional.of("bearer-token"));
        when(authCookieService.expireAccessTokenCookie()).thenReturn(mockCookie);

        var response = controller.logout(httpServletRequest);

        assertEquals(204, response.getStatusCode().value());
        verify(authUseCase).logout("bearer-token"); // ifPresent lambda covered
    }

    @Test
    void authController_switchCompany_returnsNoContent() {
        var controller = new AuthController(authUseCase, authCookieService, jwtUtils);
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        var mockCookie = ResponseCookie.from("access_token", "new-token").build();
        when(authCookieService.extractToken(any())).thenReturn(Optional.of("current-token"));
        when(jwtUtils.getUserIdFromToken("current-token")).thenReturn(userId);
        when(authUseCase.switchCompany(userId, companyId)).thenReturn("new-token");
        when(authCookieService.createAccessTokenCookie("new-token")).thenReturn(mockCookie);

        var response = controller.switchCompany(new com.kts.kronos.adapter.in.web.dto.auth.SwitchCompanyRequest(companyId), httpServletRequest);

        assertEquals(204, response.getStatusCode().value());
        verify(authUseCase).switchCompany(userId, companyId);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private LgpdRequest buildRequest(UUID requestId) {
        return new LgpdRequest(
                requestId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                LgpdRequestType.ACCESS, LgpdRequestStatus.OPEN,
                "Descrição", null, Instant.now(), Instant.now(),
                null, null, null, Instant.now().plusSeconds(86400),
                "NORMAL", null, null, null, null, null, false
        );
    }

    private AnonymizationConsolidatedResult buildAnonymizationResult() {
        Instant now = Instant.now();
        return new AnonymizationConsolidatedResult(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                AnonymizationConsolidatedStatus.SUCCESS,
                "DRY_RUN", now, now.plusMillis(100),
                10L, 5L, 2L, 0L,
                List.of(), List.of(), List.of()
        );
    }
}
