package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.exceptions.RestExceptionHandler;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.annotation.Resource;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TermsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RestExceptionHandler.class)
class TermsControllerWebMvcTest {

    @Resource
    private MockMvc mockMvc;

    @MockitoBean
    private AcceptTermsUseCase acceptTermsUseCase;

    @MockitoBean
    private JwtAuthenticatedUser jwtAuthenticatedUser;

    @MockitoBean
    private JwtUtils jwtUtils;

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

        mockMvc.perform(post("/terms/accept-biometric")
                        .header("Authorization", "Bearer token")
                        .header("User-Agent", "JUnit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("renewed-token"));

        verify(acceptTermsUseCase).acceptBiometricTerms(employeeId, "127.0.0.1", "JUnit");
    }

    @Test
    void shouldReturnAcceptedStatus() throws Exception {
        UUID employeeId = UUID.randomUUID();

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(acceptTermsUseCase.hasAcceptedBiometricTerm(employeeId)).thenReturn(true);

        mockMvc.perform(get("/terms/status"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
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

        mockMvc.perform(delete("/terms/revoke-biometric")
                        .header("Authorization", "Bearer token")
                        .header("User-Agent", "JUnit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("revoked-token"));

        verify(acceptTermsUseCase).revokeBiometricTerms(employeeId, "127.0.0.1", "JUnit");
    }
}
