package com.kts.kronos.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kts.kronos.adapter.in.web.dto.security.LoginRequest;
import com.kts.kronos.adapter.in.web.exceptions.DelegatedAuthenticationEntryPoint;
import com.kts.kronos.adapter.in.web.exceptions.RestExceptionHandler;
import com.kts.kronos.adapter.in.web.http.AuthController;
import com.kts.kronos.adapter.out.security.CustomUserDetailsService;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.port.in.usecase.AuthUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = AuthFlowIntegrationTest.TestApplication.class,
        properties = {
                "frontend.base-url-record=http://localhost:3001",
                "frontend.base-url-plataform=http://localhost:3000",
                "frontend.base-url-local=http://localhost:5173",
                "frontend.base-url-local-2=http://127.0.0.1:5173",
                "jwt.secret=aW50ZWdyYXRpb24tdGVzdC1zZWNyZXQtaW50ZWdyYXRpb24tdGVzdC1zZWNyZXQ=",
                "jwt.expiration=3600000",
                "jwt.issuer=kronos-api",
                "jwt.audience=kronos-clients",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration"
        }
)
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    @EnableAutoConfiguration
    @Import({
            AuthController.class,
            RestExceptionHandler.class,
            DelegatedAuthenticationEntryPoint.class,
            com.kts.kronos.config.SecurityConfig.class
    })
    static class TestApplication {
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthUseCase authUseCase;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @Test
    @DisplayName("Deve retornar token no login com filtros de segurança habilitados")
    void shouldLoginSuccessfullyWithSecurityFiltersEnabled() throws Exception {
        LoginRequest request = new LoginRequest("usuario.teste", "Senha123!");
        when(authUseCase.login(request.username(), request.password())).thenReturn("jwt-token-mock");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-mock"));
    }

    @Test
    @DisplayName("Deve retornar 401 no login quando credenciais forem inválidas")
    void shouldReturnUnauthorizedWhenCredentialsAreInvalid() throws Exception {
        LoginRequest request = new LoginRequest("usuario.errado", "senha-invalida");
        when(authUseCase.login(request.username(), request.password()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }
}
