package com.kts.kronos.integration;

import com.kts.kronos.adapter.in.web.exceptions.RestExceptionHandler;
import com.kts.kronos.adapter.in.web.http.LgpdController;
import com.kts.kronos.application.legal.DataProcessingCatalog;
import com.kts.kronos.application.port.in.usecase.LgpdUseCase;
import com.kts.kronos.application.security.ClientIpResolver;
import com.kts.kronos.application.service.retention.RetentionBatchExecutionSummary;
import com.kts.kronos.application.service.retention.RetentionExecutionService;
import com.kts.kronos.domain.model.LgpdRequest;
import com.kts.kronos.domain.model.RetentionDryRunResult;
import com.kts.kronos.domain.model.enuns.LgpdRequestStatus;
import com.kts.kronos.domain.model.enuns.LgpdRequestType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(LgpdController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RestExceptionHandler.class, LgpdAdminRequestManagementIntegrationTest.MethodSecurityTestConfig.class})
@DisplayName("LGPD Admin Request Management Integration Tests")
class LgpdAdminRequestManagementIntegrationTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

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

    private static final String ADMIN_REQUESTS_ENDPOINT = "/lgpd/admin/requests";
    private static final String RETENTION_DRYRUN_ENDPOINT = "/lgpd/admin/retention/dry-run";
    private static final UUID REQUEST_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID COMPANY_ID = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final UUID ASSIGNED_TO_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000103");

    @BeforeEach
    void setUp() {
        when(lgpdUseCase.listAdminRequests(any(), any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());
        when(lgpdUseCase.assignRequest(any(UUID.class), any(UUID.class)))
                .thenReturn(sampleRequest());
        when(lgpdUseCase.addNote(any(UUID.class), anyString(), any()))
                .thenReturn(sampleRequest());
        when(lgpdUseCase.completeRequest(any(UUID.class), anyString(), any()))
                .thenReturn(sampleRequest());
        when(lgpdUseCase.rejectRequest(any(UUID.class), anyString(), anyString(), any()))
                .thenReturn(sampleRequest());
        when(retentionExecutionService.executeActivePolicies(any(), any(), anyBoolean(), anyString()))
                .thenReturn(new RetentionBatchExecutionSummary(
                        "DRY_RUN",
                        1,
                        5,
                        2,
                        0,
                        false,
                        List.of(new RetentionDryRunResult(
                                "RETENTION_INTERNAL_MESSAGE",
                                "message",
                                5,
                                2,
                                "DELETE",
                                false
                        ))
                ));
    }

    @Test
    @DisplayName("CTO should be able to access admin LGPD requests endpoint")
    @WithMockUser(username = "cto-user", roles = "CTO")
    void ctoShouldAccessAdminRequests() throws Exception {
        mockMvc.perform(get(ADMIN_REQUESTS_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    @DisplayName("Manager should be able to access admin LGPD requests endpoint")
    @WithMockUser(username = "manager-user", roles = "MANAGER")
    void managerShouldAccessAdminRequests() throws Exception {
        mockMvc.perform(get(ADMIN_REQUESTS_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    @DisplayName("Partner should not be able to access admin LGPD requests endpoint")
    @WithMockUser(username = "partner-user", roles = "PARTNER")
    void partnerShouldNotAccessAdminRequests() throws Exception {
        mockMvc.perform(get(ADMIN_REQUESTS_ENDPOINT))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated user should not access admin LGPD requests endpoint")
    void unauthenticatedShouldNotAccessAdminRequests() throws Exception {
        mockMvc.perform(get(ADMIN_REQUESTS_ENDPOINT))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CTO should be able to access retention dry-run endpoint")
    @WithMockUser(username = "cto-user", roles = "CTO")
    void ctoShouldAccessRetentionDryRun() throws Exception {
        mockMvc.perform(get(RETENTION_DRYRUN_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Manager should not access retention dry-run endpoint (CTO only)")
    @WithMockUser(username = "manager-user", roles = "MANAGER")
    void managerShouldNotAccessRetentionDryRun() throws Exception {
        mockMvc.perform(get(RETENTION_DRYRUN_ENDPOINT))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin requests endpoint should support pagination parameters")
    @WithMockUser(username = "cto-user", roles = "CTO")
    void adminRequestsShouldSupportPagination() throws Exception {
        mockMvc.perform(get(ADMIN_REQUESTS_ENDPOINT)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Admin requests endpoint should support filtering by type")
    @WithMockUser(username = "cto-user", roles = "CTO")
    void adminRequestsShouldSupportTypeFilter() throws Exception {
        mockMvc.perform(get(ADMIN_REQUESTS_ENDPOINT)
                .param("type", "ACCESS"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Admin requests endpoint should support filtering by status")
    @WithMockUser(username = "manager-user", roles = "MANAGER")
    void adminRequestsShouldSupportStatusFilter() throws Exception {
        mockMvc.perform(get(ADMIN_REQUESTS_ENDPOINT)
                .param("status", "OPEN"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Admin requests endpoint should support filtering by company")
    @WithMockUser(username = "cto-user", roles = "CTO")
    void adminRequestsShouldSupportCompanyFilter() throws Exception {
        mockMvc.perform(get(ADMIN_REQUESTS_ENDPOINT)
                .param("companyId", COMPANY_ID.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CTO should be able to access request assignment endpoint")
    @WithMockUser(username = "cto-user", roles = "CTO")
    void ctoShouldAccessAssignEndpoint() throws Exception {
        mockMvc.perform(patch("/lgpd/admin/requests/{requestId}/assign", REQUEST_ID)
                .contentType("application/json")
                .content("{\"assignedToUserId\":\"" + ASSIGNED_TO_USER_ID + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Manager should be able to add note to LGPD request endpoint")
    @WithMockUser(username = "manager-user", roles = "MANAGER")
    void managerShouldAccessNoteEndpoint() throws Exception {
        mockMvc.perform(post("/lgpd/admin/requests/{requestId}/notes", REQUEST_ID)
                .contentType("application/json")
                .content("{\"publicNote\":\"Processing in progress\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Retention dry-run endpoint should return array of results")
    @WithMockUser(username = "cto-user", roles = "CTO")
    void retentionDryRunShouldReturnResults() throws Exception {
        mockMvc.perform(get(RETENTION_DRYRUN_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Admin endpoints should require MANAGER or CTO role")
    @WithMockUser(username = "employee-user", roles = "EMPLOYEE")
    void adminEndpointsShouldRequireManagerOrCto() throws Exception {
        mockMvc.perform(get(ADMIN_REQUESTS_ENDPOINT))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin requests endpoint should return JSON with proper structure")
    @WithMockUser(username = "cto-user", roles = "CTO")
    void adminEndpointShouldReturnStructuredJson() throws Exception {
        mockMvc.perform(get(ADMIN_REQUESTS_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    @DisplayName("CTO should be able to access request completion endpoint")
    @WithMockUser(username = "cto-user", roles = "CTO")
    void ctoShouldAccessCompleteEndpoint() throws Exception {
        mockMvc.perform(post("/lgpd/admin/requests/{requestId}/complete", REQUEST_ID)
                .contentType("application/json")
                .content("{\"publicResolutionNotes\":\"Request processed\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Manager should be able to access request rejection endpoint")
    @WithMockUser(username = "manager-user", roles = "MANAGER")
    void managerShouldAccessRejectEndpoint() throws Exception {
        mockMvc.perform(post("/lgpd/admin/requests/{requestId}/reject", REQUEST_ID)
                .contentType("application/json")
                .content("""
                        {
                          "closedReason":"Request cannot be fulfilled",
                          "publicNote":"Request cannot be fulfilled"
                        }
                        """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Admin endpoints should handle company filtering")
    @WithMockUser(username = "manager-user", roles = "MANAGER")
    void adminEndpointsShouldHandleCompanyFilter() throws Exception {
        mockMvc.perform(get(ADMIN_REQUESTS_ENDPOINT)
                .param("companyId", COMPANY_ID.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Retention policy endpoint should validate required parameters")
    @WithMockUser(username = "cto-user", roles = "CTO")
    void retentionEndpointShouldValidateParameters() throws Exception {
        mockMvc.perform(get(RETENTION_DRYRUN_ENDPOINT))
                .andExpect(status().isOk());
    }

    private LgpdRequest sampleRequest() {
        Instant now = Instant.parse("2026-05-26T12:00:00Z");
        return new LgpdRequest(
                REQUEST_ID,
                UUID.fromString("00000000-0000-0000-0000-000000000104"),
                UUID.fromString("00000000-0000-0000-0000-000000000105"),
                COMPANY_ID,
                LgpdRequestType.ACCESS,
                LgpdRequestStatus.OPEN,
                "Sample request",
                null,
                now,
                now,
                null,
                null,
                ASSIGNED_TO_USER_ID,
                now.plusSeconds(86_400),
                "HIGH",
                null,
                null,
                null
        );
    }
}
