package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.MessageRepository;
import com.kts.kronos.adapter.out.persistence.entity.MessageEntity;
import com.kts.kronos.domain.model.Message;
import com.kts.kronos.domain.model.enuns.MessagePriority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageProviderImplTest {

    @InjectMocks
    private MessageProviderImpl provider;

    @Mock
    private MessageRepository repository;

    @Test
    @DisplayName("save: deve converter domínio para entity")
    void shouldSaveMessage() {
        Message message = buildMessage();

        provider.save(message);

        ArgumentCaptor<MessageEntity> captor = ArgumentCaptor.forClass(MessageEntity.class);
        verify(repository).save(captor.capture());

        MessageEntity entity = captor.getValue();
        assertEquals(message.messageId(), entity.getMessageId());
        assertEquals(message.title(), entity.getTitle());
        assertEquals(message.messageText(), entity.getMessageText());
    }

    @Test
    @DisplayName("findVisibleMessagesByCompanyIdAndEmployeeId: pageable deve mapear retorno")
    void shouldFindVisibleMessagesWithPageable() {
        Message message = buildMessage();
        UUID companyId = message.companyId();
        UUID employeeId = message.employeeId();
        PageRequest pageRequest = PageRequest.of(0, 10);

        when(repository.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, employeeId, pageRequest))
                .thenReturn(new PageImpl<>(List.of(MessageEntity.fromDomain(message))));

        List<Message> result = provider.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, employeeId, pageRequest);

        assertEquals(1, result.size());
        assertEquals(message.messageId(), result.getFirst().messageId());
    }

    @Test
    @DisplayName("deleteByCreationDateBefore: deve delegar limpeza")
    void shouldDeleteByCreationDateBefore() {
        LocalDateTime threshold = LocalDateTime.of(2026, 4, 1, 0, 0);

        provider.deleteByCreationDateBefore(threshold);

        verify(repository).deleteByCreatedAtBefore(threshold);
    }

    private Message buildMessage() {
        return new Message(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Comunicado",
                "Aviso importante",
                MessagePriority.NORMAL,
                LocalDateTime.of(2026, 4, 17, 10, 0),
                UUID.randomUUID()
        );
    }
}