package com.kts.kronos.adapter.in.web.http.webmvc;

import com.kts.kronos.adapter.in.web.dto.platform.PlatformHealthMetricsResponse;
import com.kts.kronos.adapter.in.web.dto.platform.PlatformHealthResponse;
import com.kts.kronos.adapter.in.web.dto.platform.PlatformHealthSignalResponse;
import com.kts.kronos.adapter.in.web.exceptions.RestExceptionHandler;
import com.kts.kronos.adapter.in.web.http.PlatformHealthController;
import com.kts.kronos.application.service.PlatformHealthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlatformHealthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RestExceptionHandler.class, PlatformHealthControllerWebMvcTest.MethodSecurityTestConfig.class})
class PlatformHealthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlatformHealthService platformHealthService;

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }

    @Test
    @WithMockUser(roles = "CTO")
    void shouldAllowCtoToAccessPlatformHealth() throws Exception {
        when(platformHealthService.getPlatformHealth()).thenReturn(new PlatformHealthResponse(
                "OPERATIONAL",
                OffsetDateTime.parse("2026-06-03T12:00:00-03:00"),
                List.of(new PlatformHealthSignalResponse("database", "Banco de dados", "OPERATIONAL", "OK")),
                new PlatformHealthMetricsResponse(3, 0, 0, null)
        ));

        mockMvc.perform(get("/admin/platform/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("OPERATIONAL"))
                .andExpect(jsonPath("$.signals[0].id").value("database"))
                .andExpect(jsonPath("$.metrics.activeCompanies").value(3));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void shouldBlockManagerFromPlatformHealth() throws Exception {
        mockMvc.perform(get("/admin/platform/health"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(platformHealthService);
    }

    @Test
    @WithMockUser(roles = "PARTNER")
    void shouldBlockPartnerFromPlatformHealth() throws Exception {
        mockMvc.perform(get("/admin/platform/health"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(platformHealthService);
    }
}
