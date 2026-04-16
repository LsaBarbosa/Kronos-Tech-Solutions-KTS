package com.kts.kronos.config;

import com.kts.kronos.adapter.out.security.CustomUserDetailsService;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
import com.kts.kronos.application.port.in.usecase.EmployeeUseCase;
import com.kts.kronos.application.port.in.usecase.UserUseCase;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = EnumerationSecurityTestApplication.class,
        properties = {
                "frontend.base-url-record=http://record.local",
                "frontend.base-url-plataform=http://platform.local",
                "frontend.base-url-local=http://local.test",
                "frontend.base-url-local-2=http://local2.test"
        }
)
@AutoConfigureMockMvc
class UserEnumerationExposureIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompanyUseCase companyUseCase;

    @MockitoBean
    private EmployeeUseCase employeeUseCase;

    @MockitoBean
    private UserUseCase userUseCase;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @Test
    void shouldBlockPublicCnpjEnumerationEndpoint() throws Exception {
        mockMvc.perform(get("/companies/check-cnpj")
                        .param("cnpj", "12345678000199"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(companyUseCase);
    }

    @Test
    void shouldBlockManagerAccessToCnpjEnumerationEndpoint() throws Exception {
        mockMvc.perform(get("/companies/check-cnpj")
                        .param("cnpj", "12345678000199")
                        .with(user("manager").roles("MANAGER")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(companyUseCase);
    }

    @Test
    void shouldBlockPublicCpfEnumerationEndpoint() throws Exception {
        mockMvc.perform(get("/employee/check-cpf")
                        .param("cpf", "12345678901"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(employeeUseCase);
    }

    @Test
    void shouldBlockPublicUsernameEnumerationEndpoint() throws Exception {
        mockMvc.perform(get("/users/check-username")
                        .param("username", "admin"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userUseCase);
    }

    @Test
    void shouldBlockPartnerFromBroadUsersSearch() throws Exception {
        mockMvc.perform(get("/users/search")
                        .with(user("partner").roles("PARTNER")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userUseCase);
    }

    @Test
    void shouldAllowPartnerToSearchActiveManagersOnly() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        User manager = new User(userId, "manager1", "encoded", Role.MANAGER, true, employeeId);
        when(userUseCase.listUsers(true)).thenReturn(List.of(manager));

        mockMvc.perform(get("/users/search")
                        .param("active", "true")
                        .with(user("partner").roles("PARTNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$.users[0].username").value("manager1"))
                .andExpect(jsonPath("$.users[0].role").value("MANAGER"))
                .andExpect(jsonPath("$.users[0].active").value(true));
    }

    @Test
    void shouldReduceUsersSearchPayloadMetadata() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        User user = new User(userId, "manager1", "encoded", Role.MANAGER, true, employeeId);
        when(userUseCase.listUsers(null)).thenReturn(List.of(user));

        mockMvc.perform(get("/users/search")
                        .with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$.users[0].username").value("manager1"))
                .andExpect(jsonPath("$.users[0].role").value("MANAGER"))
                .andExpect(jsonPath("$.users[0].active").value(true))
                .andExpect(jsonPath("$.users[0].employeeId").doesNotExist());
    }
}
