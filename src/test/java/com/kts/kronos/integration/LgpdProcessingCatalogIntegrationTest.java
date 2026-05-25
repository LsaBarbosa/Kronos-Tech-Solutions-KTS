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
import com.kts.kronos.application.service.LgpdRetentionDryRunService;
import com.kts.kronos.domain.model.DataProcessingPurpose;
import com.kts.kronos.domain.model.enuns.DataCategory;
import com.kts.kronos.domain.model.enuns.LegalBasis;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import java.util.List;

import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.mockito.Mockito.when;

@WebMvcTest(LgpdController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RestExceptionHandler.class, LgpdProcessingCatalogIntegrationTest.MethodSecurityTestConfig.class})
@DisplayName("LGPD Processing Catalog Integration Tests")
class LgpdProcessingCatalogIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LgpdUseCase lgpdUseCase;

    @MockitoBean
    private ClientIpResolver clientIpResolver;

    @MockitoBean
    private DataProcessingCatalog dataProcessingCatalog;

    @MockitoBean
    private LgpdRetentionDryRunService lgpdRetentionDryRunService;

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    private static final String PROCESSING_CATALOG_ENDPOINT = "/lgpd/processing-catalog";

    @Test
    @DisplayName("CTO should access processing catalog and receive 200")
    @WithMockUser(username = "cto-user", roles = "CTO")
    void ctoShouldAccessProcessingCatalog() throws Exception {
        List<DataProcessingPurpose> catalog = createSampleCatalog();
        when(dataProcessingCatalog.getActiveTreatments()).thenReturn(catalog);

        mockMvc.perform(get(PROCESSING_CATALOG_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].code").value("EMPLOYEE_IDENTIFICATION"))
                .andExpect(jsonPath("$[0].dataCategory").value("IDENTIFICATION"))
                .andExpect(jsonPath("$[0].legalBasis").value("CONTRACT_EXECUTION"))
                .andExpect(jsonPath("$[0].purpose").isNotEmpty())
                .andExpect(jsonPath("$[0].retentionPolicyCode").isNotEmpty())
                .andExpect(jsonPath("$[0].sensitive").value(false))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    @DisplayName("MANAGER should access processing catalog and receive 200")
    @WithMockUser(username = "manager-user", roles = "MANAGER")
    void managerShouldAccessProcessingCatalog() throws Exception {
        List<DataProcessingPurpose> catalog = createSampleCatalog();
        when(dataProcessingCatalog.getActiveTreatments()).thenReturn(catalog);

        mockMvc.perform(get(PROCESSING_CATALOG_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].code").value("EMPLOYEE_IDENTIFICATION"))
                .andExpect(jsonPath("$[0].dataCategory").value("IDENTIFICATION"))
                .andExpect(jsonPath("$[0].legalBasis").value("CONTRACT_EXECUTION"))
                .andExpect(jsonPath("$[0].sensitive").value(false))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    @DisplayName("PARTNER should access processing catalog and receive 200")
    @WithMockUser(username = "partner-user", roles = "PARTNER")
    void partnerShouldAccessProcessingCatalog() throws Exception {
        List<DataProcessingPurpose> catalog = createSampleCatalog();
        when(dataProcessingCatalog.getActiveTreatments()).thenReturn(catalog);

        mockMvc.perform(get(PROCESSING_CATALOG_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].code").value("EMPLOYEE_IDENTIFICATION"))
                .andExpect(jsonPath("$[0].dataCategory").value("IDENTIFICATION"))
                .andExpect(jsonPath("$[0].legalBasis").value("CONTRACT_EXECUTION"))
                .andExpect(jsonPath("$[0].purpose").isNotEmpty())
                .andExpect(jsonPath("$[0].retentionPolicyCode").isNotEmpty())
                .andExpect(jsonPath("$[0].sensitive").value(false))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    @DisplayName("EMPLOYEE should receive 403 when accessing processing catalog")
    @WithMockUser(username = "employee-user", roles = "EMPLOYEE")
    void employeeShouldBeForbiddenFromAccessingProcessingCatalog() throws Exception {
        mockMvc.perform(get(PROCESSING_CATALOG_ENDPOINT))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated user should receive 401 or be redirected")
    void unauthenticatedUserShouldBeUnauthorized() throws Exception {
        mockMvc.perform(get(PROCESSING_CATALOG_ENDPOINT))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Processing catalog should contain required fields in all items")
    @WithMockUser(username = "cto-user", roles = "CTO")
    void processingCatalogShouldContainAllRequiredFields() throws Exception {
        List<DataProcessingPurpose> catalog = createSampleCatalog();
        when(dataProcessingCatalog.getActiveTreatments()).thenReturn(catalog);

        mockMvc.perform(get(PROCESSING_CATALOG_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].code").isNotEmpty())
                .andExpect(jsonPath("$[0].dataCategory").isNotEmpty())
                .andExpect(jsonPath("$[0].legalBasis").isNotEmpty())
                .andExpect(jsonPath("$[0].purpose").isNotEmpty())
                .andExpect(jsonPath("$[0].retentionPolicyCode").isNotEmpty());
    }

    @Test
    @DisplayName("Processing catalog should differentiate sensitive and non-sensitive items")
    @WithMockUser(username = "manager-user", roles = "MANAGER")
    void processingCatalogShouldDifferentiateSensitiveItems() throws Exception {
        List<DataProcessingPurpose> catalogWithSensitive = List.of(
                new DataProcessingPurpose(
                        "BIOMETRIC_DATA",
                        DataCategory.BIOMETRIC,
                        LegalBasis.CONSENT,
                        "Biometric authentication",
                        "RETENTION_BIOMETRIC_ACTIVE_CONSENT",
                        true,
                        true
                ),
                new DataProcessingPurpose(
                        "EMPLOYEE_ID",
                        DataCategory.IDENTIFICATION,
                        LegalBasis.CONTRACT_EXECUTION,
                        "Employee identification",
                        "RETENTION_EMPLOYEE_CONTRACT",
                        false,
                        true
                )
        );
        when(dataProcessingCatalog.getActiveTreatments()).thenReturn(catalogWithSensitive);

        mockMvc.perform(get(PROCESSING_CATALOG_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].sensitive").value(true))
                .andExpect(jsonPath("$[1].sensitive").value(false));
    }

    @Test
    @DisplayName("Processing catalog should only include active items")
    @WithMockUser(username = "cto-user", roles = "CTO")
    void processingCatalogShouldOnlyIncludeActiveItems() throws Exception {
        List<DataProcessingPurpose> catalog = createSampleCatalog();
        when(dataProcessingCatalog.getActiveTreatments()).thenReturn(catalog);

        mockMvc.perform(get(PROCESSING_CATALOG_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    @DisplayName("Processing catalog should return valid DTO structure with no null fields")
    @WithMockUser(username = "cto-user", roles = "CTO")
    void processingCatalogDtoStructureIsValid() throws Exception {
        List<DataProcessingPurpose> catalog = createSampleCatalog();
        when(dataProcessingCatalog.getActiveTreatments()).thenReturn(catalog);

        mockMvc.perform(get(PROCESSING_CATALOG_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].code").exists())
                .andExpect(jsonPath("$[0].code").isNotEmpty())
                .andExpect(jsonPath("$[0].dataCategory").exists())
                .andExpect(jsonPath("$[0].legalBasis").exists())
                .andExpect(jsonPath("$[0].purpose").exists())
                .andExpect(jsonPath("$[0].purpose").isNotEmpty())
                .andExpect(jsonPath("$[0].retentionPolicyCode").exists())
                .andExpect(jsonPath("$[0].retentionPolicyCode").isNotEmpty())
                .andExpect(jsonPath("$[0].sensitive").exists())
                .andExpect(jsonPath("$[0].active").exists());
    }

    @Test
    @DisplayName("Processing catalog should handle empty catalog gracefully")
    @WithMockUser(username = "cto-user", roles = "CTO")
    void processingCatalogHandlesEmptyList() throws Exception {
        when(dataProcessingCatalog.getActiveTreatments()).thenReturn(List.of());

        mockMvc.perform(get(PROCESSING_CATALOG_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Processing catalog should correctly identify all sensitive items")
    @WithMockUser(username = "manager-user", roles = "MANAGER")
    void processingCatalogIdentifiesAllSensitiveItems() throws Exception {
        List<DataProcessingPurpose> catalogWithMultipleSensitive = List.of(
                new DataProcessingPurpose(
                        "BIOMETRIC_DATA",
                        DataCategory.BIOMETRIC,
                        LegalBasis.CONSENT,
                        "Biometric authentication",
                        "RETENTION_BIOMETRIC_ACTIVE_CONSENT",
                        true,
                        true
                ),
                new DataProcessingPurpose(
                        "GEOLOCATION_DATA",
                        DataCategory.GEOLOCATION,
                        LegalBasis.LEGAL_OBLIGATION,
                        "Time record geolocation",
                        "RETENTION_TIME_RECORD",
                        true,
                        true
                ),
                new DataProcessingPurpose(
                        "EMPLOYEE_ID",
                        DataCategory.IDENTIFICATION,
                        LegalBasis.CONTRACT_EXECUTION,
                        "Employee identification",
                        "RETENTION_EMPLOYEE_CONTRACT",
                        false,
                        true
                )
        );
        when(dataProcessingCatalog.getActiveTreatments()).thenReturn(catalogWithMultipleSensitive);

        mockMvc.perform(get(PROCESSING_CATALOG_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].sensitive").value(true))
                .andExpect(jsonPath("$[1].sensitive").value(true))
                .andExpect(jsonPath("$[2].sensitive").value(false));
    }

    @Test
    @DisplayName("Processing catalog should maintain data consistency across calls")
    @WithMockUser(username = "cto-user", roles = "CTO")
    void processingCatalogMaintainsConsistency() throws Exception {
        List<DataProcessingPurpose> catalog = createSampleCatalog();
        when(dataProcessingCatalog.getActiveTreatments()).thenReturn(catalog);

        // First call
        String firstResponse = mockMvc.perform(get(PROCESSING_CATALOG_ENDPOINT))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Second call should return same data
        String secondResponse = mockMvc.perform(get(PROCESSING_CATALOG_ENDPOINT))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertEquals(firstResponse, secondResponse);
    }

    @Test
    @DisplayName("Processing catalog response should include proper content type")
    @WithMockUser(username = "manager-user", roles = "MANAGER")
    void processingCatalogResponseContentType() throws Exception {
        List<DataProcessingPurpose> catalog = createSampleCatalog();
        when(dataProcessingCatalog.getActiveTreatments()).thenReturn(catalog);

        mockMvc.perform(get(PROCESSING_CATALOG_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Processing catalog should have consistent field values across items")
    @WithMockUser(username = "cto-user", roles = "CTO")
    void processingCatalogFieldConsistency() throws Exception {
        List<DataProcessingPurpose> catalog = createSampleCatalog();
        when(dataProcessingCatalog.getActiveTreatments()).thenReturn(catalog);

        mockMvc.perform(get(PROCESSING_CATALOG_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", containsInAnyOrder("EMPLOYEE_IDENTIFICATION", "BIOMETRIC_AUTHENTICATION")))
                .andExpect(jsonPath("$[*].active", everyItem(is(true))));
    }

    private List<DataProcessingPurpose> createSampleCatalog() {
        return List.of(
                new DataProcessingPurpose(
                        "EMPLOYEE_IDENTIFICATION",
                        DataCategory.IDENTIFICATION,
                        LegalBasis.CONTRACT_EXECUTION,
                        "Identificação de colaboradores",
                        "RETENTION_EMPLOYEE_CONTRACT",
                        false,
                        true
                ),
                new DataProcessingPurpose(
                        "BIOMETRIC_AUTHENTICATION",
                        DataCategory.BIOMETRIC,
                        LegalBasis.CONSENT,
                        "Autenticação biométrica",
                        "RETENTION_BIOMETRIC_ACTIVE_CONSENT",
                        true,
                        true
                )
        );
    }
}
