package com.kts.kronos.config;

import com.kts.kronos.adapter.out.security.CustomUserDetailsService;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.adapter.in.web.dto.user.UserListResponse;
import com.kts.kronos.adapter.in.web.dto.user.UserSearchItemResponse;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.port.in.usecase.AuthUseCase;
import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
import com.kts.kronos.application.port.in.usecase.EmployeeUseCase;
import com.kts.kronos.application.port.in.usecase.UserUseCase;
import com.kts.kronos.application.port.out.provider.TokenBlacklistProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.observability.application.ObservabilityStatusUseCase;
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

import static org.mockito.ArgumentMatchers.any;
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

    @MockitoBean
    private TokenBlacklistProvider tokenBlacklistProvider;

    @MockitoBean
    private UserProvider userProvider;

    @MockitoBean
    private ObservabilityStatusUseCase observabilityStatusUseCase;

    @MockitoBean
    private AcceptTermsUseCase acceptTermsUseCase;

    @MockitoBean
    private AuthUseCase authUseCase;

    @MockitoBean
    private JwtAuthenticatedUser jwtAuthenticatedUser;

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
        when(userUseCase.listUsersResponse(true)).thenReturn(new UserListResponse(List.of(
                UserSearchItemResponse.fromDomain(manager, false)
        )));

        mockMvc.perform(get("/users/search")
                        .param("active", "true")
                        .with(user("partner").roles("PARTNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$.users[0].username").value("manager1"))
                .andExpect(jsonPath("$.users[0].role").value("MANAGER"))
                .andExpect(jsonPath("$.users[0].active").value(true))
                .andExpect(jsonPath("$.users[0].biometricConsentAccepted").value(false));
    }

    @Test
    void shouldExposeOnlyTheReducedPayloadOnUsersSearch() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        User user = new User(userId, "manager1", "encoded", Role.MANAGER, true, employeeId);
        when(userUseCase.listUsersResponse(null)).thenReturn(new UserListResponse(List.of(
                UserSearchItemResponse.fromDomain(user, true)
        )));

        // employeeId e biometricConsentAccepted são necessários no DTO resumido para o front
        // filtrar/linkar conta ao colaborador no /lista-colaboradores. Não são PII e o solicitante
        // já enxerga a mesma chave via /employee/. O teste segue garantindo que campos sensíveis
        // (password, sessionVersion, deletedAt, deletedBy, deactivationReason) NÃO sejam expostos.
        mockMvc.perform(get("/users/search")
                        .with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$.users[0].employeeId").value(employeeId.toString()))
                .andExpect(jsonPath("$.users[0].username").value("manager1"))
                .andExpect(jsonPath("$.users[0].role").value("MANAGER"))
                .andExpect(jsonPath("$.users[0].active").value(true))
                .andExpect(jsonPath("$.users[0].biometricConsentAccepted").value(true))
                .andExpect(jsonPath("$.users[0].password").doesNotExist())
                .andExpect(jsonPath("$.users[0].sessionVersion").doesNotExist())
                .andExpect(jsonPath("$.users[0].deletedAt").doesNotExist())
                .andExpect(jsonPath("$.users[0].deletedBy").doesNotExist())
                .andExpect(jsonPath("$.users[0].deactivationReason").doesNotExist());
    }
}
