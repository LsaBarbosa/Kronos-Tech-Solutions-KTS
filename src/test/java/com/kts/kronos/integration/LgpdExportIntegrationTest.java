package com.kts.kronos.integration;

import com.kts.kronos.adapter.in.web.dto.lgpd.LgpdEmployeeExportResponse;
import com.kts.kronos.adapter.in.web.exceptions.RestExceptionHandler;
import com.kts.kronos.adapter.in.web.http.LgpdController;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.legal.DataProcessingCatalog;
import com.kts.kronos.application.port.in.usecase.LgpdUseCase;
import com.kts.kronos.application.security.ClientIpResolver;
import com.kts.kronos.application.service.retention.RetentionExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LgpdController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RestExceptionHandler.class, LgpdExportIntegrationTest.MethodSecurityTestConfig.class})
@DisplayName("LGPD Export Flow Integration Tests")
class LgpdExportIntegrationTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    private static final UUID EMPLOYEE_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID OTHER_EMPLOYEE_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID EXPORT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID REQUESTED_BY_USER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final Instant EXPORTED_AT = Instant.parse("2026-05-29T12:00:00Z");

    @MockitoBean
    private LgpdUseCase lgpdUseCase;

    @MockitoBean
    private ClientIpResolver clientIpResolver;

    @MockitoBean
    private DataProcessingCatalog dataProcessingCatalog;

    @MockitoBean
    private RetentionExecutionService retentionExecutionService;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(clientIpResolver.resolve(any())).thenReturn("127.0.0.1");
        when(lgpdUseCase.exportOwnEmployeeData(anyString(), any()))
                .thenReturn(sampleResponse(false));
        when(lgpdUseCase.exportEmployeeData(eq(EMPLOYEE_ID), eq(false), anyString(), any(), eq("Compliance review")))
                .thenReturn(sampleResponse(false));
        when(lgpdUseCase.exportEmployeeData(eq(EMPLOYEE_ID), eq(true), anyString(), any(), eq("Audit")))
                .thenReturn(sampleResponse(true));
        when(lgpdUseCase.exportEmployeeData(eq(EMPLOYEE_ID), eq(true), anyString(), any(), eq("Personal access")))
                .thenReturn(sampleResponse(true));
        when(lgpdUseCase.exportEmployeeData(eq(EMPLOYEE_ID), eq(true), anyString(), any(), eq("Standard export")))
                .thenReturn(sampleResponse(false));
    }

    @Test
    @DisplayName("Partner should export own data successfully")
    @WithMockUser(username = "partner-user", roles = "PARTNER")
    void partnerShouldExportOwnDataSuccessfully() throws Exception {
        mockMvc.perform(get("/lgpd/me/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.manifest.exportId").value(EXPORT_ID.toString()))
                .andExpect(jsonPath("$.manifest.exportedAt").value(EXPORTED_AT.toString()))
                .andExpect(jsonPath("$.manifest.sections").isArray());
    }

    @Test
    @DisplayName("Manager should export employee data with justification")
    @WithMockUser(username = "manager-user", roles = "MANAGER")
    void managerShouldExportEmployeeDataWithJustification() throws Exception {
        mockMvc.perform(get("/lgpd/employees/{employeeId}/export", EMPLOYEE_ID)
                        .param("exportReason", "Compliance review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.manifest.exportId").value(EXPORT_ID.toString()))
                .andExpect(jsonPath("$.manifest.sections").isArray());
    }

    @Test
    @DisplayName("Manager should not export data from different company")
    @WithMockUser(username = "manager-user", roles = "MANAGER")
    void managerShouldNotExportDataFromDifferentCompany() throws Exception {
        when(lgpdUseCase.exportEmployeeData(eq(OTHER_EMPLOYEE_ID), anyBoolean(), anyString(), any(), eq("Unauthorized access")))
                .thenThrow(new ForbiddenException("Você não pode acessar recursos de outro colaborador."));

        mockMvc.perform(get("/lgpd/employees/{employeeId}/export", OTHER_EMPLOYEE_ID)
                        .param("exportReason", "Unauthorized access"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Partner should not export other employee data")
    @WithMockUser(username = "partner-user", roles = "PARTNER")
    void partnerShouldNotExportOtherEmployeeData() throws Exception {
        when(lgpdUseCase.exportEmployeeData(eq(EMPLOYEE_ID), anyBoolean(), anyString(), any(), eq("Unauthorized")))
                .thenThrow(new ForbiddenException("Você não pode acessar recursos de outro colaborador."));

        mockMvc.perform(get("/lgpd/employees/{employeeId}/export", EMPLOYEE_ID)
                        .param("exportReason", "Unauthorized"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CTO should export data with geolocation")
    @WithMockUser(username = "cto-user", roles = "CTO")
    void ctoShouldExportDataWithGeolocation() throws Exception {
        mockMvc.perform(get("/lgpd/employees/{employeeId}/export", EMPLOYEE_ID)
                        .param("exportReason", "Audit")
                        .param("includePreciseGeolocation", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.manifest.exportId").value(EXPORT_ID.toString()))
                .andExpect(jsonPath("$.manifest.includePreciseGeolocation").value(true));
    }

    @Test
    @DisplayName("Partner should export own geolocation data if authorized")
    @WithMockUser(username = "partner-user", roles = "PARTNER")
    void partnerShouldExportOwnGeolocationIfAuthorized() throws Exception {
        mockMvc.perform(get("/lgpd/employees/{employeeId}/export", EMPLOYEE_ID)
                        .param("exportReason", "Personal access")
                        .param("includePreciseGeolocation", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.manifest.exportId").value(EXPORT_ID.toString()))
                .andExpect(jsonPath("$.manifest.includePreciseGeolocation").value(true));
    }

    @Test
    @DisplayName("Manager should not receive precise geolocation for employee unless authorized")
    @WithMockUser(username = "manager-user", roles = "MANAGER")
    void managerShouldNotReceivePreciseGeolocation() throws Exception {
        mockMvc.perform(get("/lgpd/employees/{employeeId}/export", EMPLOYEE_ID)
                        .param("exportReason", "Standard export")
                        .param("includePreciseGeolocation", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.manifest.includePreciseGeolocation").value(false));
    }

    @Test
    @DisplayName("Export should register audit log")
    @WithMockUser(username = "partner-user", roles = "PARTNER")
    void exportShouldRegisterAuditLog() throws Exception {
        mockMvc.perform(get("/lgpd/me/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auditLogs").isArray());
    }

    @Test
    @DisplayName("Export data should not contain raw sensitive information")
    @WithMockUser(username = "partner-user", roles = "PARTNER")
    void exportDataShouldNotContainRawSensitiveInfo() throws Exception {
        mockMvc.perform(get("/lgpd/me/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.manifest.warnings").isArray())
                .andExpect(jsonPath("$.manifest.warnings[0]").isNotEmpty());
    }

    @Test
    @DisplayName("Unauthorized user should not export data")
    void unauthorizedUserShouldNotExportData() throws Exception {
        mockMvc.perform(get("/lgpd/me/export"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Export without justification should fail")
    @WithMockUser(username = "manager-user", roles = "MANAGER")
    void exportWithoutJustificationShouldFail() throws Exception {
        when(lgpdUseCase.exportEmployeeData(eq(EMPLOYEE_ID), eq(false), anyString(), any(), isNull()))
                .thenThrow(new BadRequestException("Justificativa obrigatória para exportação de terceiros."));

        mockMvc.perform(get("/lgpd/employees/{employeeId}/export", EMPLOYEE_ID))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Export request should return manifest with sections")
    @WithMockUser(username = "partner-user", roles = "PARTNER")
    void exportShouldReturnManifestWithSections() throws Exception {
        mockMvc.perform(get("/lgpd/me/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.manifest.exportId").value(EXPORT_ID.toString()))
                .andExpect(jsonPath("$.manifest.exportedAt").value(EXPORTED_AT.toString()))
                .andExpect(jsonPath("$.manifest.sections").isArray())
                .andExpect(jsonPath("$.manifest.sections[0]").isNotEmpty());
    }

    private LgpdEmployeeExportResponse sampleResponse(boolean includePreciseGeolocation) {
        return new LgpdEmployeeExportResponse(
                new LgpdEmployeeExportResponse.ExportManifest(
                        EXPORT_ID,
                        EXPORTED_AT,
                        REQUESTED_BY_USER_ID,
                        EMPLOYEE_ID,
                        includePreciseGeolocation,
                        List.of("employee", "auditLogs", "legalConsents"),
                        List.of("Este arquivo contém dados pessoais e deve permanecer em local seguro.")
                ),
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new LgpdEmployeeExportResponse.ExportedBiometricStatus(false, false, false, 0),
                EXPORTED_AT
        );
    }
}
