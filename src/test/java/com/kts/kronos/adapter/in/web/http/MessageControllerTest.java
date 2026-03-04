package com.kts.kronos.adapter.in.web.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kts.kronos.adapter.in.web.dto.message.CreateMessageRequest;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.InternalServerException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.MessageUseCase;
import com.kts.kronos.domain.model.Message;
import com.kts.kronos.domain.model.enuns.MessagePriority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MessageController.class)
@AutoConfigureMockMvc(addFilters = false) // Desativa Spring Security para focar na lógica do Controller
class MessageControllerTest {

    private static final String GENERIC_INTERNAL_ERROR_MESSAGE = "Erro interno inesperado. Tente novamente mais tarde.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MessageUseCase messageUseCase;

    private static final String BASE_URL = "/messages";
    private static final UUID MSG_ID = UUID.randomUUID();
    private static final UUID SENDER_ID = UUID.randomUUID();
    private static final UUID RECIPIENT_ID = UUID.randomUUID();
    private static final UUID COMPANY_ID = UUID.randomUUID();

    // ==================================================================================
    // 1. PUBLICAR MENSAGEM (POST)
    // ==================================================================================

    @Test
    @DisplayName("Deve publicar mensagem com sucesso (200 OK)")
    void shouldPostMessageSuccessfully() throws Exception {
        CreateMessageRequest request = new CreateMessageRequest(
                "Conteúdo da mensagem",
                "Título Importante",
                MessagePriority.NORMAL,
                List.of(RECIPIENT_ID)
        );

        doNothing().when(messageUseCase).postMessage(any(CreateMessageRequest.class));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(messageUseCase, times(1)).postMessage(any(CreateMessageRequest.class));
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request se o payload for inválido (Validação @NotBlank)")
    void shouldReturn400WhenPostMessageInvalidPayload() throws Exception {
        // Título e Texto vazios devem falhar na validação @Valid do Controller
        CreateMessageRequest invalidRequest = new CreateMessageRequest(
                "",
                "",
                MessagePriority.ALERT,
                List.of(RECIPIENT_ID)
        );

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        // O UseCase NÃO deve ser chamado se a validação falhar
        verify(messageUseCase, never()).postMessage(any());
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request se houver erro de negócio (Ex: Lista de destinatários vazia)")
    void shouldReturn400WhenBusinessErrorOnPostMessage() throws Exception {
        CreateMessageRequest request = new CreateMessageRequest(
                "Texto", "Título", MessagePriority.CRITICAL, null
        );

        // Simula o erro lançado pelo Service (MessageService:40)
        doThrow(new BadRequestException(CHOOSE_EMPLOYEE))
                .when(messageUseCase).postMessage(any(CreateMessageRequest.class));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(CHOOSE_EMPLOYEE));
    }

    @Test
    @DisplayName("Deve retornar 500 Internal Server Error ao publicar mensagem com falha inesperada")
    void shouldReturn500WhenUnexpectedErrorOnPostMessage() throws Exception {
        CreateMessageRequest request = new CreateMessageRequest(
                "Texto", "Título", MessagePriority.NORMAL, List.of(RECIPIENT_ID)
        );

        doThrow(new InternalServerException("erro"))
                .when(messageUseCase).postMessage(any(CreateMessageRequest.class));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value(GENERIC_INTERNAL_ERROR_MESSAGE));
    }

    // ==================================================================================
    // 2. LISTAR MENSAGENS (GET)
    // ==================================================================================

    @Test
    @DisplayName("Deve listar mensagens do usuário/empresa com sucesso (200 OK)")
    void shouldListMessagesSuccessfully() throws Exception {
        // Cria objeto de domínio
        Message messageDomain = new Message(
                MSG_ID, SENDER_ID, COMPANY_ID, "Título", "Texto",
                MessagePriority.NORMAL, LocalDateTime.now(), RECIPIENT_ID
        );

        when(messageUseCase.listMessagesForMyCompany()).thenReturn(List.of(messageDomain));

        mockMvc.perform(get(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].messageId").value(MSG_ID.toString()))
                .andExpect(jsonPath("$[0].title").value("Título"))
                .andExpect(jsonPath("$[0].senderEmployeeId").value(SENDER_ID.toString()));
    }

    @Test
    @DisplayName("Deve retornar lista vazia se não houver mensagens (200 OK)")
    void shouldReturnEmptyListWhenNoMessagesFound() throws Exception {
        when(messageUseCase.listMessagesForMyCompany()).thenReturn(Collections.emptyList());

        mockMvc.perform(get(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found se o funcionário não for encontrado no contexto")
    void shouldReturn404WhenEmployeeNotFoundOnList() throws Exception {
        // Simula erro ao buscar o funcionário logado no service
        when(messageUseCase.listMessagesForMyCompany())
                .thenThrow(new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(EMPLOYEE_NOT_FOUND));
    }

    @Test
    @DisplayName("Deve retornar 500 Internal Server Error ao listar mensagens com falha inesperada")
    void shouldReturn500WhenUnexpectedErrorOnListMessages() throws Exception {
        when(messageUseCase.listMessagesForMyCompany())
                .thenThrow(new InternalServerException("erro"));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value(GENERIC_INTERNAL_ERROR_MESSAGE));
    }

    // ==================================================================================
    // 3. DELETAR MENSAGEM (DELETE)
    // ==================================================================================

    @Test
    @DisplayName("Deve deletar mensagem com sucesso (200 OK)")
    void shouldDeleteMessageSuccessfully() throws Exception {
        doNothing().when(messageUseCase).deleteMessage(MSG_ID);

        mockMvc.perform(delete(BASE_URL + "/{messageId}", MSG_ID))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(messageUseCase).deleteMessage(MSG_ID);
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found se a mensagem não existir")
    void shouldReturn404WhenDeletingNonExistentMessage() throws Exception {
        doThrow(new ResourceNotFoundException(MESSAGE_NOT_FOUND))
                .when(messageUseCase).deleteMessage(MSG_ID);

        mockMvc.perform(delete(BASE_URL + "/{messageId}", MSG_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(MESSAGE_NOT_FOUND));
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request se tentar deletar mensagem de outro autor")
    void shouldReturn400WhenDeletingMessageOfAnotherAuthor() throws Exception {
        // No seu Service, a verificação de propriedade lança BadRequestException
        doThrow(new BadRequestException(ONLY_MANAGER_CAN_DELETE_MESSAGE))
                .when(messageUseCase).deleteMessage(MSG_ID);

        mockMvc.perform(delete(BASE_URL + "/{messageId}", MSG_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(ONLY_MANAGER_CAN_DELETE_MESSAGE));
    }

    @Test
    @DisplayName("Deve retornar 500 Internal Server Error ao deletar mensagem com falha inesperada")
    void shouldReturn500WhenUnexpectedErrorOnDeleteMessage() throws Exception {
        doThrow(new InternalServerException("erro"))
                .when(messageUseCase).deleteMessage(MSG_ID);

        mockMvc.perform(delete(BASE_URL + "/{messageId}", MSG_ID))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value(GENERIC_INTERNAL_ERROR_MESSAGE));
    }
}
