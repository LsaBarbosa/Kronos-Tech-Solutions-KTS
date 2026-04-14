package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.application.port.in.usecase.MessageUseCase;
import com.kts.kronos.domain.model.Message;
import com.kts.kronos.domain.model.enuns.MessagePriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MessageControllerTest {

    @Mock
    private MessageUseCase useCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new MessageController(useCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

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
}
