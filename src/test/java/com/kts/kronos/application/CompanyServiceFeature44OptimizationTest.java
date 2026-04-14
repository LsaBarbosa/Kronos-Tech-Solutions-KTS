package com.kts.kronos.application;

import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.application.port.in.usecase.UserUseCase;
import com.kts.kronos.application.port.out.projection.CompanyEmployeeCountsProjection;
import com.kts.kronos.application.port.out.provider.AddressLookupProvider;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.application.service.CompanyService;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceFeature44OptimizationTest {

    @InjectMocks
    private CompanyService service;

    @Mock
    private CompanyProvider companyProvider;
    @Mock
    private AddressLookupProvider viaCep;
    @Mock
    private EmployeeProvider employeeProvider;
    @Mock
    private UserProvider userProvider;
    @Mock
    private UserUseCase userUseCase;

    @Test
    @DisplayName("listCompanies: consolida contagens por empresa em uma única consulta agregada")
    void shouldUseAggregatedCountsWhenListingCompanies() {
        UUID companyAId = UUID.randomUUID();
        UUID companyBId = UUID.randomUUID();

        Company companyA = company(companyAId, "Company A", true);
        Company companyB = company(companyBId, "Company B", true);

        when(companyProvider.findAll()).thenReturn(List.of(companyA, companyB));
        when(employeeProvider.countByCompanyIds(Set.of(companyAId, companyBId))).thenReturn(List.of(
                projection(companyAId, 8L, 2L),
                projection(companyBId, 3L, 1L)
        ));

        var result = service.listCompanies(null);

        assertEquals(2, result.size());
        assertEquals(8L, result.get(0).activeEmployees());
        assertEquals(2L, result.get(0).inactiveEmployees());
        assertEquals(3L, result.get(1).activeEmployees());
        assertEquals(1L, result.get(1).inactiveEmployees());
        verify(employeeProvider).countByCompanyIds(Set.of(companyAId, companyBId));
        verify(employeeProvider, never()).countByCompanyIdAndActive(any(), anyBoolean());
    }

    @Test
    @DisplayName("listCompanies: com filtro active usa provider filtrado e trata lista vazia sem consulta agregada")
    void shouldHandleActiveFilterAndEmptyCompanies() {
        when(companyProvider.findByActive(true)).thenReturn(List.of());

        var result = service.listCompanies(true);

        assertEquals(0, result.size());
        verify(companyProvider).findByActive(true);
        verify(employeeProvider, never()).countByCompanyIds(any());
    }

    @Test
    @DisplayName("toggleActivate: carrega usuários por lote e evita findByEmployeeId em loop")
    void shouldBatchLoadUsersWhenTogglingCompanyActivation() {
        UUID companyId = UUID.randomUUID();
        UUID employeeAId = UUID.randomUUID();
        UUID employeeBId = UUID.randomUUID();
        UUID userAId = UUID.randomUUID();
        UUID userBId = UUID.randomUUID();

        Company company = company(companyId, "KTS", true);
        Employee employeeA = employee(employeeAId, companyId, "Ana");
        Employee employeeB = employee(employeeBId, companyId, "Bruno");

        User userA = new User(userAId, "ana", "x", Role.PARTNER, true, employeeAId);
        User userB = new User(userBId, "bruno", "x", Role.PARTNER, false, employeeBId);

        when(companyProvider.findByCnpj(company.cnpj())).thenReturn(Optional.of(company));
        when(employeeProvider.countByCompanyIds(Set.of(companyId))).thenReturn(List.of(
                projection(companyId, 2L, 0L)
        ));
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of(employeeA, employeeB));
        when(userProvider.findByEmployeeIds(Set.of(employeeAId, employeeBId))).thenReturn(List.of(userA, userB));

        service.toggleActivate(company.cnpj());

        verify(companyProvider).save(argThat(savedCompany ->
                savedCompany.companyId().equals(companyId) && !savedCompany.active()
        ));
        verify(userProvider).findByEmployeeIds(Set.of(employeeAId, employeeBId));
        verify(userProvider, never()).findByEmployeeId(any());
        verify(userUseCase).toggleActivate(userAId);
        verify(userUseCase, never()).toggleActivate(userBId);
    }

    @Test
    @DisplayName("toggleActivate: cobre cenário de empresa inativa voltando a ativa")
    void shouldToggleInactiveCompanyBackToActive() {
        UUID companyId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Company company = company(companyId, "KTS", false);
        Employee employee = employee(employeeId, companyId, "Ana");
        User user = new User(userId, "ana", "x", Role.PARTNER, false, employeeId);

        when(companyProvider.findByCnpj(company.cnpj())).thenReturn(Optional.of(company));
        when(employeeProvider.countByCompanyIds(Set.of(companyId))).thenReturn(List.of(
                projection(companyId, 1L, 1L)
        ));
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of(employee));
        when(userProvider.findByEmployeeIds(Set.of(employeeId))).thenReturn(List.of(user));

        service.toggleActivate(company.cnpj());

        verify(companyProvider).save(argThat(savedCompany ->
                savedCompany.companyId().equals(companyId) && savedCompany.active()
        ));
        verify(userUseCase).toggleActivate(userId);
    }

    @Test
    @DisplayName("toggleActivate: quando empresa não possui funcionários, retorna sem buscar usuários")
    void shouldReturnEarlyWhenCompanyHasNoEmployees() {
        UUID companyId = UUID.randomUUID();
        Company company = company(companyId, "KTS", true);

        when(companyProvider.findByCnpj(company.cnpj())).thenReturn(Optional.of(company));
        when(employeeProvider.countByCompanyIds(Set.of(companyId))).thenReturn(List.of(
                projection(companyId, null, null)
        ));
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of());

        service.toggleActivate(company.cnpj());

        verify(userProvider, never()).findByEmployeeIds(any());
        verify(userUseCase, never()).toggleActivate(any());
    }

    @Test
    @DisplayName("getCompany: normaliza contagens nulas para zero")
    void shouldNormalizeNullCountsToZeroWhenGettingCompany() {
        UUID companyId = UUID.randomUUID();
        Company company = company(companyId, "KTS", true);

        when(companyProvider.findByCnpj(company.cnpj())).thenReturn(Optional.of(company));
        when(employeeProvider.countByCompanyIds(Set.of(companyId))).thenReturn(List.of(
                projection(companyId, null, null)
        ));

        var result = service.getCompany(company.cnpj());

        assertEquals(0L, result.activeEmployees());
        assertEquals(0L, result.inactiveEmployees());
    }

    @Test
    @DisplayName("loadEmployeeCountsByCompanyIds: com entrada nula retorna mapa vazio")
    void shouldReturnEmptyMapWhenCompanyIdsIsNull() throws Exception {
        var method = CompanyService.class.getDeclaredMethod("loadEmployeeCountsByCompanyIds", Set.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<UUID, long[]> result = (Map<UUID, long[]>) method.invoke(service, new Object[]{null});

        assertEquals(Map.of(), result);
        verify(employeeProvider, never()).countByCompanyIds(any());
    }

    private Company company(UUID companyId, String name, boolean active) {
        return new Company(
                companyId,
                name,
                "12345678000199",
                name.toLowerCase().replace(" ", "") + "@example.com",
                active,
                new Address("Rua A", "100", "01001000", "Sao Paulo", "SP"),
                new Location(-23.55, -46.63),
                0L,
                0L
        );
    }

    private Employee employee(UUID employeeId, UUID companyId, String name) {
        return new Employee(
                employeeId,
                name,
                "12345678901",
                "12345678901",
                "Developer",
                name.toLowerCase() + "@kts.com",
                5000.0,
                "21999999999",
                true,
                null,
                companyId,
                null,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private CompanyEmployeeCountsProjection projection(UUID companyId, Long active, Long inactive) {
        return new CompanyEmployeeCountsProjection() {
            @Override
            public UUID getCompanyId() {
                return companyId;
            }

            @Override
            public Long getActiveCount() {
                return active;
            }

            @Override
            public Long getInactiveCount() {
                return inactive;
            }
        };
    }
}
