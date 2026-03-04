package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.kts.kronos.constants.Messages.EMPLOYEE_NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TermsController.class)
@AutoConfigureMockMvc(addFilters = false) // Desabilita segurança (JWT Filters) para focar na lógica do Controller
class TermsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AcceptTermsUseCase acceptanceUseCase;

    @MockitoBean
    private JwtAuthenticatedUser jwtAuthenticatedUser;

    private static final String BASE_URL = "/terms";
    private static final UUID EMPLOYEE_ID = UUID.randomUUID();

    @BeforeEach
    void setup() {
        // Simula comportamento padrão: O usuário está autenticado
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(EMPLOYEE_ID);
    }

    // ==================================================================================
    // 1. ACEITE DE TERMOS (POST /terms/accept-biometric)
    // ==================================================================================

    @Test
    @DisplayName("Deve registrar aceite dos termos com sucesso (200 OK) pegando IP real")
    void shouldAcceptTermsSuccessfullyWithRemoteAddr() throws Exception {
        String userAgent = "Mozilla/5.0 Test";

        // Configura o Mock para não fazer nada (void) quando chamado
        doNothing().when(acceptanceUseCase).acceptBiometricTerms(any(), any(), any());

        mockMvc.perform(post(BASE_URL + "/accept-biometric")
                        .header("User-Agent", userAgent)
                        // Sem header X-Forwarded-For, o controller deve usar o request.getRemoteAddr()
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Verifica se o serviço foi chamado com o ID correto e o UserAgent passado
        verify(acceptanceUseCase).acceptBiometricTerms(eq(EMPLOYEE_ID), any(), eq(userAgent));
    }

    @Test
    @DisplayName("Deve extrair IP corretamente do header X-Forwarded-For (Proxy)")
    void shouldExtractIpFromForwardedHeader() throws Exception {
        String proxyHeader = "203.0.113.195, 10.0.0.1";
        String expectedIp = "203.0.113.195"; // A lógica pega o primeiro IP antes da vírgula

        mockMvc.perform(post(BASE_URL + "/accept-biometric")
                        .header("X-Forwarded-For", proxyHeader)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Verifica se o controller limpou a string e passou apenas o primeiro IP
        verify(acceptanceUseCase).acceptBiometricTerms(eq(EMPLOYEE_ID), eq(expectedIp), anyString());
    }

    @Test
    @DisplayName("Deve usar valor padrão 'Desconhecido' se User-Agent não for enviado")
    void shouldUseDefaultUserAgentWhenMissing() throws Exception {
        mockMvc.perform(post(BASE_URL + "/accept-biometric")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(acceptanceUseCase).acceptBiometricTerms(eq(EMPLOYEE_ID), any(), eq("Desconhecido"));
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found se o funcionário não for encontrado no serviço")
    void shouldReturn404WhenEmployeeNotFoundOnAccept() throws Exception {
        // Simula erro de negócio: O ID do token não existe no banco de dados
        doThrow(new ResourceNotFoundException(EMPLOYEE_NOT_FOUND))
                .when(acceptanceUseCase).acceptBiometricTerms(any(), any(), any());

        mockMvc.perform(post(BASE_URL + "/accept-biometric"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(EMPLOYEE_NOT_FOUND));
    }

    // ==================================================================================
    // 2. VERIFICAR STATUS (GET /terms/status)
    // ==================================================================================

    @Test
    @DisplayName("Deve retornar TRUE se o usuário já aceitou os termos")
    void shouldReturnTrueWhenTermsAccepted() throws Exception {
        when(acceptanceUseCase.hasAcceptedBiometricTerm(EMPLOYEE_ID)).thenReturn(true);

        mockMvc.perform(get(BASE_URL + "/status"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("Deve retornar FALSE se o usuário ainda não aceitou os termos")
    void shouldReturnFalseWhenTermsNotAccepted() throws Exception {
        when(acceptanceUseCase.hasAcceptedBiometricTerm(EMPLOYEE_ID)).thenReturn(false);

        mockMvc.perform(get(BASE_URL + "/status"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }
}