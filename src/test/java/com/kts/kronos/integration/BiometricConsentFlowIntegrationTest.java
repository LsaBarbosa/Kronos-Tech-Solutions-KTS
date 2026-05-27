package com.kts.kronos.integration;

import org.junit.jupiter.api.BeforeEach;
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
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.domain.model.enuns.Role;
import testsupport.LgpdComplianceTestApplication;
import testsupport.LgpdTestFixtures;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = LgpdComplianceTestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
@DisplayName("Biometric Consent Flow Integration Tests")
class BiometricConsentFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LgpdTestFixtures lgpdTestFixtures;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtAuthenticatedUser jwtAuthenticatedUser;

    private UUID testEmployeeId;
    private String biometricTermHash;
    private static final UUID TEST_USER_ID = UUID.randomUUID();

    @BeforeEach
    void setupTestData() {
        testEmployeeId = lgpdTestFixtures.seedTestEmployee();
        biometricTermHash = lgpdTestFixtures.seedBiometricTermAndReturnHash();

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(testEmployeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(TEST_USER_ID);
        when(jwtAuthenticatedUser.getUsername()).thenReturn("testuser");
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
    }

    private String acceptBiometricJson() {
        return "{\"version\":\"1.0\",\"contentHashSha256\":\"" + biometricTermHash + "\"}";
    }

    @Test
    @DisplayName("Should return current biometric term details")
    @WithMockUser(username = "testuser", roles = "MANAGER")
    void shouldReturnCurrentBiometricTermDetails() throws Exception {
        mockMvc.perform(get("/terms/biometric/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").isNotEmpty())
                .andExpect(jsonPath("$.version").isNotEmpty())
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.content").isNotEmpty())
                .andExpect(jsonPath("$.contentHashSha256").isNotEmpty())
                .andExpect(jsonPath("$.active").isBoolean());
    }

    @Test
    @DisplayName("Should check consent status and return boolean")
    @WithMockUser(username = "testuser", roles = "MANAGER")
    void shouldCheckConsentStatusReturnsBoolean() throws Exception {
        mockMvc.perform(get("/terms/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.biometricConsentAccepted").isBoolean())
                .andExpect(jsonPath("$.currentVersion").isNotEmpty())
                .andExpect(jsonPath("$.currentHash").isNotEmpty())
                .andExpect(jsonPath("$.requiresNewAcceptance").isBoolean());
    }

    @Test
    @DisplayName("Should accept biometric term successfully")
    @WithMockUser(username = "testuser", roles = "MANAGER")
    void shouldAcceptBiometricTermSuccessfully() throws Exception {
        mockMvc.perform(post("/terms/accept-biometric")
                .contentType("application/json")
                .content(acceptBiometricJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.biometricConsentAccepted").value(true))
                .andExpect(jsonPath("$.requiresNewAcceptance").value(false));
    }

    @Test
    @DisplayName("Should reflect status change after acceptance")
    @WithMockUser(username = "testuser", roles = "MANAGER")
    void shouldReflectStatusChangeAfterAcceptance() throws Exception {
        // Accept
        mockMvc.perform(post("/terms/accept-biometric")
                .contentType("application/json")
                .content(acceptBiometricJson()))
                .andExpect(status().isOk());

        // Check new status is true
        mockMvc.perform(get("/terms/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.biometricConsentAccepted").value(true));
    }

    @Test
    @DisplayName("Should revoke biometric consent successfully")
    @WithMockUser(username = "testuser", roles = "MANAGER")
    void shouldRevokeBiometricConsentSuccessfully() throws Exception {
        // Accept first
        mockMvc.perform(post("/terms/accept-biometric")
                .contentType("application/json")
                .content(acceptBiometricJson()))
                .andExpect(status().isOk());

        // Revoke
        mockMvc.perform(delete("/terms/revoke-biometric"))
                .andExpect(status().isOk());

        // Verify revoked status
        mockMvc.perform(get("/terms/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.biometricConsentAccepted").value(false));
    }

    @Test
    @DisplayName("Should reflect status change after revocation")
    @WithMockUser(username = "testuser", roles = "MANAGER")
    void shouldReflectStatusChangeAfterRevocation() throws Exception {
        // Accept
        mockMvc.perform(post("/terms/accept-biometric")
                .contentType("application/json")
                .content(acceptBiometricJson()))
                .andExpect(status().isOk());

        // Verify active
        mockMvc.perform(get("/terms/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.biometricConsentAccepted").value(true));

        // Revoke
        mockMvc.perform(delete("/terms/revoke-biometric"))
                .andExpect(status().isOk());

        // Verify revoked
        mockMvc.perform(get("/terms/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.biometricConsentAccepted").value(false));
    }

    @Test
    @DisplayName("Should handle multiple accept/revoke cycles")
    @WithMockUser(username = "testuser", roles = "MANAGER")
    void shouldHandleMultipleAcceptRevokeCycles() throws Exception {
        for (int i = 0; i < 3; i++) {
            // Accept
            mockMvc.perform(post("/terms/accept-biometric")
                    .contentType("application/json")
                    .content(acceptBiometricJson()))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/terms/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.biometricConsentAccepted").value(true));

            // Revoke
            mockMvc.perform(delete("/terms/revoke-biometric"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/terms/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.biometricConsentAccepted").value(false));
        }
    }

    @Test
    @DisplayName("Should retrieve consent history")
    @WithMockUser(username = "testuser", roles = "MANAGER")
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
                .content(acceptBiometricJson()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/terms/revoke-biometric"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject invalid accept request with missing fields")
    @WithMockUser(username = "testuser", roles = "MANAGER")
    void shouldRejectInvalidAcceptRequest() throws Exception {
        mockMvc.perform(post("/terms/accept-biometric")
                .contentType("application/json")
                .content("{\"version\":\"1.0\"}"))
                .andExpect(status().isBadRequest());
    }
}
