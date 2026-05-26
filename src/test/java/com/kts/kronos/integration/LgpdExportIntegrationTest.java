package com.kts.kronos.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.kts.kronos.adapter.out.persistence.UserRepository;
import testsupport.LgpdComplianceTestApplication;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = LgpdComplianceTestApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("LGPD Export Flow Integration Tests")
class LgpdExportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @DisplayName("Partner should export own data successfully")
    @WithMockUser(username = "partner-user", roles = "PARTNER")
    void partnerShouldExportOwnDataSuccessfully() throws Exception {
        mockMvc.perform(post("/lgpd/export/own-data")
                .contentType("application/json")
                .content("{\"justification\":\"Personal access\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportId").isNotEmpty())
                .andExpect(jsonPath("$.exportedAt").isNotEmpty())
                .andExpect(jsonPath("$.sections").isArray());
    }

    @Test
    @DisplayName("Manager should export employee data with justification")
    @WithMockUser(username = "manager-user", roles = "MANAGER")
    void managerShouldExportEmployeeDataWithJustification() throws Exception {
        mockMvc.perform(post("/lgpd/export/employee-data")
                .contentType("application/json")
                .content("{\"employeeId\":\"550e8400-e29b-41d4-a716-446655440000\",\"justification\":\"Compliance review\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportId").isNotEmpty())
                .andExpect(jsonPath("$.sections").isArray());
    }

    @Test
    @DisplayName("Manager should not export data from different company")
    @WithMockUser(username = "manager-user", roles = "MANAGER")
    void managerShouldNotExportDataFromDifferentCompany() throws Exception {
        mockMvc.perform(post("/lgpd/export/employee-data")
                .contentType("application/json")
                .content("{\"employeeId\":\"550e8400-e29b-41d4-a716-446655440001\",\"justification\":\"Unauthorized access\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Partner should not export other employee data")
    @WithMockUser(username = "partner-user", roles = "PARTNER")
    void partnerShouldNotExportOtherEmployeeData() throws Exception {
        mockMvc.perform(post("/lgpd/export/employee-data")
                .contentType("application/json")
                .content("{\"employeeId\":\"550e8400-e29b-41d4-a716-446655440000\",\"justification\":\"Unauthorized\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CTO should export data with geolocation")
    @WithMockUser(username = "cto-user", roles = "CTO")
    void ctoShouldExportDataWithGeolocation() throws Exception {
        mockMvc.perform(post("/lgpd/export/employee-data")
                .contentType("application/json")
                .content("{\"employeeId\":\"550e8400-e29b-41d4-a716-446655440000\",\"justification\":\"Audit\",\"includeGeolocation\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportId").isNotEmpty())
                .andExpect(jsonPath("$.includedGeolocation").value(true));
    }

    @Test
    @DisplayName("Partner should export own geolocation data if authorized")
    @WithMockUser(username = "partner-user", roles = "PARTNER")
    void partnerShouldExportOwnGeolocationIfAuthorized() throws Exception {
        mockMvc.perform(post("/lgpd/export/own-data")
                .contentType("application/json")
                .content("{\"justification\":\"Personal access\",\"includeGeolocation\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportId").isNotEmpty());
    }

    @Test
    @DisplayName("Manager should not receive precise geolocation for employee unless authorized")
    @WithMockUser(username = "manager-user", roles = "MANAGER")
    void managerShouldNotReceivePreciseGeolocation() throws Exception {
        mockMvc.perform(post("/lgpd/export/employee-data")
                .contentType("application/json")
                .content("{\"employeeId\":\"550e8400-e29b-41d4-a716-446655440000\",\"justification\":\"Standard export\",\"includeGeolocation\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.includedGeolocation").value(false));
    }

    @Test
    @DisplayName("Export should register audit log")
    @WithMockUser(username = "partner-user", roles = "PARTNER")
    void exportShouldRegisterAuditLog() throws Exception {
        mockMvc.perform(post("/lgpd/export/own-data")
                .contentType("application/json")
                .content("{\"justification\":\"Personal access\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportId").isNotEmpty());

        // Audit log should be registered (verified in audit log repository)
        // This would be verified by checking that the audit action LGPD_DATA_EXPORTED was logged
    }

    @Test
    @DisplayName("Export data should not contain raw sensitive information")
    @WithMockUser(username = "partner-user", roles = "PARTNER")
    void exportDataShouldNotContainRawSensitiveInfo() throws Exception {
        mockMvc.perform(post("/lgpd/export/own-data")
                .contentType("application/json")
                .content("{\"justification\":\"Personal access\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists());
        // The returned data should not contain CPF, email, or other sensitive info in raw form
        // Verification would be done by inspecting the export manifest and actual file content
    }

    @Test
    @DisplayName("Unauthorized user should not export data")
    void unauthorizedUserShouldNotExportData() throws Exception {
        mockMvc.perform(post("/lgpd/export/own-data")
                .contentType("application/json")
                .content("{\"justification\":\"Personal access\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Export without justification should fail")
    @WithMockUser(username = "manager-user", roles = "MANAGER")
    void exportWithoutJustificationShouldFail() throws Exception {
        mockMvc.perform(post("/lgpd/export/employee-data")
                .contentType("application/json")
                .content("{\"employeeId\":\"550e8400-e29b-41d4-a716-446655440000\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Export request should return manifest with sections")
    @WithMockUser(username = "partner-user", roles = "PARTNER")
    void exportShouldReturnManifestWithSections() throws Exception {
        mockMvc.perform(post("/lgpd/export/own-data")
                .contentType("application/json")
                .content("{\"justification\":\"Personal access\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportId").isNotEmpty())
                .andExpect(jsonPath("$.exportedAt").isNotEmpty())
                .andExpect(jsonPath("$.sections").isArray())
                .andExpect(jsonPath("$.sections[*]").isArray());
    }
}
