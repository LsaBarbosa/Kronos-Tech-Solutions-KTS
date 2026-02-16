package com.kts.kronos.adapter.in.web.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kts.kronos.adapter.in.web.dto.employee.RecoverPasswordRequest;
import com.kts.kronos.adapter.in.web.dto.security.FaceLoginRequest;
import com.kts.kronos.adapter.in.web.dto.security.LoginRequest;
import com.kts.kronos.adapter.in.web.dto.security.ResetPasswordRequest;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.AuthUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.kts.kronos.application.service.AuthService.INACTIVE_USER;
import static com.kts.kronos.application.service.AuthService.NO_USER_LINKED_TO_THIS_EMPLOYEE;
import static com.kts.kronos.constants.Messages.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
// 'addFilters = false' desabilita o Spring Security para focar apenas na lógica do Controller
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Mockamos o UseCase, pois o Controller delega a regra de negócio para ele
    @MockitoBean
    private AuthUseCase authUseCase;

    // Constantes para as rotas baseadas na classe AuthController e ApiPaths
    private static final String BASE_URL = "/auth";

    @Test
    @DisplayName("Deve retornar 200 OK e Token JWT ao realizar login com sucesso")
    void shouldReturnTokenWhenLoginIsSuccessful() throws Exception {
        // Cenário (Arrange)
        LoginRequest request = new LoginRequest("usuario.teste", "Senha123!");
        String expectedToken = "eyJhbGciOiJIUzI1NiJ9.token-mock-sucesso";

        // Simulamos que o serviço retorna o token corretamente
        when(authUseCase.login(request.username(), request.password()))
                .thenReturn(expectedToken);

        // Execução e Verificação (Act & Assert)
        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(expectedToken));
    }

    @Test
    @DisplayName("Deve retornar 204 No Content ao solicitar recuperação de senha com sucesso")
    void shouldReturnNoContentWhenRecoverPasswordIsSuccessful() throws Exception {
        // Cenário (Arrange)
        RecoverPasswordRequest request = new RecoverPasswordRequest("12345678901", "email@teste.com");

        // Simulamos que o método void execute sem lançar exceções
        doNothing().when(authUseCase).recoverPassword(any(RecoverPasswordRequest.class), any());

        // Execução e Verificação (Act & Assert)
        mockMvc.perform(post(BASE_URL + "/recover-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Origin", "http://localhost:3000") // Simulando header opcional
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(authUseCase).recoverPassword(any(RecoverPasswordRequest.class), eq("http://localhost:3000"));
    }

    @Test
    @DisplayName("Deve retornar 200 OK e Token ao realizar login facial com sucesso")
    void shouldReturnTokenWhenFaceLoginIsSuccessful() throws Exception {
        // Cenário (Arrange)
        String fakeBase64Image = "data:image/jpeg;base64,/9j/4AAQSkZJRg...";
        FaceLoginRequest request = new FaceLoginRequest(fakeBase64Image);
        String expectedToken = "eyJhbGciOiJIUzI1NiJ9.face-token-mock";

        when(authUseCase.loginFace(request.faceImageBase64()))
                .thenReturn(expectedToken);

        // Execução e Verificação (Act & Assert)
        mockMvc.perform(post(BASE_URL + "/login-face")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(expectedToken));
    }

    @Test
    @DisplayName("Deve retornar 204 No Content ao redefinir a senha com sucesso")
    void shouldReturnNoContentWhenResetPasswordIsSuccessful() throws Exception {
        // Cenário (Arrange)
        ResetPasswordRequest request = new ResetPasswordRequest(
                "token-uuid-valido",
                "NovaSenha123!",
                "NovaSenha123!"
        );

        doNothing().when(authUseCase).resetPassword(any(ResetPasswordRequest.class));

        // Execução e Verificação (Act & Assert)
        mockMvc.perform(post(BASE_URL + "/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(authUseCase).resetPassword(any(ResetPasswordRequest.class));
    }

    @Test
    @DisplayName("Deve retornar 401 Unauthorized quando as credenciais forem inválidas")
    void shouldReturn401WhenCredentialsAreInvalid() throws Exception {
        LoginRequest request = new LoginRequest("usuario.errado", "senha123");

        // Simula o erro que o Spring Security/AuthService lança quando falha
        when(authUseCase.login(request.username(), request.password()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized()) // Valida HTTP 401
                .andExpect(jsonPath("$.title").value("Unauthorized")); // Valida estrutura do ProblemDetail
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found se o usuário não existir no login")
    void shouldReturn404WhenUserNotFound() throws Exception {
        LoginRequest request = new LoginRequest("inexistente", "senha");

        when(authUseCase.login(request.username(), request.password()))
                .thenThrow(new ResourceNotFoundException(USER_NOT_FOUND));

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound()) // Valida HTTP 404
                .andExpect(jsonPath("$.detail").value(USER_NOT_FOUND));
    }

    // --- CENÁRIOS DE LOGIN FACIAL ---

    @Test
    @DisplayName("Deve retornar 403 Forbidden quando a face não for reconhecida")
    void shouldReturn403WhenFaceNotRecognized() throws Exception {
        FaceLoginRequest request = new FaceLoginRequest("base64-rosto-desconhecido");

        // Simula erro de biometria (ForbiddenException no AuthService)
        when(authUseCase.loginFace(request.faceImageBase64()))
                .thenThrow(new ForbiddenException(FACE_NOT_RECOGNIZED));

        mockMvc.perform(post(BASE_URL + "/login-face")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden()) // Valida HTTP 403
                .andExpect(jsonPath("$.detail").value("Sem permissão para utilizar esse recurso"));
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando a imagem Base64 for inválida")
    void shouldReturn400WhenImageIsInvalid() throws Exception {
        FaceLoginRequest request = new FaceLoginRequest("string-quebrada");

        when(authUseCase.loginFace(request.faceImageBase64()))
                .thenThrow(new BadRequestException(INVALID_BASE64_IMAGE));

        mockMvc.perform(post(BASE_URL + "/login-face")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()) // Valida HTTP 400
                .andExpect(jsonPath("$.detail").value(INVALID_BASE64_IMAGE));
    }

    // --- CENÁRIOS DE REDEFINIÇÃO DE SENHA ---

    @Test
    @DisplayName("Deve retornar 404 Not Found quando o token de reset for inválido ou expirado")
    void shouldReturn404WhenResetTokenIsInvalid() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("token-invalido", "Senha123!", "Senha123!");

        doThrow(new ResourceNotFoundException(INVALID_PASSWORD_RESET_TOKEN))
                .when(authUseCase).resetPassword(any(ResetPasswordRequest.class));

        mockMvc.perform(post(BASE_URL + "/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound()) // Valida HTTP 404
                .andExpect(jsonPath("$.detail").value(INVALID_PASSWORD_RESET_TOKEN));
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando as senhas não conferem")
    void shouldReturn400WhenPasswordsDoNotMatch() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("token-valido", "Senha123!", "OutraSenha!");

        doThrow(new BadRequestException(INVALID_CONFIRM_PASSWORD))
                .when(authUseCase).resetPassword(any(ResetPasswordRequest.class));

        mockMvc.perform(post(BASE_URL + "/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()) // Valida HTTP 400
                .andExpect(jsonPath("$.detail").value(INVALID_CONFIRM_PASSWORD));
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o payload for inválido (Bean Validation)")
    void shouldReturn400WhenValidationFails() throws Exception {
        // Request com campos em branco que deveriam ser @NotBlank
        LoginRequest invalidRequest = new LoginRequest("", "");

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest()); // O próprio Spring valida antes de chegar no UseCase
    }

    @Test
    @DisplayName("Deve aceitar solicitação de recuperação de senha SEM o header 'Origin'")
    void shouldAcceptRecoverPasswordWithoutOriginHeader() throws Exception {
        // O header Origin é marcado como required=false no Controller.
        // Este teste garante que o Spring não rejeita a requisição se ele faltar.

        RecoverPasswordRequest request = new RecoverPasswordRequest("12345678901", "email@teste.com");

        doNothing().when(authUseCase).recoverPassword(any(RecoverPasswordRequest.class), eq(null));

        mockMvc.perform(post(BASE_URL + "/recover-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent()); // 204

        // Verifica se o UseCase foi chamado passando 'null' no segundo argumento
        verify(authUseCase).recoverPassword(any(RecoverPasswordRequest.class), eq(null));
    }

    // --- TESTES DE REGRAS DE NEGÓCIO ESPECÍFICAS (LOGIN FACIAL) ---

    @Test
    @DisplayName("Deve retornar 404 quando a face é reconhecida mas não há usuário vinculado")
    void shouldReturn404WhenFaceRecognizedButNoUserLinked() throws Exception {
        // Caso: O funcionário existe no Rekognition, mas não tem login criado na tabela User.
        FaceLoginRequest request = new FaceLoginRequest("imagem-valida-base64");

        when(authUseCase.loginFace(request.faceImageBase64()))
                .thenThrow(new ResourceNotFoundException(NO_USER_LINKED_TO_THIS_EMPLOYEE));

        mockMvc.perform(post(BASE_URL + "/login-face")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(NO_USER_LINKED_TO_THIS_EMPLOYEE));
    }

    @Test
    @DisplayName("Deve retornar 400 quando o usuário vinculado à face está INATIVO")
    void shouldReturn400WhenUserIsInactiveInFaceLogin() throws Exception {
        // Caso: Face ok, User existe, mas active = false.
        FaceLoginRequest request = new FaceLoginRequest("imagem-valida-base64");

        when(authUseCase.loginFace(request.faceImageBase64()))
                .thenThrow(new BadRequestException(INACTIVE_USER));

        mockMvc.perform(post(BASE_URL + "/login-face")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(INACTIVE_USER));
    }
}