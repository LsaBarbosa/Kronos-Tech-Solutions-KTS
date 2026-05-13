package com.kts.kronos.adapter.in.web.http.webmvc;

import com.kts.kronos.adapter.in.web.exceptions.RestExceptionHandler;
import com.kts.kronos.adapter.in.web.http.TermsController;
import com.kts.kronos.adapter.out.security.AuthCookieService;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.security.ClientIpResolver;
import com.kts.kronos.domain.model.enuns.Role;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.annotation.Resource;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TermsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RestExceptionHandler.class, AuthCookieService.class})
class TermsControllerWebMvcTest {

    @Resource
    private MockMvc mockMvc;

    @MockitoBean
    private AcceptTermsUseCase acceptTermsUseCase;

    @MockitoBean
    private JwtAuthenticatedUser jwtAuthenticatedUser;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private ClientIpResolver clientIpResolver;

    @Test
    void shouldAcceptBiometricTermsAndReturnRenewedToken() throws Exception {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(jwtAuthenticatedUser.getUsername()).thenReturn("lucas");
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(jwtUtils.generateToken(employeeId, "lucas", "MANAGER", userId, true))
                .thenReturn("renewed-token");
        when(clientIpResolver.resolve(any(HttpServletRequest.class))).thenReturn("127.0.0.1");

        mockMvc.perform(post("/terms/accept-biometric")
                .header("Authorization", "Bearer token")
                .header("User-Agent", "JUnit"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("KRONOS_ACCESS_TOKEN=renewed-token"),
                        org.hamcrest.Matchers.containsString("HttpOnly"),
                        org.hamcrest.Matchers.containsString("Secure"),
                        org.hamcrest.Matchers.containsString("SameSite=Lax")
                )));

        verify(acceptTermsUseCase).acceptBiometricTerms(employeeId, "127.0.0.1", "JUnit");
    }

    @Test
    void shouldReturnAcceptedStatus() throws Exception {
        UUID employeeId = UUID.randomUUID();

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(acceptTermsUseCase.hasAcceptedBiometricTerm(employeeId)).thenReturn(true);

        mockMvc.perform(get("/terms/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(true));
    }

    @Test
    void shouldRevokeBiometricTermsAndReturnRenewedToken() throws Exception {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(jwtAuthenticatedUser.getUsername()).thenReturn("lucas");
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(jwtUtils.generateToken(employeeId, "lucas", "MANAGER", userId, false))
                .thenReturn("revoked-token");
        when(clientIpResolver.resolve(any(HttpServletRequest.class))).thenReturn("127.0.0.1");

        mockMvc.perform(delete("/terms/revoke-biometric")
                .header("Authorization", "Bearer token")
                .header("User-Agent", "JUnit"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("KRONOS_ACCESS_TOKEN=revoked-token"),
                        org.hamcrest.Matchers.containsString("HttpOnly"),
                        org.hamcrest.Matchers.containsString("Secure"),
                        org.hamcrest.Matchers.containsString("SameSite=Lax")
                )));

        verify(acceptTermsUseCase).revokeBiometricTerms(employeeId, "127.0.0.1", "JUnit");
    }

    @Test
    void shouldTranslateExceptionWhenAcceptingBiometricTerms() throws Exception {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(jwtAuthenticatedUser.getUsername()).thenReturn("lucas");
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(clientIpResolver.resolve(any(HttpServletRequest.class))).thenReturn("127.0.0.1");
        doThrow(new BadRequestException("Termo já aceito"))
                .when(acceptTermsUseCase).acceptBiometricTerms(employeeId, "127.0.0.1", "JUnit");

        mockMvc.perform(post("/terms/accept-biometric")
                        .header("Authorization", "Bearer token")
                        .header("User-Agent", "JUnit"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Termo já aceito"));
    }
}
