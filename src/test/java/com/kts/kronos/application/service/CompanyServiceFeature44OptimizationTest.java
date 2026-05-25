package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.address.AddressRequest;
import com.kts.kronos.adapter.in.web.dto.address.UpdateAddressRequest;
import com.kts.kronos.adapter.in.web.dto.company.CreateCompanyRequest;
import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.adapter.in.web.dto.company.UpdateCompanyRequest;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ConflictException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.UserUseCase;
import com.kts.kronos.application.port.out.projection.CompanyEmployeeCountsProjection;
import com.kts.kronos.application.port.out.provider.AddressLookupProvider;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.application.security.AuthenticationRateLimitService;
import com.kts.kronos.observability.application.KronosMetrics;
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
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
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
    @Mock
    private AuthenticationRateLimitService authenticationRateLimitService;
    @Mock
    private KronosMetrics kronosMetrics;

    @Test
    @DisplayName("createCompany: cria empresa com endereço consultado")
    void shouldCreateCompanyWhenCnpjDoesNotExist() {
        CreateCompanyRequest request = new CreateCompanyRequest(
                "KTS",
                "12345678000199",
                "contato@kts.com",
                new AddressRequest("01001000", "123"),
                null,
                new Location(-23.55, -46.63)
        );

        when(companyProvider.existsByCnpj(request.cnpj())).thenReturn(false);
        when(viaCep.lookup("01001000")).thenReturn(new Address("Rua A", "0", "01001000", "Sao Paulo", "SP"));

        service.createCompany(request);

        verify(companyProvider).save(argThat(company ->
                company.name().equals("KTS")
                        && company.cnpj().equals("12345678000199")
                        && company.address().number().equals("123")
                        && company.location().latitude().equals(-23.55)
        ));
    }

    @Test
    @DisplayName("createCompany: rejeita CNPJ já cadastrado")
    void shouldRejectExistingCnpjWhenCreatingCompany() {
        CreateCompanyRequest request = new CreateCompanyRequest(
                "KTS",
                "12345678000199",
                "contato@kts.com",
                new AddressRequest("01001000", "123"),
                null,
                new Location(-23.55, -46.63)
        );

        when(companyProvider.existsByCnpj(request.cnpj())).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.createCompany(request));
        verify(companyProvider, never()).save(any());
    }

    @Test
    @DisplayName("createCompany: corrida de CNPJ duplicado deve virar 409")
    void shouldTranslateDuplicateCnpjRaceToConflict() {
        CreateCompanyRequest request = new CreateCompanyRequest(
                "KTS",
                "12345678000199",
                "contato@kts.com",
                new AddressRequest("01001000", "123"),
                null,
                new Location(-23.55, -46.63)
        );

        when(companyProvider.existsByCnpj(request.cnpj())).thenReturn(false);
        when(viaCep.lookup("01001000")).thenReturn(new Address("Rua A", "0", "01001000", "Sao Paulo", "SP"));
        doThrow(new DataIntegrityViolationException("duplicate key"))
                .when(companyProvider).save(any(Company.class));

        assertThrows(ConflictException.class, () -> service.createCompany(request));
    }

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
    @DisplayName("getCompany: falha quando CNPJ não existe")
    void shouldFailWhenGettingUnknownCompany() {
        when(companyProvider.findByCnpj("00000000000000")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getCompany("00000000000000"));
        verify(employeeProvider, never()).countByCompanyIds(any());
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
    @DisplayName("getCompanyNameById: retorna nome ou falha quando ausente")
    void shouldGetCompanyNameByIdOrFail() {
        UUID companyId = UUID.randomUUID();
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company(companyId, "KTS", true)));

        assertEquals("KTS", service.getCompanyNameById(companyId));

        UUID missingId = UUID.randomUUID();
        when(companyProvider.findById(missingId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getCompanyNameById(missingId));
    }

    @Test
    @DisplayName("updateCompany: atualiza campos básicos sem trocar endereço")
    void shouldUpdateCompanyWithoutAddressChange() {
        Company company = company(UUID.randomUUID(), "Old", true);
        when(companyProvider.findByCnpj(company.cnpj())).thenReturn(Optional.of(company));

        service.updateCompany(company.cnpj(), new UpdateCompanyRequest("New", "new@kts.com", false, null, null));

        verify(companyProvider).save(argThat(saved ->
                saved.name().equals("New")
                        && saved.email().equals("new@kts.com")
                        && !saved.active()
                        && saved.address().equals(company.address())
                        && saved.location().equals(company.location())
        ));
        verify(viaCep, never()).lookup(any());
    }

    @Test
    @DisplayName("updateCompany: exige geolocalização quando endereço muda")
    void shouldRequireLocationWhenUpdatingAddress() {
        Company company = company(UUID.randomUUID(), "KTS", true);
        when(companyProvider.findByCnpj(company.cnpj())).thenReturn(Optional.of(company));

        UpdateCompanyRequest request = new UpdateCompanyRequest(
                null,
                null,
                null,
                new UpdateAddressRequest("01001000", "500"),
                null
        );

        assertThrows(BadRequestException.class, () -> service.updateCompany(company.cnpj(), request));
        verify(companyProvider, never()).save(any());
    }

    @Test
    @DisplayName("updateCompany: troca endereço quando localização é informada")
    void shouldUpdateCompanyAddressWhenLocationIsProvided() {
        Company company = company(UUID.randomUUID(), "KTS", true);
        UpdateCompanyRequest request = new UpdateCompanyRequest(
                null,
                null,
                null,
                new UpdateAddressRequest("01001000", "500"),
                new Location(-22.9, -43.2)
        );

        when(companyProvider.findByCnpj(company.cnpj())).thenReturn(Optional.of(company));
        when(viaCep.lookup("01001000")).thenReturn(new Address("Rua Nova", "0", "01001000", "Rio", "RJ"));

        service.updateCompany(company.cnpj(), request);

        verify(companyProvider).save(argThat(saved ->
                saved.name().equals(company.name())
                        && saved.email().equals(company.email())
                        && saved.active() == company.active()
                        && saved.address().street().equals("Rua Nova")
                        && saved.address().number().equals("500")
                        && saved.location().latitude().equals(-22.9)
        ));
    }

    @Test
    @DisplayName("updateCompany: falha quando empresa não existe")
    void shouldFailWhenUpdatingUnknownCompany() {
        when(companyProvider.findByCnpj("00000000000000")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateCompany("00000000000000", new UpdateCompanyRequest(null, null, null, null, null)));
    }

    @Test
    @DisplayName("deleteByCnpj/cnpjExists: inativa empresa e preserva historico legal")
    void shouldDeactivateCompanyEmployeesAndUsers() {
        Company company = company(UUID.randomUUID(), "KTS", true);
        UUID employeeAId = UUID.randomUUID();
        UUID employeeBId = UUID.randomUUID();
        UUID userAId = UUID.randomUUID();
        UUID userBId = UUID.randomUUID();
        var employeeA = employee(employeeAId, company.companyId(), "Ana");
        var employeeB = employee(employeeBId, company.companyId(), "Bruno");
        var userA = new User(userAId, "ana", "x", Role.PARTNER, true, employeeAId);
        var userB = new User(userBId, "bruno", "x", Role.MANAGER, true, employeeBId);

        when(companyProvider.findByCnpj(company.cnpj())).thenReturn(Optional.of(company));
        when(employeeProvider.countByCompanyIds(Set.of(company.companyId()))).thenReturn(List.of());
        when(employeeProvider.findByCompanyId(company.companyId())).thenReturn(List.of(employeeA, employeeB));
        when(userProvider.findByEmployeeIds(Set.of(employeeAId, employeeBId))).thenReturn(List.of(userA, userB));
        when(companyProvider.existsByCnpj(company.cnpj())).thenReturn(true);

        service.deleteByCnpj(company.cnpj());

        verify(companyProvider).save(argThat(saved ->
                saved.companyId().equals(company.companyId())
                        && !saved.active()
                        && "COMPANY_DELETE".equals(saved.deactivationReason())
                        && saved.deletedAt() != null
        ));
        verify(employeeProvider).save(argThat(saved ->
                saved.employeeId().equals(employeeAId)
                        && !saved.active()
                        && "COMPANY_DELETE".equals(saved.deactivationReason())
        ));
        verify(employeeProvider).save(argThat(saved ->
                saved.employeeId().equals(employeeBId)
                        && !saved.active()
                        && "COMPANY_DELETE".equals(saved.deactivationReason())
        ));
        verify(userProvider).save(argThat(saved ->
                saved.userId().equals(userAId)
                        && !saved.active()
                        && "COMPANY_DELETE".equals(saved.deactivationReason())
        ));
        verify(userProvider).save(argThat(saved ->
                saved.userId().equals(userBId)
                        && !saved.active()
                        && "COMPANY_DELETE".equals(saved.deactivationReason())
        ));
        verify(companyProvider, never()).deleteByCnpj(company.cnpj());
        assertEquals(true, service.cnpjExists(company.cnpj()));
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
