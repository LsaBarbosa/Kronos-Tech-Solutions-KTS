package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.application.service.PublicPrivacyService;
import com.kts.kronos.application.port.out.provider.CacheProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.function.Supplier;

@WebMvcTest(PublicPrivacyController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(PublicPrivacyService.class)
class PublicPrivacyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CacheProvider cacheProvider;

    @BeforeEach
    void stubCacheProviderAsNoOp() {
        doAnswer(invocation -> {
            Supplier<?> loader = invocation.getArgument(3);
            return loader.get();
        }).when(cacheProvider).getOrLoad(anyString(), anyString(), any(), any());
    }

    @Test
    void shouldGetProcessingCatalogWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/public/privacy/processing-catalog"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldGetPrivacyPolicyWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/public/privacy/policy"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldGetBiometricTermWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/public/privacy/biometric-term"))
                .andExpect(status().isOk());
    }

    @Test
    void processingCatalogShouldHaveVersion() throws Exception {
        mockMvc.perform(get("/public/privacy/processing-catalog"))
                .andExpect(jsonPath("$.version").exists())
                .andExpect(jsonPath("$.version").isString());
    }

    @Test
    void processingCatalogShouldHaveEffectiveDate() throws Exception {
        mockMvc.perform(get("/public/privacy/processing-catalog"))
                .andExpect(jsonPath("$.effectiveDate").exists())
                .andExpect(jsonPath("$.effectiveDate").isString());
    }

    @Test
    void processingCatalogShouldHaveActivities() throws Exception {
        mockMvc.perform(get("/public/privacy/processing-catalog"))
                .andExpect(jsonPath("$.activities").isArray())
                .andExpect(jsonPath("$.activities", hasSize(greaterThan(0))));
    }

    @Test
    void eachActivityShouldHaveRequiredFields() throws Exception {
        mockMvc.perform(get("/public/privacy/processing-catalog"))
                .andExpect(jsonPath("$.activities[0].code").exists())
                .andExpect(jsonPath("$.activities[0].title").exists())
                .andExpect(jsonPath("$.activities[0].description").exists())
                .andExpect(jsonPath("$.activities[0].dataCategories").isArray())
                .andExpect(jsonPath("$.activities[0].purposes").isArray())
                .andExpect(jsonPath("$.activities[0].legalBases").isArray())
                .andExpect(jsonPath("$.activities[0].retentionPolicy").exists())
                .andExpect(jsonPath("$.activities[0].dataSubjectRights").isArray());
    }

    @Test
    void privacyPolicyShouldHaveVersion() throws Exception {
        mockMvc.perform(get("/public/privacy/policy"))
                .andExpect(jsonPath("$.version").exists());
    }

    @Test
    void privacyPolicyShouldHaveEffectiveDate() throws Exception {
        mockMvc.perform(get("/public/privacy/policy"))
                .andExpect(jsonPath("$.effectiveDate").exists());
    }

    @Test
    void privacyPolicyShouldHaveSections() throws Exception {
        mockMvc.perform(get("/public/privacy/policy"))
                .andExpect(jsonPath("$.sections").isArray())
                .andExpect(jsonPath("$.sections", hasSize(greaterThan(0))));
    }

    @Test
    void privacyPolicySectionsShouldHaveContent() throws Exception {
        mockMvc.perform(get("/public/privacy/policy"))
                .andExpect(jsonPath("$.sections[0].title").exists())
                .andExpect(jsonPath("$.sections[0].content").exists());
    }

    @Test
    void biometricTermShouldHaveVersion() throws Exception {
        mockMvc.perform(get("/public/privacy/biometric-term"))
                .andExpect(jsonPath("$.version").exists());
    }

    @Test
    void biometricTermShouldHaveEffectiveDate() throws Exception {
        mockMvc.perform(get("/public/privacy/biometric-term"))
                .andExpect(jsonPath("$.effectiveDate").exists());
    }

    @Test
    void biometricTermShouldHaveSections() throws Exception {
        mockMvc.perform(get("/public/privacy/biometric-term"))
                .andExpect(jsonPath("$.sections").isArray())
                .andExpect(jsonPath("$.sections", hasSize(greaterThan(0))));
    }

    @Test
    void biometricTermSectionsShouldHaveContent() throws Exception {
        mockMvc.perform(get("/public/privacy/biometric-term"))
                .andExpect(jsonPath("$.sections[0].title").exists())
                .andExpect(jsonPath("$.sections[0].content").exists());
    }

    @Test
    void processingCatalogShouldNotContainEmployeeId() throws Exception {
        String response = mockMvc.perform(get("/public/privacy/processing-catalog"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assert !response.contains("employeeId") : "Response contains employeeId";
    }

    @Test
    void processingCatalogShouldNotContainUserId() throws Exception {
        String response = mockMvc.perform(get("/public/privacy/processing-catalog"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assert !response.contains("userId") : "Response contains userId";
    }

    @Test
    void processingCatalogShouldNotContainCompanyId() throws Exception {
        String response = mockMvc.perform(get("/public/privacy/processing-catalog"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assert !response.contains("companyId") : "Response contains companyId";
    }

    @Test
    void processingCatalogShouldNotContainS3References() throws Exception {
        String response = mockMvc.perform(get("/public/privacy/processing-catalog"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assert !response.contains("storagePath") : "Response contains storagePath";
        assert !response.contains("bucket") : "Response contains bucket";
        assert !response.contains("s3") : "Response contains s3";
    }

    @Test
    void processingCatalogShouldNotContainRoleReferences() throws Exception {
        String response = mockMvc.perform(get("/public/privacy/processing-catalog"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assert !response.contains("ROLE_") : "Response contains ROLE_";
        assert !response.contains("ANY_EMPLOYEE") : "Response contains ANY_EMPLOYEE";
    }

    @Test
    void processingCatalogShouldNotContainTableNames() throws Exception {
        String response = mockMvc.perform(get("/public/privacy/processing-catalog"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assert !response.contains("tb_") : "Response contains tb_";
    }

    @Test
    void privacyPolicyShouldNotContainSensitiveData() throws Exception {
        String response = mockMvc.perform(get("/public/privacy/policy"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assert !response.contains("employeeId") : "Response contains employeeId";
        assert !response.contains("userId") : "Response contains userId";
        assert !response.contains("storagePath") : "Response contains storagePath";
    }

    @Test
    void biometricTermShouldNotContainSensitiveData() throws Exception {
        String response = mockMvc.perform(get("/public/privacy/biometric-term"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assert !response.contains("employeeId") : "Response contains employeeId";
        assert !response.contains("userId") : "Response contains userId";
        assert !response.contains("faceS3ObjectKey") : "Response contains faceS3ObjectKey";
    }
}
