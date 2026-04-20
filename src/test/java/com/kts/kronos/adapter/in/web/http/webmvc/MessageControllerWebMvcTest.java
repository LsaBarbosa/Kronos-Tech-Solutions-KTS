package com.kts.kronos.adapter.in.web.http.webmvc;

import com.kts.kronos.adapter.in.web.http.MessageController;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.exceptions.BadRequestException;
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
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MessageController.class)
@AutoConfigureMockMvc(addFilters = false)
class MessageControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MessageUseCase useCase;

    @Test
    void shouldPostMessageDelegatingToUseCase() throws Exception {
        UUID recipientId = UUID.randomUUID();

        mockMvc.perform(post("/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "messageText": "Aviso importante",
                                  "title": "Comunicado",
                                  "priority": "NORMAL",
                                  "recipientEmployeeIds": ["%s"]
                                }
                                """.formatted(recipientId)))
                .andExpect(status().isOk());

        verify(useCase).postMessage(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldGetMessagesWithPaginationParams() throws Exception {
        UUID messageId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();

        Message message = new Message(
                messageId,
                senderId,
                companyId,
                "Comunicado",
                "Aviso importante",
                MessagePriority.CRITICAL,
                LocalDateTime.of(2026, 4, 10, 15, 30),
                recipientId
        );

        when(useCase.listMessagesForMyCompany(2, 15)).thenReturn(List.of(message));

        mockMvc.perform(get("/messages")
                        .param("page", "2")
                        .param("size", "15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].messageId").value(messageId.toString()))
                .andExpect(jsonPath("$[0].title").value("Comunicado"))
                .andExpect(jsonPath("$[0].messageText").value("Aviso importante"));

        verify(useCase).listMessagesForMyCompany(2, 15);
    }

    @Test
    void shouldDeleteMessageDelegatingToUseCase() throws Exception {
        UUID messageId = UUID.randomUUID();

        mockMvc.perform(delete("/messages/{messageId}", messageId))
                .andExpect(status().isOk());

        verify(useCase).deleteMessage(messageId);
    }

    @Test
    @DisplayName("postMessage: deve delegar criação de mensagem")
    void shouldPostMessage() throws Exception {
        UUID recipientEmployeeId = UUID.randomUUID();

        mockMvc.perform(post("/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "messageText": "Mensagem importante",
                                  "title": "Comunicado",
                                  "priority": "ALERT",
                                  "recipientEmployeeIds": ["%s"]
                                }
                                """.formatted(recipientEmployeeId)))
                .andExpect(status().isOk());

        verify(useCase).postMessage(any());
    }

    @Test
    @DisplayName("postMessage: deve retornar 400 para payload inválido")
    void shouldReturnBadRequestForInvalidCreatePayload() throws Exception {
        mockMvc.perform(post("/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "messageText": "",
                                  "title": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].name", hasItem("messageText")))
                .andExpect(jsonPath("$.errors[*].name", hasItem("title")))
                .andExpect(jsonPath("$.errors[*].name", hasItem("priority")));
    }

    @Test
    @DisplayName("postMessage: deve traduzir erro de regra")
    void shouldTranslateExceptionOnPost() throws Exception {
        doThrow(new BadRequestException("Destinatário inválido"))
                .when(useCase).postMessage(any());

        mockMvc.perform(post("/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "messageText": "Mensagem importante",
                                  "title": "Comunicado",
                                  "priority": "ALERT",
                                  "recipientEmployeeIds": ["%s"]
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Destinatário inválido"));
    }

    @Test
    @DisplayName("getMessages: deve listar mensagens")
    void shouldListMessages() throws Exception {
        UUID messageId = UUID.randomUUID();
        UUID senderEmployeeId = UUID.randomUUID();
        UUID recipientEmployeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        when(useCase.listMessagesForMyCompany(0, 10))
                .thenReturn(List.of(message(messageId, senderEmployeeId, recipientEmployeeId, companyId)));

        mockMvc.perform(get("/messages")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].messageId").value(messageId.toString()))
                .andExpect(jsonPath("$[0].title").value("Comunicado"))
                .andExpect(jsonPath("$[0].messageText").value("Mensagem importante"))
                .andExpect(jsonPath("$[0].priority").value("ALERT"))
                .andExpect(jsonPath("$[0].senderEmployeeId").value(senderEmployeeId.toString()))
                .andExpect(jsonPath("$[0].recipientEmployeeId").value(recipientEmployeeId.toString()));

        verify(useCase).listMessagesForMyCompany(0, 10);
    }

    @Test
    @DisplayName("deleteMessage: deve delegar exclusão")
    void shouldDeleteMessage() throws Exception {
        UUID messageId = UUID.randomUUID();

        mockMvc.perform(delete("/messages/{messageId}", messageId))
                .andExpect(status().isOk());

        verify(useCase).deleteMessage(messageId);
    }

    @Test
    @DisplayName("deleteMessage: deve traduzir exceção pelo handler")
    void shouldTranslateExceptionOnDelete() throws Exception {
        UUID messageId = UUID.randomUUID();

        doThrow(new ResourceNotFoundException("Mensagem não encontrada"))
                .when(useCase).deleteMessage(messageId);

        mockMvc.perform(delete("/messages/{messageId}", messageId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Mensagem não encontrada"));
    }

    private Message message(UUID messageId, UUID senderEmployeeId, UUID recipientEmployeeId, UUID companyId) {
        return new Message(
                messageId,
                senderEmployeeId,
                companyId,
                "Comunicado",
                "Mensagem importante",
                MessagePriority.ALERT,
                LocalDateTime.of(2026, 1, 15, 10, 30, 0),
                recipientEmployeeId
        );
    }
}
