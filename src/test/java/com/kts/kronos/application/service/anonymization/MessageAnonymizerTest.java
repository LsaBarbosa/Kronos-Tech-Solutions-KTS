package com.kts.kronos.application.service.anonymization;

import com.kts.kronos.application.security.PrivacyLogReferenceService;
import com.kts.kronos.adapter.out.persistence.MessageRepository;
import com.kts.kronos.adapter.out.persistence.entity.MessageEntity;
import com.kts.kronos.domain.model.AnonymizationPlan;
import com.kts.kronos.domain.model.enuns.AnonymizationResourceType;
import com.kts.kronos.domain.model.enuns.MessagePriority;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageAnonymizerTest {


    @Mock
    private PrivacyLogReferenceService privacyLogReferenceService;

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private MessageAnonymizer anonymizer;

    @Test
    void testSupports() {
        assertEquals(AnonymizationResourceType.MESSAGE, anonymizer.supports());
    }

    @Test
    void testExecuteDryRunWithNoMessages() {
        when(messageRepository.findVisibleMessagesByCompanyIdAndEmployeeId(any(), any())).thenReturn(new ArrayList<>());

        var plan = createPlan();
        var result = anonymizer.execute(plan, "DRY_RUN");

        assertEquals("DRY_RUN", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(0, result.scannedCount());
    }

    @Test
    void testExecuteDryRunWithMessages() {
        var messages = Arrays.asList(createMessage(), createMessage());
        when(messageRepository.findVisibleMessagesByCompanyIdAndEmployeeId(any(), any())).thenReturn(messages);

        var plan = createPlan();
        var result = anonymizer.execute(plan, "DRY_RUN");

        assertEquals("DRY_RUN", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(2, result.scannedCount());
        assertEquals(0, result.affectedCount());
    }

    @Test
    void testExecuteApplyAnonymizesMessages() {
        var msg1 = createMessage();
        var msg2 = createMessage();
        when(messageRepository.findVisibleMessagesByCompanyIdAndEmployeeId(any(), any())).thenReturn(Arrays.asList(msg1, msg2));

        var plan = createPlan();
        var result = anonymizer.execute(plan, "APPLY");

        assertEquals("APPLY", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(2, result.affectedCount());

        verify(messageRepository, times(2)).save(any());
    }

    @Test
    void testExecuteApplyAnonymizesTitleAndText() {
        var message = createMessage();
        when(messageRepository.findVisibleMessagesByCompanyIdAndEmployeeId(any(), any())).thenReturn(Arrays.asList(message));

        anonymizer.execute(createPlan(), "APPLY");

        var savedCaptor = org.mockito.ArgumentCaptor.forClass(MessageEntity.class);
        verify(messageRepository).save(savedCaptor.capture());

        var saved = savedCaptor.getValue();
        assertEquals("ANON", saved.getTitle());
        assertEquals("ANON", saved.getMessageText());
    }

    @Test
    void testExecuteApplyHandlesException() {
        when(messageRepository.findVisibleMessagesByCompanyIdAndEmployeeId(any(), any())).thenThrow(new RuntimeException("DB error"));

        var plan = createPlan();
        var result = anonymizer.execute(plan, "APPLY");

        assertEquals("APPLY", result.executionMode());
        assertEquals("ERROR", result.status());
        assertEquals(1, result.errorCount());
    }

    private AnonymizationPlan createPlan() {
        return new AnonymizationPlan(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Test anonymization",
                false,
                false,
                false,
                false,
                true,
                false
        );
    }

    private MessageEntity createMessage() {
        return MessageEntity.builder()
                .messageId(UUID.randomUUID())
                .employeeId(UUID.randomUUID())
                .companyId(UUID.randomUUID())
                .title("Test Message")
                .messageText("Test message content")
                .priority(MessagePriority.NORMAL)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
