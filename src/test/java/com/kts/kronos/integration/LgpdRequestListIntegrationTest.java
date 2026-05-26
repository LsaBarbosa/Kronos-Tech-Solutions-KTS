package com.kts.kronos.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.kts.kronos.adapter.in.web.exceptions.RestExceptionHandler;
import com.kts.kronos.adapter.in.web.http.LgpdController;
import com.kts.kronos.application.legal.DataProcessingCatalog;
import com.kts.kronos.application.port.in.usecase.LgpdUseCase;
import com.kts.kronos.application.security.ClientIpResolver;
import com.kts.kronos.application.service.retention.RetentionExecutionService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(LgpdController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RestExceptionHandler.class, LgpdRequestListIntegrationTest.MethodSecurityTestConfig.class})
@DisplayName("LGPD Request List Integration Tests")
class LgpdRequestListIntegrationTest {

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

    private static final String LGPD_REQUESTS_ENDPOINT = "/lgpd/requests";

    @Test
    @DisplayName("Partner should be able to list LGPD requests")
    @WithMockUser(username = "partner-user", roles = "PARTNER")
    void partnerShouldListRequests() throws Exception {
        mockMvc.perform(get(LGPD_REQUESTS_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Manager should be able to list LGPD requests")
    @WithMockUser(username = "manager-user", roles = "MANAGER")
    void managerShouldListRequests() throws Exception {
        mockMvc.perform(get(LGPD_REQUESTS_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("CTO should be able to list LGPD requests")
    @WithMockUser(username = "cto-user", roles = "CTO")
    void ctoShouldListRequests() throws Exception {
        mockMvc.perform(get(LGPD_REQUESTS_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Unauthenticated user should receive 401 when accessing LGPD requests")
    void unauthenticatedUserShouldReceive401() throws Exception {
        mockMvc.perform(get(LGPD_REQUESTS_ENDPOINT))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("LGPD requests list should return valid JSON array")
    @WithMockUser(username = "partner-user", roles = "PARTNER")
    void requestsListShouldReturnValidArray() throws Exception {
        mockMvc.perform(get(LGPD_REQUESTS_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("LGPD requests endpoint should handle different authenticated users")
    @WithMockUser(username = "user1", roles = "PARTNER")
    void endpointShouldHandleDifferentUsers() throws Exception {
        mockMvc.perform(get(LGPD_REQUESTS_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("LGPD requests list endpoint should support all authorized roles")
    @WithMockUser(username = "admin", roles = "CTO")
    void endpointShouldSupportAuthorizedRoles() throws Exception {
        mockMvc.perform(get(LGPD_REQUESTS_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("LGPD requests should be returned as JSON array even when empty")
    @WithMockUser(username = "partner-user", roles = "PARTNER")
    void emptyRequestsListShouldBeArray() throws Exception {
        mockMvc.perform(get(LGPD_REQUESTS_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").exists());
    }

    @Test
    @DisplayName("LGPD requests endpoint should reject EMPLOYEE role without specific access")
    @WithMockUser(username = "employee", roles = "EMPLOYEE")
    void employeeAccessShouldBeDenied() throws Exception {
        mockMvc.perform(get(LGPD_REQUESTS_ENDPOINT))
                .andExpect(status().isForbidden());
    }
}
