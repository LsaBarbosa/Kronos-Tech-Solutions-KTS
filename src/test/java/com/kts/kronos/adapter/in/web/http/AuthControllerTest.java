package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.employee.RecoverPasswordRequest;
import com.kts.kronos.adapter.out.security.AuthCookieService;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.port.in.usecase.AuthUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthUseCase authUseCase;
    @Mock
    private JwtUtils jwtUtils;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new AuthController(authUseCase, authCookieService(), jwtUtils);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void shouldHandleRecoverPasswordWithoutDependingOnOriginHeader() throws Exception {
        mockMvc.perform(post("/auth/recover-password")
                        .header("Origin", "https://attacker.example")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cpf": "12345678901",
                                  "email": "user@kts.com"
                                }
                                """))
                .andExpect(status().isNoContent());

        var requestCaptor = ArgumentCaptor.forClass(RecoverPasswordRequest.class);
        verify(authUseCase).recoverPassword(requestCaptor.capture());
        assertEquals("12345678901", requestCaptor.getValue().cpf());
        assertEquals("user@kts.com", requestCaptor.getValue().email());
        verifyNoMoreInteractions(authUseCase);
    }

    private AuthCookieService authCookieService() {
        return new AuthCookieService("KRONOS_ACCESS_TOKEN", true, "Lax", "/", "", 900);
    }

    // L95: switchCompany — extractToken returns empty → orElseThrow fires IllegalArgumentException
    @Test
    void switchCompany_noAuthCookie_throwsIllegalArgument() {
        UUID companyId = UUID.randomUUID();
        // No cookie set → extractToken returns empty → MockMvc wraps in ServletException
        assertThatThrownBy(() ->
            mockMvc.perform(post("/auth/switch-company")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"companyId\":\"" + companyId + "\"}")))
            .hasCauseInstanceOf(IllegalArgumentException.class);
    }
}
