package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.demo.*;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.service.demo.DemoSandboxService;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CtoDemoControllerCoverageTest {

    @InjectMocks private CtoDemoController controller;
    @Mock private DemoSandboxService demoService;
    @Mock private JwtAuthenticatedUser currentUser;

    @Test
    void createDemo_delegatesToService_returnsOk() {
        UUID actorId = UUID.randomUUID();
        var counters = DemoOperationCounters.zero();
        var validation = DemoValidationResult.noResidues();
        var response = new DemoCreateResponse(UUID.randomUUID(), "SUCCESS", "KTS Demo", "admin@kts.com", true, counters, validation);
        when(currentUser.getuserId()).thenReturn(actorId);
        when(currentUser.getCurrentRole()).thenReturn(Role.CTO);
        when(demoService.create(actorId, "CTO")).thenReturn(response);

        ResponseEntity<DemoCreateResponse> result = controller.createDemo();

        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
        assertEquals(response, result.getBody());
        verify(demoService).create(actorId, "CTO");
    }

    @Test
    void deleteDemo_delegatesToService_returnsOk() {
        UUID actorId = UUID.randomUUID();
        var counters = DemoOperationCounters.zero();
        var validation = DemoValidationResult.noResidues();
        var response = new DemoPurgeResponse(UUID.randomUUID(), "SUCCESS", counters, validation);
        when(currentUser.getuserId()).thenReturn(actorId);
        when(currentUser.getCurrentRole()).thenReturn(Role.CTO);
        when(demoService.purge(actorId, "CTO")).thenReturn(response);

        ResponseEntity<DemoPurgeResponse> result = controller.deleteDemo();

        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
        verify(demoService).purge(actorId, "CTO");
    }

    @Test
    void demoStatus_delegatesToService_returnsOk() {
        var validation = DemoValidationResult.noResidues();
        var statusResp = new DemoStatusResponse(true, false, true, "KTS Demo", "admin", "sk-1",
                new DemoStatusResponse.LastOperation("CREATE", "SUCCESS", java.time.LocalDateTime.now()), validation);
        when(demoService.status()).thenReturn(statusResp);

        ResponseEntity<DemoStatusResponse> result = controller.demoStatus();

        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
        verify(demoService).status();
    }

    @Test
    void validateDemo_delegatesToService_returnsOk() {
        var validation = DemoValidationResult.noResidues();
        when(demoService.validate()).thenReturn(validation);

        ResponseEntity<DemoValidationResult> result = controller.validateDemo();

        assertNotNull(result);
        assertTrue(result.getBody().clean());
        verify(demoService).validate();
    }
}
