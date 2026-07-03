package com.kts.kronos.application.service;

import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.UserUseCase;
import com.kts.kronos.application.port.out.provider.AddressLookupProvider;
import com.kts.kronos.application.port.out.provider.CacheProvider;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.application.security.AuthenticationRateLimitService;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.observability.application.KronosMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyToggleActivateServiceTest {

    @InjectMocks private CompanyService service;

    @Mock private CompanyProvider companyProvider;
    @Mock private EmployeeProvider employeeProvider;
    @Mock private UserProvider userProvider;
    @Mock private UserUseCase userUseCase;
    @Mock private AddressLookupProvider viaCep;
    @Mock private JwtAuthenticatedUser jwtAuthenticatedUser;
    @Mock private AuthenticationRateLimitService authenticationRateLimitService;
    @Mock private KronosMetrics kronosMetrics;
    @Mock private CacheProvider cacheProvider;
    @Mock private CompanyHardDeleteService companyHardDeleteService;

    @Test
    void toggleActivate_empresa_ativa_desativa_employees_e_propaga_para_users() {
        UUID companyId = UUID.randomUUID();
        UUID empId1 = UUID.randomUUID();
        UUID empId2 = UUID.randomUUID();
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        Company activeCompany = company(companyId, true);
        Employee emp1 = employee(empId1, companyId);
        Employee emp2 = employee(empId2, companyId);
        User user1 = user(userId1, empId1, true);
        User user2 = user(userId2, empId2, true);

        when(companyProvider.findByCnpj("12345678000199")).thenReturn(Optional.of(activeCompany));
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of(emp1, emp2));
        when(userProvider.findByEmployeeIds(Set.of(empId1, empId2))).thenReturn(List.of(user1, user2));

        service.toggleActivate("12345678000199");

        // Empresa salva como inativa
        verify(companyProvider).save(argThat(c -> !c.active()));
        // Propaga para os dois usuários
        verify(userUseCase).toggleActivate(userId1);
        verify(userUseCase).toggleActivate(userId2);
    }

    @Test
    void toggleActivate_empresa_inativa_reativa_employees_e_users() {
        UUID companyId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Company inactiveCompany = company(companyId, false);
        Employee emp = employee(empId, companyId);
        User inactiveUser = user(userId, empId, false);

        when(companyProvider.findByCnpj("12345678000199")).thenReturn(Optional.of(inactiveCompany));
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of(emp));
        when(userProvider.findByEmployeeIds(Set.of(empId))).thenReturn(List.of(inactiveUser));

        service.toggleActivate("12345678000199");

        // Empresa salva como ativa
        verify(companyProvider).save(argThat(Company::active));
        // User inativo deve ser reativado
        verify(userUseCase).toggleActivate(userId);
    }

    @Test
    void toggleActivate_empresa_sem_employees_nao_propaga_para_users() {
        UUID companyId = UUID.randomUUID();
        Company activeCompany = company(companyId, true);

        when(companyProvider.findByCnpj("12345678000199")).thenReturn(Optional.of(activeCompany));
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of());

        service.toggleActivate("12345678000199");

        verify(companyProvider).save(argThat(c -> !c.active()));
        verifyNoInteractions(userProvider, userUseCase);
    }

    @Test
    void toggleActivate_cnpj_inexistente_lanca_not_found() {
        when(companyProvider.findByCnpj("00000000000000")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.toggleActivate("00000000000000"));

        verify(companyProvider, never()).save(any());
        verifyNoInteractions(employeeProvider, userProvider, userUseCase);
    }

    // --- helpers ---

    private Company company(UUID id, boolean active) {
        return new Company(id, "Empresa Teste", "12345678000199",
                "teste@kts.com", active,
                new Address("Rua A", "1", "01001000", "São Paulo", "SP"),
                null, 0L, 0L);
    }

    private Employee employee(UUID empId, UUID companyId) {
        return new Employee(empId, "Colaborador", "12345678901", "12345678901",
                "Dev", "dev@kts.com", 5000.0, "11999999999",
                true, null, companyId, null, false,
                null, null, null, null, null, null, null, null, null, null);
    }

    private User user(UUID userId, UUID empId, boolean active) {
        return new User(userId, "user@kts.com", "hash", Role.PARTNER, active, empId);
    }
}
