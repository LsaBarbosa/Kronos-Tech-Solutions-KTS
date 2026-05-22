package com.kts.kronos.adapter.in.web.http.webmvc;

import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.adapter.in.web.dto.lgpd.LgpdEmployeeExportResponse;
import com.kts.kronos.adapter.in.web.exceptions.RestExceptionHandler;
import com.kts.kronos.adapter.in.web.http.LgpdController;
import com.kts.kronos.application.port.in.usecase.LgpdUseCase;
import com.kts.kronos.application.security.ClientIpResolver;
import com.kts.kronos.domain.model.LgpdRequest;
import com.kts.kronos.domain.model.LgpdRequestHistory;
import com.kts.kronos.domain.model.enuns.LgpdRequestStatus;
import com.kts.kronos.domain.model.enuns.LgpdRequestType;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LgpdController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RestExceptionHandler.class, LgpdControllerWebMvcTest.MethodSecurityTestConfig.class})
@WithMockUser(roles = "MANAGER")
class LgpdControllerWebMvcTest {
    @Resource
    private MockMvc mockMvc;

    @MockitoBean
    private LgpdUseCase lgpdUseCase;

    @MockitoBean
    private ClientIpResolver clientIpResolver;

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Test
    void shouldCreateLgpdRequest() throws Exception {
        UUID employeeId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        when(clientIpResolver.resolve(any(HttpServletRequest.class))).thenReturn("127.0.0.1");
        when(lgpdUseCase.createRequest(any(), eq("127.0.0.1"), eq("JUnit"))).thenReturn(new LgpdRequest(
                requestId,
                employeeId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                LgpdRequestType.DATA_EXPORT,
                LgpdRequestStatus.OPEN,
                "Exportar meus dados",
                null,
                Instant.now(),
                Instant.now(),
                null,
                null
        ));

        mockMvc.perform(post("/lgpd/requests")
                        .contentType("application/json")
                        .header("User-Agent", "JUnit")
                        .content("""
                                {
                                  "type": "DATA_EXPORT",
                                  "description": "Exportar meus dados"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestId").value(requestId.toString()))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void shouldReturnRequestHistory() throws Exception {
        UUID requestId = UUID.randomUUID();

        when(lgpdUseCase.getRequestHistory(requestId)).thenReturn(List.of(
                new LgpdRequestHistory(UUID.randomUUID(), requestId, LgpdRequestStatus.OPEN, "Criada", UUID.randomUUID(), Instant.now())
        ));

        mockMvc.perform(get("/lgpd/requests/{requestId}/history", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].requestId").value(requestId.toString()));
    }

    @Test
    void shouldUpdateRequestStatus() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        when(lgpdUseCase.updateRequestStatus(eq(requestId), any())).thenReturn(new LgpdRequest(
                requestId,
                employeeId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                LgpdRequestType.DATA_EXPORT,
                LgpdRequestStatus.COMPLETED,
                "Exportar meus dados",
                "Atendido",
                Instant.now(),
                Instant.now(),
                Instant.now(),
                UUID.randomUUID()
        ));

        mockMvc.perform(patch("/lgpd/requests/{requestId}/status", requestId)
                        .contentType("application/json")
                        .content("""
                                {
                                  "status": "COMPLETED",
                                  "notes": "Atendido"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.resolutionNotes").value("Atendido"));
    }

    @Test
    void shouldExportEmployeeData() throws Exception {
        UUID employeeId = UUID.randomUUID();
        when(clientIpResolver.resolve(any(HttpServletRequest.class))).thenReturn("127.0.0.1");
        when(lgpdUseCase.exportEmployeeData(eq(employeeId), eq(false), eq("127.0.0.1"), eq("JUnit"))).thenReturn(
                new LgpdEmployeeExportResponse(
                        new LgpdEmployeeExportResponse.ExportedEmployee(
                                employeeId,
                                "Lucas",
                                "12345678901",
                                "98765432100",
                                "Dev",
                                "lucas@kts.com",
                                5000.0,
                                "11999999999",
                                true,
                                new LgpdEmployeeExportResponse.ExportedAddress("Rua A", "100", "01001000", "São Paulo", "SP"),
                                UUID.randomUUID(),
                                LocalDateTime.now(),
                                true,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                Set.of(),
                                null,
                                null,
                                null
                        ),
                        new LgpdEmployeeExportResponse.ExportedUser(
                                UUID.randomUUID(),
                                "lucas",
                                com.kts.kronos.domain.model.enuns.Role.MANAGER,
                                true,
                                employeeId,
                                null,
                                null,
                                null
                        ),
                        new LgpdEmployeeExportResponse.ExportedCompany(
                                UUID.randomUUID(),
                                "KTS",
                                "12345678000199",
                                "contato@kts.com",
                                true,
                                new LgpdEmployeeExportResponse.ExportedAddress("Rua A", "100", "01001000", "São Paulo", "SP"),
                                new Location(-23.0, -46.0),
                                10,
                                1,
                                null,
                                null,
                                null
                        ),
                        List.of(new LgpdEmployeeExportResponse.ExportedDocumentMetadata(
                                UUID.randomUUID(),
                                employeeId,
                                com.kts.kronos.domain.model.enuns.DocumentType.BIOMETRIC_CONSENT_TERM,
                                "termo.pdf",
                                "application/pdf",
                                LocalDateTime.now(),
                                null,
                                false,
                                false,
                                "checksum-123"
                        )),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        new LgpdEmployeeExportResponse.ExportedBiometricStatus(true, true, true, 1),
                        Instant.now()
                )
        );

        mockMvc.perform(get("/lgpd/employees/{employeeId}/export", employeeId)
                        .header("User-Agent", "JUnit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("lucas"))
                .andExpect(jsonPath("$.documents[0].checksumSha256").value("checksum-123"))
                .andExpect(jsonPath("$.user.password").doesNotExist());

        verify(lgpdUseCase).exportEmployeeData(employeeId, false, "127.0.0.1", "JUnit");
    }

    @Test
    void shouldForwardPreciseGeolocationFlagToUseCase() throws Exception {
        UUID employeeId = UUID.randomUUID();
        when(clientIpResolver.resolve(any(HttpServletRequest.class))).thenReturn("127.0.0.1");
        when(lgpdUseCase.exportEmployeeData(eq(employeeId), eq(true), eq("127.0.0.1"), eq("JUnit"))).thenReturn(
                new LgpdEmployeeExportResponse(
                        null, null, null, List.of(), List.of(), List.of(), List.of(), List.of(), null, Instant.now()
                )
        );

        mockMvc.perform(get("/lgpd/employees/{employeeId}/export", employeeId)
                        .queryParam("includePreciseGeolocation", "true")
                        .header("User-Agent", "JUnit"))
                .andExpect(status().isOk());

        verify(lgpdUseCase).exportEmployeeData(employeeId, true, "127.0.0.1", "JUnit");
    }

    @Test
    void shouldAnonymizeEmployee() throws Exception {
        UUID employeeId = UUID.randomUUID();
        when(clientIpResolver.resolve(any(HttpServletRequest.class))).thenReturn("127.0.0.1");

        mockMvc.perform(post("/lgpd/employees/{employeeId}/anonymize", employeeId)
                        .header("User-Agent", "JUnit"))
                .andExpect(status().isNoContent());

        verify(lgpdUseCase).anonymizeEmployee(employeeId, "127.0.0.1", "JUnit");
    }

    @Test
    @WithMockUser(roles = "PARTNER")
    void shouldForbidPartnerFromAnonymizingEmployee() throws Exception {
        UUID employeeId = UUID.randomUUID();

        mockMvc.perform(post("/lgpd/employees/{employeeId}/anonymize", employeeId)
                        .header("User-Agent", "JUnit"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CTO")
    void shouldAllowCtoToAnonymizeEmployee() throws Exception {
        UUID employeeId = UUID.randomUUID();
        when(clientIpResolver.resolve(any(HttpServletRequest.class))).thenReturn("127.0.0.1");

        mockMvc.perform(post("/lgpd/employees/{employeeId}/anonymize", employeeId)
                        .header("User-Agent", "JUnit"))
                .andExpect(status().isNoContent());

        verify(lgpdUseCase).anonymizeEmployee(employeeId, "127.0.0.1", "JUnit");
    }
}
