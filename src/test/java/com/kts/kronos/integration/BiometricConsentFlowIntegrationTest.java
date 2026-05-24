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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Biometric Consent Flow Integration Tests")
class BiometricConsentFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @DisplayName("Should return current biometric term details")
    @WithMockUser(username = "testuser", roles = "EMPLOYEE")
    void shouldReturnCurrentBiometricTermDetails() throws Exception {
        mockMvc.perform(get("/terms/biometric/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentType").isNotEmpty())
                .andExpect(jsonPath("$.version").isNotEmpty())
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.content").isNotEmpty())
                .andExpect(jsonPath("$.contentHashSha256").isNotEmpty())
                .andExpect(jsonPath("$.active").isBoolean());
    }

    @Test
    @DisplayName("Should check consent status and return boolean")
    @WithMockUser(username = "testuser", roles = "EMPLOYEE")
    void shouldCheckConsentStatusReturnsBoolean() throws Exception {
        mockMvc.perform(get("/terms/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").isBoolean());
    }

    @Test
    @DisplayName("Should accept biometric term successfully")
    @WithMockUser(username = "testuser", roles = "EMPLOYEE")
    void shouldAcceptBiometricTermSuccessfully() throws Exception {
        mockMvc.perform(post("/terms/accept-biometric")
                .contentType("application/json")
                .content("{\"version\":\"1.0\",\"contentHashSha256\":\"abc123def456\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should reflect status change after acceptance")
    @WithMockUser(username = "testuser", roles = "EMPLOYEE")
    void shouldReflectStatusChangeAfterAcceptance() throws Exception {
        // Accept
        mockMvc.perform(post("/terms/accept-biometric")
                .contentType("application/json")
                .content("{\"version\":\"1.0\",\"contentHashSha256\":\"abc123\"}"))
                .andExpect(status().isNoContent());

        // Check new status is true
        mockMvc.perform(get("/terms/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(true));
    }

    @Test
    @DisplayName("Should revoke biometric consent successfully")
    @WithMockUser(username = "testuser", roles = "EMPLOYEE")
    void shouldRevokeBiometricConsentSuccessfully() throws Exception {
        // Accept first
        mockMvc.perform(post("/terms/accept-biometric")
                .contentType("application/json")
                .content("{\"version\":\"1.0\",\"contentHashSha256\":\"abc123\"}"))
                .andExpect(status().isNoContent());

        // Revoke
        mockMvc.perform(delete("/terms/revoke-biometric"))
                .andExpect(status().isNoContent());

        // Verify revoked status
        mockMvc.perform(get("/terms/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(false));
    }

    @Test
    @DisplayName("Should reflect status change after revocation")
    @WithMockUser(username = "testuser", roles = "EMPLOYEE")
    void shouldReflectStatusChangeAfterRevocation() throws Exception {
        // Accept
        mockMvc.perform(post("/terms/accept-biometric")
                .contentType("application/json")
                .content("{\"version\":\"1.0\",\"contentHashSha256\":\"abc123\"}"))
                .andExpect(status().isNoContent());

        // Verify active
        mockMvc.perform(get("/terms/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(true));

        // Revoke
        mockMvc.perform(delete("/terms/revoke-biometric"))
                .andExpect(status().isNoContent());

        // Verify revoked
        mockMvc.perform(get("/terms/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(false));
    }

    @Test
    @DisplayName("Should handle multiple accept/revoke cycles")
    @WithMockUser(username = "testuser", roles = "EMPLOYEE")
    void shouldHandleMultipleAcceptRevokeCycles() throws Exception {
        for (int i = 0; i < 3; i++) {
            // Accept
            mockMvc.perform(post("/terms/accept-biometric")
                    .contentType("application/json")
                    .content("{\"version\":\"1.0\",\"contentHashSha256\":\"hash" + i + "\"}"))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/terms/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accepted").value(true));

            // Revoke
            mockMvc.perform(delete("/terms/revoke-biometric"))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/terms/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accepted").value(false));
        }
    }

    @Test
    @DisplayName("Should retrieve consent history")
    @WithMockUser(username = "testuser", roles = "EMPLOYEE")
    void shouldRetrieveConsentHistory() throws Exception {
        mockMvc.perform(get("/terms/consents/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Should return unauthorized when not authenticated")
    void shouldReturnUnauthorizedWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/terms/status"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/terms/accept-biometric")
                .contentType("application/json")
                .content("{\"version\":\"1.0\",\"contentHashSha256\":\"abc123\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/terms/revoke-biometric"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject invalid accept request with missing fields")
    @WithMockUser(username = "testuser", roles = "EMPLOYEE")
    void shouldRejectInvalidAcceptRequest() throws Exception {
        mockMvc.perform(post("/terms/accept-biometric")
                .contentType("application/json")
                .content("{\"version\":\"1.0\"}"))
                .andExpect(status().isBadRequest());
    }
}
