package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.retention.RetentionExecutionSummaryResponse;
import com.kts.kronos.application.port.out.provider.RetentionExecutionLogProvider;
import com.kts.kronos.domain.model.RetentionExecutionLog;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LgpdRetentionControllerTest {

    @Mock
    private RetentionExecutionLogProvider retentionExecutionLogProvider;

    @InjectMocks
    private LgpdRetentionController controller;

    @Test
    void testListRetentionExecutions() {
        var log = createRetentionExecutionLog();
        Page<RetentionExecutionLog> page = new PageImpl<>(List.of(log));

        when(retentionExecutionLogProvider.findAll(PageRequest.of(0, 20)))
                .thenReturn(page);

        var pageable = PageRequest.of(0, 20);
        ResponseEntity<Page<RetentionExecutionSummaryResponse>> response = controller.listRetentionExecutions(pageable);

        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        assertTrue(response.getBody().getContent().stream()
                .anyMatch(e -> "RET_TEST".equals(e.policyCode())));
    }

    @Test
    void testGetRetentionExecutionById() {
        var executionId = UUID.randomUUID();
        var log = createRetentionExecutionLog();

        when(retentionExecutionLogProvider.findById(executionId))
                .thenReturn(Optional.of(log));

        ResponseEntity<RetentionExecutionSummaryResponse> response = controller.getRetentionExecution(executionId);

        assertNotNull(response.getBody());
        assertEquals("RET_TEST", response.getBody().policyCode());
        assertEquals("SUCCESS", response.getBody().status());
    }

    @Test
    void testGetRetentionExecutionByIdNotFound() {
        var executionId = UUID.randomUUID();

        when(retentionExecutionLogProvider.findById(executionId))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> controller.getRetentionExecution(executionId));
    }

    private RetentionExecutionLog createRetentionExecutionLog() {
        return new RetentionExecutionLog(
                UUID.randomUUID(),
                "RET_TEST",
                RetentionResourceType.BLACKLISTED_TOKEN,
                "DRY_RUN",
                Instant.now(),
                Instant.now(),
                "SUCCESS",
                10L,
                5L,
                0L,
                0L,
                null
        );
    }
}
