package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.exceptions.RestExceptionHandler;
import com.kts.kronos.application.port.out.provider.RetentionExecutionLogProvider;
import com.kts.kronos.application.port.out.provider.RetentionPolicyProvider;
import com.kts.kronos.application.service.retention.RetentionPolicyExecutor;
import com.kts.kronos.domain.model.RetentionExecutionLog;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.enuns.RetentionExecutionMode;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RetentionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RestExceptionHandler.class, RetentionControllerTest.MethodSecurityTestConfig.class})
@WithMockUser(roles = "CTO")
class RetentionControllerTest {
    @Resource
    private MockMvc mockMvc;

    @MockitoBean
    private RetentionPolicyProvider retentionPolicyProvider;

    @MockitoBean
    private RetentionExecutionLogProvider retentionExecutionLogProvider;

    @MockitoBean
    private RetentionPolicyExecutor retentionPolicyExecutor;

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Test
    void shouldReturnDashboardMetrics() throws Exception {
        var policies = List.of(
                createPolicy("POLICY_1", true),
                createPolicy("POLICY_2", false)
        );
        var executions = List.of(
                createExecutionLog("POLICY_1")
        );

        when(retentionPolicyProvider.findAll()).thenReturn(policies);
        when(retentionExecutionLogProvider.findRecent(10)).thenReturn(executions);

        mockMvc.perform(get("/admin/retention/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPoliciesEnabled").value(1))
                .andExpect(jsonPath("$.totalPoliciesDisabled").value(1))
                .andExpect(jsonPath("$.policies.length()").value(2))
                .andExpect(jsonPath("$.recentExecutions.length()").value(1));
    }

    @Test
    void shouldListPolicies() throws Exception {
        var policies = List.of(
                createPolicy("POLICY_1", true),
                createPolicy("POLICY_2", true)
        );

        when(retentionPolicyProvider.findAll()).thenReturn(policies);

        mockMvc.perform(get("/admin/retention/policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].policyCode").value("POLICY_1"));
    }

    @Test
    void shouldGetPolicyByCode() throws Exception {
        var policy = createPolicy("POLICY_1", true);

        when(retentionPolicyProvider.findByCode("POLICY_1")).thenReturn(policy);

        mockMvc.perform(get("/admin/retention/policies/POLICY_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyCode").value("POLICY_1"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void shouldForbidManagerFromApplyingPolicy() throws Exception {
        mockMvc.perform(post("/admin/retention/policies/POLICY_1/apply"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldListExecutions() throws Exception {
        var executions = List.of(createExecutionLog("POLICY_1"));
        var page = new PageImpl<>(executions);

        when(retentionExecutionLogProvider.findAll(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/admin/retention/executions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].policyCode").value("POLICY_1"));
    }

    private RetentionPolicy createPolicy(String code, boolean enabled) {
        return new RetentionPolicy(
                UUID.randomUUID(),
                code,
                "Test policy",
                "MESSAGE",
                90,
                RetentionExecutionMode.APPLY,
                enabled,
                false,
                false,
                null,
                Instant.now(),
                Instant.now()
        );
    }

    private RetentionExecutionLog createExecutionLog(String policyCode) {
        return new RetentionExecutionLog(
                UUID.randomUUID(),
                policyCode,
                RetentionResourceType.MESSAGE,
                "APPLY",
                Instant.now(),
                Instant.now(),
                "success",
                100L,
                80L,
                20L,
                0L,
                "Retention execution completed"
        );
    }
}
