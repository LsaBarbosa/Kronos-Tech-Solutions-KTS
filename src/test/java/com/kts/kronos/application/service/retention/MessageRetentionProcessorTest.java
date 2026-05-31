package com.kts.kronos.application.service.retention;

import com.kts.kronos.adapter.out.persistence.MessageRepository;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.enuns.RetentionExecutionMode;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageRetentionProcessorTest {

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private MessageRetentionProcessor processor;

    @Test
    void testSupports() {
        assertEquals(RetentionResourceType.MESSAGE, processor.supports());
    }

    @Test
    void testExecuteDryRunWithNoExpiredMessages() {
        when(messageRepository.countExpiredAndRemovable(any())).thenReturn(0L);
        when(messageRepository.countPreservedMessages(any())).thenReturn(0L);

        var policy = createPolicy();
        var result = processor.execute(policy, "DRY_RUN");

        assertEquals("DRY_RUN", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(0, result.scannedCount());
        assertEquals(0, result.affectedCount());
        assertEquals(0, result.skippedCount());
    }

    @Test
    void testExecuteDryRunWithOnlyRemovableMessages() {
        when(messageRepository.countExpiredAndRemovable(any())).thenReturn(15L);
        when(messageRepository.countPreservedMessages(any())).thenReturn(0L);

        var policy = createPolicy();
        var result = processor.execute(policy, "DRY_RUN");

        assertEquals("DRY_RUN", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(15, result.scannedCount());
        assertEquals(0, result.affectedCount());
        assertEquals(0, result.skippedCount());
    }

    @Test
    void testExecuteDryRunWithRemovableAndPreservedMessages() {
        when(messageRepository.countExpiredAndRemovable(any())).thenReturn(15L);
        when(messageRepository.countPreservedMessages(any())).thenReturn(5L);

        var policy = createPolicy();
        var result = processor.execute(policy, "DRY_RUN");

        assertEquals("DRY_RUN", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(20, result.scannedCount());
        assertEquals(0, result.affectedCount());
        assertEquals(5, result.skippedCount());
    }

    @Test
    void testExecuteDryRunWithOnlyPreservedMessages() {
        when(messageRepository.countExpiredAndRemovable(any())).thenReturn(0L);
        when(messageRepository.countPreservedMessages(any())).thenReturn(5L);

        var policy = createPolicy();
        var result = processor.execute(policy, "DRY_RUN");

        assertEquals("DRY_RUN", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(5, result.scannedCount());
        assertEquals(0, result.affectedCount());
        assertEquals(5, result.skippedCount());
    }

    @Test
    void testExecuteApplySoftDeletesExpiredMessages() {
        when(messageRepository.softDeleteExpiredMessages(any(), any(), anyString())).thenReturn(15);
        when(messageRepository.countPreservedMessages(any())).thenReturn(5L);

        var policy = createPolicy();
        var result = processor.execute(policy, "APPLY");

        assertEquals("APPLY", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(20, result.scannedCount());
        assertEquals(15, result.affectedCount());
        assertEquals(5, result.skippedCount());

        verify(messageRepository).softDeleteExpiredMessages(any(LocalDateTime.class), any(LocalDateTime.class), anyString());
    }

    @Test
    void testExecuteApplyNoMessagesSoftDeleted() {
        when(messageRepository.softDeleteExpiredMessages(any(), any(), anyString())).thenReturn(0);
        when(messageRepository.countPreservedMessages(any())).thenReturn(0L);

        var policy = createPolicy();
        var result = processor.execute(policy, "APPLY");

        assertEquals("APPLY", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(0, result.scannedCount());
        assertEquals(0, result.affectedCount());
        assertEquals(0, result.skippedCount());
    }

    @Test
    void testExecuteApplyPreservesHighPriorityMessages() {
        when(messageRepository.softDeleteExpiredMessages(any(), any(), anyString())).thenReturn(10);
        when(messageRepository.countPreservedMessages(any())).thenReturn(8L);

        var policy = createPolicy();
        var result = processor.execute(policy, "APPLY");

        assertEquals("APPLY", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(18, result.scannedCount());
        assertEquals(10, result.affectedCount());
        assertEquals(8, result.skippedCount());
    }

    @Test
    void testExecuteHandlesException() {
        when(messageRepository.countExpiredAndRemovable(any()))
                .thenThrow(new RuntimeException("Database connection error"));

        var policy = createPolicy();
        var result = processor.execute(policy, "DRY_RUN");

        assertEquals("DRY_RUN", result.executionMode());
        assertEquals("ERROR", result.status());
        assertEquals(1, result.errorCount());
        assertNotNull(result.notes());
        assertTrue(result.notes().contains("Database connection error"));
    }

    @Test
    void testExecuteApplyHandlesException() {
        when(messageRepository.softDeleteExpiredMessages(any(), any(), anyString()))
                .thenThrow(new RuntimeException("Update failed"));

        var policy = createPolicy();
        var result = processor.execute(policy, "APPLY");

        assertEquals("APPLY", result.executionMode());
        assertEquals("ERROR", result.status());
        assertEquals(1, result.errorCount());
        assertTrue(result.notes().contains("Update failed"));
    }

    @Test
    void testSupportsApply() {
        assertTrue(processor.supportsApply());
    }

    private RetentionPolicy createPolicy() {
        return new RetentionPolicy(
                UUID.randomUUID(),
                "TEST_MESSAGE_RETENTION",
                "Test policy for messages",
                "MESSAGE",
                30,
                RetentionExecutionMode.DRY_RUN,
                true,
                false,
                false,
                null,
                Instant.now(),
                null
        );
    }
}
