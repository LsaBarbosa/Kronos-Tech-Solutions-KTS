package com.kts.kronos.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import testsupport.LgpdComplianceTestApplication;
import testsupport.LgpdTestFixtures;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = LgpdComplianceTestApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("LGPD Admin Export Flow Integration Tests")
class LgpdAdminExportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LgpdTestFixtures lgpdTestFixtures;

    @Test
    @DisplayName("Unauthorized user should not access /lgpd/me/export")
    void unauthorizedShouldNotAccessOwnExport() throws Exception {
        mockMvc.perform(get("/lgpd/me/export"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Employee should not access admin export endpoint")
    @WithMockUser(username = "employee-user", roles = "EMPLOYEE")
    void employeeShouldNotAccessAdminExportEndpoint() throws Exception {
        String requestId = "550e8400-e29b-41d4-a716-446655440000";
        String payload = """
                {
                    "includePreciseGeolocation": false,
                    "legalBasis": "Art. 7, II, LGPD",
                    "operationalReason": "Compliance audit",
                    "reviewerNotes": "Approved by legal team"
                }
                """;

        mockMvc.perform(post("/lgpd/admin/requests/{requestId}/export", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Manager should not export with precise geolocation")
    @WithMockUser(username = "manager-user", roles = "MANAGER")
    void managerShouldNotExportWithPreciseGeolocation() throws Exception {
        String requestId = "550e8400-e29b-41d4-a716-446655440000";
        String payload = """
                {
                    "includePreciseGeolocation": true,
                    "legalBasis": "Art. 7, II, LGPD",
                    "operationalReason": "Compliance audit",
                    "reviewerNotes": "Approved by legal team"
                }
                """;

        mockMvc.perform(post("/lgpd/admin/requests/{requestId}/export", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin should not export for non-existent request")
    @WithMockUser(username = "manager-user", roles = "MANAGER")
    void adminShouldNotExportForNonExistentRequest() throws Exception {
        String requestId = "550e8400-e29b-41d4-a716-446655440000";
        String payload = """
                {
                    "includePreciseGeolocation": false,
                    "legalBasis": "Art. 7, II, LGPD",
                    "operationalReason": "Compliance audit",
                    "reviewerNotes": "Approved by legal team"
                }
                """;

        mockMvc.perform(post("/lgpd/admin/requests/{requestId}/export", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin should not export without legalBasis validation")
    @WithMockUser(username = "manager-user", roles = "MANAGER")
    void adminShouldNotExportWithoutLegalBasis() throws Exception {
        String requestId = "550e8400-e29b-41d4-a716-446655440000";
        String payload = """
                {
                    "includePreciseGeolocation": false,
                    "legalBasis": "",
                    "operationalReason": "Compliance audit",
                    "reviewerNotes": "Approved by legal team"
                }
                """;

        mockMvc.perform(post("/lgpd/admin/requests/{requestId}/export", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin should not export without operationalReason validation")
    @WithMockUser(username = "manager-user", roles = "MANAGER")
    void adminShouldNotExportWithoutOperationalReason() throws Exception {
        String requestId = "550e8400-e29b-41d4-a716-446655440000";
        String payload = """
                {
                    "includePreciseGeolocation": false,
                    "legalBasis": "Art. 7, II, LGPD",
                    "operationalReason": "",
                    "reviewerNotes": "Approved by legal team"
                }
                """;

        mockMvc.perform(post("/lgpd/admin/requests/{requestId}/export", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin should not export without reviewerNotes validation")
    @WithMockUser(username = "manager-user", roles = "MANAGER")
    void adminShouldNotExportWithoutReviewerNotes() throws Exception {
        String requestId = "550e8400-e29b-41d4-a716-446655440000";
        String payload = """
                {
                    "includePreciseGeolocation": false,
                    "legalBasis": "Art. 7, II, LGPD",
                    "operationalReason": "Compliance audit",
                    "reviewerNotes": ""
                }
                """;

        mockMvc.perform(post("/lgpd/admin/requests/{requestId}/export", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CTO should not export with precise geolocation without notes")
    @WithMockUser(username = "cto-user", roles = "CTO")
    void ctoShouldNotExportWithPreciseGeolocationWithoutNotes() throws Exception {
        String requestId = "550e8400-e29b-41d4-a716-446655440000";
        String payload = """
                {
                    "includePreciseGeolocation": true,
                    "legalBasis": "Art. 7, II, LGPD",
                    "operationalReason": "Security investigation",
                    "reviewerNotes": ""
                }
                """;

        mockMvc.perform(post("/lgpd/admin/requests/{requestId}/export", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isForbidden());
    }
}
