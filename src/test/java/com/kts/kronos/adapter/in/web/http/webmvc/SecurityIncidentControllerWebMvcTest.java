package com.kts.kronos.adapter.in.web.http.webmvc;

import com.kts.kronos.adapter.in.web.dto.security.SecurityIncidentResponse;
import com.kts.kronos.adapter.in.web.http.SecurityIncidentController;
import com.kts.kronos.application.security.ClientIpResolver;
import com.kts.kronos.adapter.in.web.exceptions.RestExceptionHandler;
import com.kts.kronos.application.port.in.usecase.SecurityIncidentUseCase;
import com.kts.kronos.domain.model.enuns.SecurityIncidentSeverity;
import com.kts.kronos.domain.model.enuns.SecurityIncidentStatus;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SecurityIncidentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RestExceptionHandler.class)
class SecurityIncidentControllerWebMvcTest {

    @Resource
    private MockMvc mockMvc;

    @MockitoBean
    private SecurityIncidentUseCase securityIncidentUseCase;

    @MockitoBean
    private ClientIpResolver clientIpResolver;

    @BeforeEach
    void setUp() {
        when(clientIpResolver.resolve(any())).thenReturn("127.0.0.1");
    }

    @Test
    void shouldCreateIncidentSuccessfully() throws Exception {
        var incidentId = UUID.randomUUID();
        var response = buildResponse(incidentId);

        when(securityIncidentUseCase.createIncident(any(), anyString(), anyString())).thenReturn(response);

        mockMvc.perform(post("/security-incidents")
                .contentType(MediaType.APPLICATION_JSON)
                .header("User-Agent", "JUnit")
                .content("""
                        {
                            "title": "Test Incident",
                            "description": "Test description",
                            "severity": "HIGH",
                            "personalDataInvolved": true,
                            "sensitiveDataInvolved": false,
                            "affectedSubjectsEstimate": 10
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.incidentId").value(incidentId.toString()))
                .andExpect(jsonPath("$.title").value("Test Incident"))
                .andExpect(jsonPath("$.status").value("DETECTED"));

        verify(securityIncidentUseCase).createIncident(any(), anyString(), anyString());
    }

    @Test
    void shouldListIncidentsSuccessfully() throws Exception {
        var incident1 = buildResponse(UUID.randomUUID());
        var incident2 = buildResponse(UUID.randomUUID());
        var page = new PageImpl<>(List.of(incident1, incident2), Pageable.unpaged(), 2);

        when(securityIncidentUseCase.listIncidents(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/security-incidents")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));

        verify(securityIncidentUseCase).listIncidents(any(Pageable.class));
    }

    @Test
    void shouldGetIncidentSuccessfully() throws Exception {
        var incidentId = UUID.randomUUID();
        var response = buildResponse(incidentId);

        when(securityIncidentUseCase.getIncident(incidentId)).thenReturn(response);

        mockMvc.perform(get("/security-incidents/" + incidentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentId").value(incidentId.toString()))
                .andExpect(jsonPath("$.title").value("Test Incident"));

        verify(securityIncidentUseCase).getIncident(incidentId);
    }

    @Test
    void shouldUpdateIncidentSuccessfully() throws Exception {
        var incidentId = UUID.randomUUID();
        var now = Instant.now();
        var response = new SecurityIncidentResponse(
                incidentId,
                "Test Incident",
                "Test description",
                now,
                now,
                SecurityIncidentSeverity.HIGH,
                true,
                false,
                10,
                SecurityIncidentStatus.CONFIRMED,
                null,
                null,
                UUID.randomUUID(),
                now,
                now,
                false, null, null, null, null, null, null, null, null, null, null, null, null
        );

        when(securityIncidentUseCase.updateIncident(eq(incidentId), any(), anyString(), anyString()))
                .thenReturn(response);

        mockMvc.perform(patch("/security-incidents/" + incidentId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("User-Agent", "JUnit")
                .content("""
                        {
                            "status": "CONFIRMED",
                            "confirmedAt": "2026-05-21T10:00:00Z",
                            "notifiedAnpdAt": null,
                            "notifiedSubjectsAt": null
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        verify(securityIncidentUseCase).updateIncident(eq(incidentId), any(), anyString(), anyString());
    }

    private SecurityIncidentResponse buildResponse(UUID incidentId) {
        var now = Instant.now();
        return new SecurityIncidentResponse(
                incidentId,
                "Test Incident",
                "Test description",
                now,
                null,
                SecurityIncidentSeverity.HIGH,
                true,
                false,
                10,
                SecurityIncidentStatus.DETECTED,
                null,
                null,
                UUID.randomUUID(),
                now,
                null,
                false, null, null, null, null, null, null, null, null, null, null, null, null
        );
    }
}
