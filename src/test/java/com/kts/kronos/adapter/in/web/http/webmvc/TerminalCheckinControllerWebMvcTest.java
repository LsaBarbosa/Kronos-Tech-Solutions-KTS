package com.kts.kronos.adapter.in.web.http.webmvc;

import com.kts.kronos.adapter.in.web.dto.terminal.TerminalCheckinResponse;
import com.kts.kronos.adapter.in.web.http.TerminalCheckinController;
import com.kts.kronos.adapter.out.security.AuthCookieService;
import com.kts.kronos.application.port.in.usecase.TerminalCheckinUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TerminalCheckinController.class)
@AutoConfigureMockMvc(addFilters = false)
class TerminalCheckinControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TerminalCheckinUseCase terminalCheckinUseCase;

    @MockitoBean
    private AuthCookieService authCookieService;

    @Test
    @DisplayName("checkinByFace: retorna 200 com cookie de sessão e resposta de checkin")
    void shouldCheckinByFaceAndReturnOkWithCookie() throws Exception {
        var checkinResponse = new TerminalCheckinResponse("CHECKIN", "Checkin realizado com sucesso");
        var result = new TerminalCheckinUseCase.TerminalCheckinResult("jwt-token-value", checkinResponse);

        when(terminalCheckinUseCase.checkinByFace(any())).thenReturn(result);
        when(authCookieService.createAccessTokenCookie("jwt-token-value"))
                .thenReturn(ResponseCookie.from("KRONOS_ACCESS_TOKEN", "jwt-token-value")
                        .maxAge(Duration.ofHours(8))
                        .path("/")
                        .httpOnly(true)
                        .secure(true)
                        .sameSite("Lax")
                        .build());

        mockMvc.perform(post("/auth/terminal-checkin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"faceImageBase64\":\"validBase64\",\"livenessPassed\":true,\"latitude\":0.0,\"longitude\":0.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actionType").value("CHECKIN"))
                .andExpect(jsonPath("$.message").value("Checkin realizado com sucesso"));
    }
}
