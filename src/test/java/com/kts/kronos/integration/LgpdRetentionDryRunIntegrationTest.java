package com.kts.kronos.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.kts.kronos.adapter.in.web.exceptions.RestExceptionHandler;
import com.kts.kronos.adapter.in.web.http.LgpdController;
import com.kts.kronos.application.legal.DataProcessingCatalog;
import com.kts.kronos.application.port.in.usecase.LgpdUseCase;
import com.kts.kronos.application.security.ClientIpResolver;
import com.kts.kronos.application.service.LgpdRetentionDryRunService;
import com.kts.kronos.domain.model.RetentionDryRunResult;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.mockito.Mockito.when;

@WebMvcTest(LgpdController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RestExceptionHandler.class, LgpdRetentionDryRunIntegrationTest.MethodSecurityTestConfig.class})
@DisplayName("LGPD Retention DRY_RUN Endpoint Integration Tests")
class LgpdRetentionDryRunIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DataProcessingCatalog dataProcessingCatalog;

    @MockitoBean
    private LgpdUseCase lgpdUseCase;

    @MockitoBean
    private ClientIpResolver clientIpResolver;

    @MockitoBean
    private LgpdRetentionDryRunService lgpdRetentionDryRunService;

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    private static final String DRY_RUN_ENDPOINT = "/lgpd/admin/retention/dry-run";

    @BeforeEach
    void setUp() {
        when(lgpdRetentionDryRunService.executeDryRun()).thenReturn(createMockRetentionResults());
    }

    private List<RetentionDryRunResult> createMockRetentionResults() {
        return List.of(
                new RetentionDryRunResult(
                        "RETENTION_INTERNAL_MESSAGE",
                        "MESSAGE",
                        100L,
                        0L,
                        "DELETE",
                        false
                ),
                new RetentionDryRunResult(
                        "RETENTION_SECURITY_LOG",
                        "AUDIT_LOG",
                        250L,
                        0L,
                        "DELETE",
                        false
                ),
                new RetentionDryRunResult(
                        "RETENTION_DOCUMENT_GENERAL",
                        "DOCUMENT",
                        50L,
                        0L,
                        "DELETE",
                        false
                )
        );
    }

    @Test
    @DisplayName("CTO should access dry-run endpoint and receive 200")
    @WithMockUser(username = "cto-user", roles = "CTO")
    void ctoShouldAccessDryRunEndpoint() throws Exception {
        mockMvc.perform(get(DRY_RUN_ENDPOINT)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    @DisplayName("Response should contain proper retention DTO structure")
    @WithMockUser(username = "cto-user", roles = "CTO")
    void responseShouldContainProperDtoStructure() throws Exception {
        mockMvc.perform(get(DRY_RUN_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].policyCode").value("RETENTION_INTERNAL_MESSAGE"))
                .andExpect(jsonPath("$[0].resourceType").value("MESSAGE"))
                .andExpect(jsonPath("$[0].totalScanned").value(100))
                .andExpect(jsonPath("$[0].totalEligible").value(0))
                .andExpect(jsonPath("$[0].action").value("DELETE"))
                .andExpect(jsonPath("$[0].requiresManualApproval").value(false));
    }

    @Test
    @DisplayName("Response should contain required fields in all items")
    @WithMockUser(username = "cto-user", roles = "CTO")
    void responseShouldContainAllRequiredFields() throws Exception {
        mockMvc.perform(get(DRY_RUN_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].policyCode").isNotEmpty())
                .andExpect(jsonPath("$[*].resourceType").isNotEmpty())
                .andExpect(jsonPath("$[*].totalScanned").isNotEmpty())
                .andExpect(jsonPath("$[*].totalEligible").isNotEmpty())
                .andExpect(jsonPath("$[*].action").isNotEmpty())
                .andExpect(jsonPath("$[*].requiresManualApproval").isNotEmpty());
    }

    @Test
    @DisplayName("Response should not contain PII")
    @WithMockUser(username = "cto-user", roles = "CTO")
    void responseShouldNotContainPii() throws Exception {
        MvcResult result = mockMvc.perform(get(DRY_RUN_ENDPOINT))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        assertFalse(responseBody.contains("@"), "Response should not contain email addresses");
        assertFalse(responseBody.matches(".*\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}.*"), "Response should not contain CPF numbers");
        assertFalse(responseBody.contains("password"), "Response should not contain passwords");
        assertFalse(responseBody.contains("token"), "Response should not contain tokens");
        assertTrue(responseBody.contains("policyCode"), "Response should contain policy codes");
        assertTrue(responseBody.contains("resourceType"), "Response should contain resource types");
    }

    @Test
    @DisplayName("Response should not be always empty array")
    @WithMockUser(username = "cto-user", roles = "CTO")
    void responseShouldNotBeAlwaysEmpty() throws Exception {
        mockMvc.perform(get(DRY_RUN_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].policyCode", notNullValue()))
                .andExpect(jsonPath("$[0].totalScanned", notNullValue()));
    }

    @Test
    @DisplayName("Multiple resource types should be supported")
    @WithMockUser(username = "cto-user", roles = "CTO")
    void shouldSupportMultipleResourceTypes() throws Exception {
        mockMvc.perform(get(DRY_RUN_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].resourceType").value("MESSAGE"))
                .andExpect(jsonPath("$[1].resourceType").value("AUDIT_LOG"))
                .andExpect(jsonPath("$[2].resourceType").value("DOCUMENT"));
    }

    @Test
    @DisplayName("MANAGER should receive 403 when accessing dry-run endpoint")
    @WithMockUser(username = "manager-user", roles = "MANAGER")
    void managerShouldBeForbiddenFromAccessingDryRun() throws Exception {
        mockMvc.perform(get(DRY_RUN_ENDPOINT))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("EMPLOYEE should receive 403 when accessing dry-run endpoint")
    @WithMockUser(username = "employee-user", roles = "EMPLOYEE")
    void employeeShouldBeForbiddenFromAccessingDryRun() throws Exception {
        mockMvc.perform(get(DRY_RUN_ENDPOINT))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PARTNER should receive 403 when accessing dry-run endpoint")
    @WithMockUser(username = "partner-user", roles = "PARTNER")
    void partnerShouldBeForbiddenFromAccessingDryRun() throws Exception {
        mockMvc.perform(get(DRY_RUN_ENDPOINT))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated user should receive 401 when accessing dry-run endpoint")
    void unauthenticatedUserShouldBeUnauthorized() throws Exception {
        mockMvc.perform(get(DRY_RUN_ENDPOINT))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Dry-run results should have numeric counts")
    @WithMockUser(username = "cto-user", roles = "CTO")
    void dryRunResultsShouldHaveValidCounts() throws Exception {
        mockMvc.perform(get(DRY_RUN_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalScanned").value(100))
                .andExpect(jsonPath("$[0].totalEligible").value(0))
                .andExpect(jsonPath("$[1].totalScanned").value(250))
                .andExpect(jsonPath("$[2].totalScanned").value(50));
    }

    @Test
    @DisplayName("Dry-run action should be DELETE or valid action")
    @WithMockUser(username = "cto-user", roles = "CTO")
    void dryRunActionShouldBeValid() throws Exception {
        mockMvc.perform(get(DRY_RUN_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("DELETE"))
                .andExpect(jsonPath("$[1].action").value("DELETE"))
                .andExpect(jsonPath("$[2].action").value("DELETE"));
    }

    @Test
    @DisplayName("Response content type should be JSON")
    @WithMockUser(username = "cto-user", roles = "CTO")
    void responseContentTypeShouldBeJson() throws Exception {
        mockMvc.perform(get(DRY_RUN_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON));
    }
}
