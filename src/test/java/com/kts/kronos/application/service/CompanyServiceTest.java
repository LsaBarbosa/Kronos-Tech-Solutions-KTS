package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.address.AddressRequest;
import com.kts.kronos.adapter.in.web.dto.address.UpdateAddressRequest;
import com.kts.kronos.adapter.in.web.dto.company.CreateCompanyRequest;
import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.adapter.in.web.dto.company.UpdateCompanyRequest;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.AddressLookupProvider;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock CompanyProvider companyProvider;
    @Mock AddressLookupProvider viaCep;
    @Mock EmployeeProvider employeeProvider;
    @Mock UserProvider userProvider;

    @InjectMocks CompanyService service;

    @Test
    void createCompanyShouldSaveWhenCnpjDoesNotExist() {
        Address viaCepAddress = new Address("Rua A", "", "12345678", "Sao Paulo", "SP");
        CreateCompanyRequest request = new CreateCompanyRequest(
                "Kronos",
                "12345678000100",
                "mail@kts.com",
                new AddressRequest("12345678", "100"),
                null,
                new Location(-23.5, -46.6)
        );

        when(companyProvider.existsByCnpj(request.cnpj())).thenReturn(false);
        when(viaCep.lookup("12345678")).thenReturn(viaCepAddress);

        service.createCompany(request);

        ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
        verify(companyProvider).save(captor.capture());

        Company saved = captor.getValue();
        assertEquals("Kronos", saved.name());
        assertEquals("12345678000100", saved.cnpj());
        assertEquals("mail@kts.com", saved.email());
        assertEquals("100", saved.address().number());
        assertEquals(-23.5, saved.location().latitude());
    }

    @Test
    void createCompanyShouldThrowWhenCnpjAlreadyExists() {
        CreateCompanyRequest request = new CreateCompanyRequest(
                "Kronos",
                "12345678000100",
                "mail@kts.com",
                new AddressRequest("12345678", "100"),
                null,
                new Location(-23.5, -46.6)
        );

        when(companyProvider.existsByCnpj(request.cnpj())).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.createCompany(request));
        verify(companyProvider, never()).save(any());
        verify(viaCep, never()).lookup(any());
    }

    @Test
    void getCompanyShouldReturnWithEmployeeCounts() {
        String cnpj = "12345678000100";
        UUID companyId = UUID.randomUUID();
        Company company = company(companyId, true);

        when(companyProvider.findByCnpj(cnpj)).thenReturn(Optional.of(company));
        when(employeeProvider.countByCompanyIdAndActive(companyId, true)).thenReturn(10L);
        when(employeeProvider.countByCompanyIdAndActive(companyId, false)).thenReturn(2L);

        Company result = service.getCompany(cnpj);

        assertEquals(10L, result.activeEmployees());
        assertEquals(2L, result.inactiveEmployees());
    }

    @Test
    void getCompanyShouldThrowWhenNotFound() {
        when(companyProvider.findByCnpj("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getCompany("missing"));
    }

    @Test
    void listCompaniesShouldReturnEmptyWhenProviderHasNoData() {
        when(companyProvider.findAll()).thenReturn(List.of());

        List<Company> result = service.listCompanies(null);

        assertTrue(result.isEmpty());
        verify(employeeProvider, never()).countByCompanyIdsAndActive(any(), anyBoolean());
    }

    @Test
    void listCompaniesShouldLoadCountsUsingActiveFilter() {
        Company c1 = company(UUID.randomUUID(), true);
        Company c2 = company(UUID.randomUUID(), true);

        when(companyProvider.findByActive(true)).thenReturn(List.of(c1, c2));
        when(employeeProvider.countByCompanyIdsAndActive(List.of(c1.companyId(), c2.companyId()), true))
                .thenReturn(Map.of(c1.companyId(), 4L, c2.companyId(), 6L));
        when(employeeProvider.countByCompanyIdsAndActive(List.of(c1.companyId(), c2.companyId()), false))
                .thenReturn(Map.of(c1.companyId(), 1L));

        List<Company> result = service.listCompanies(true);

        assertEquals(2, result.size());
        assertEquals(4L, result.get(0).activeEmployees());
        assertEquals(1L, result.get(0).inactiveEmployees());
        assertEquals(6L, result.get(1).activeEmployees());
        assertEquals(0L, result.get(1).inactiveEmployees());
    }

    @Test
    void getCompanyNameByIdReturnsNameFromProvider() {
        UUID companyId = UUID.randomUUID();
        Company company = company(companyId, true);
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));

        assertEquals("Kronos", service.getCompanyNameById(companyId));
    }

    @Test
    void updateCompanyShouldUpdateAllProvidedFields() {
        String cnpj = "12345678000100";
        Company current = company(UUID.randomUUID(), true);
        UpdateCompanyRequest request = new UpdateCompanyRequest(
                "Novo Nome",
                "novo@kts.com",
                false,
                new UpdateAddressRequest("87654321", "200"),
                new Location(-10.0, -20.0)
        );
        when(companyProvider.findByCnpj(cnpj)).thenReturn(Optional.of(current));
        when(viaCep.lookup("87654321")).thenReturn(new Address("Rua B", "0", "87654321", "RJ", "RJ"));

        service.updateCompany(cnpj, request);

        ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
        verify(companyProvider).save(captor.capture());

        Company saved = captor.getValue();
        assertEquals("Novo Nome", saved.name());
        assertEquals("novo@kts.com", saved.email());
        assertFalse(saved.active());
        assertEquals("200", saved.address().number());
        assertEquals(-10.0, saved.location().latitude());
    }

    @Test
    void updateCompanyShouldKeepOldValuesWhenFieldsAreNull() {
        String cnpj = "12345678000100";
        Company current = company(UUID.randomUUID(), true);
        UpdateCompanyRequest request = new UpdateCompanyRequest(null, null, null, null, null);
        when(companyProvider.findByCnpj(cnpj)).thenReturn(Optional.of(current));

        service.updateCompany(cnpj, request);

        ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
        verify(companyProvider).save(captor.capture());
        Company saved = captor.getValue();

        assertEquals(current.name(), saved.name());
        assertEquals(current.email(), saved.email());
        assertEquals(current.active(), saved.active());
        assertEquals(current.address(), saved.address());
        assertEquals(current.location(), saved.location());
        verify(viaCep, never()).lookup(any());
    }

    @Test
    void updateCompanyShouldThrowWhenAddressProvidedWithoutLocation() {
        String cnpj = "12345678000100";
        Company current = company(UUID.randomUUID(), true);
        UpdateCompanyRequest request = new UpdateCompanyRequest(
                "Novo Nome",
                "novo@kts.com",
                true,
                new UpdateAddressRequest("87654321", "200"),
                null
        );
        when(companyProvider.findByCnpj(cnpj)).thenReturn(Optional.of(current));

        assertThrows(BadRequestException.class, () -> service.updateCompany(cnpj, request));
        verify(companyProvider, never()).save(any());
    }

    @Test
    void toggleActivateShouldUpdateCompanyEmployeesAndUsers() {
        String cnpj = "12345678000100";
        UUID companyId = UUID.randomUUID();
        Company current = company(companyId, true);

        UUID employee1Id = UUID.randomUUID();
        UUID employee2Id = UUID.randomUUID();
        Employee employee1 = employee(employee1Id, companyId);
        Employee employee2 = employee(employee2Id, companyId);

        User userNeedsUpdate = new User(UUID.randomUUID(), "user1", "pwd", Role.MANAGER, true, employee1Id);
        User userAlreadyInTargetStatus = new User(UUID.randomUUID(), "user2", "pwd", Role.MANAGER, false, employee2Id);

        when(companyProvider.findByCnpj(cnpj)).thenReturn(Optional.of(current));
        when(employeeProvider.countByCompanyIdAndActive(companyId, true)).thenReturn(2L);
        when(employeeProvider.countByCompanyIdAndActive(companyId, false)).thenReturn(0L);
        when(employeeProvider.updateActiveByCompanyId(companyId, false)).thenReturn(2);
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of(employee1, employee2));
        when(userProvider.findByEmployeeIdIn(List.of(employee1Id, employee2Id)))
                .thenReturn(List.of(userNeedsUpdate, userAlreadyInTargetStatus));

        service.toggleActivate(cnpj);

        verify(companyProvider).save(eq(current.withEmployeeCounts(2L, 0L).withActive(false)));
        verify(userProvider).save(eq(userNeedsUpdate.withActive(false)));
        verify(userProvider, times(1)).save(any(User.class));
    }

    @Test
    void deleteByCnpjShouldDeleteWhenExists() {
        when(companyProvider.existsByCnpj("123")).thenReturn(true);

        service.deleteByCnpj("123");

        verify(companyProvider).deleteByCnpj("123");
    }

    @Test
    void deleteByCnpjShouldThrowWhenCompanyDoesNotExist() {
        when(companyProvider.existsByCnpj("missing")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.deleteByCnpj("missing"));
        verify(companyProvider, never()).deleteByCnpj(any());
    }

    @Test
    void cnpjExistsShouldDelegateToProvider() {
        when(companyProvider.existsByCnpj("123")).thenReturn(true);

        assertTrue(service.cnpjExists("123"));
    }

    private Company company(UUID companyId, boolean active) {
        return new Company(companyId, "Kronos", "12345678000100", "mail@kts.com", active,
                new Address("Rua A", "1", "12345678", "SP", "SP"),
                new Location(1.0, 2.0), 0, 0);
    }

    private Employee employee(UUID employeeId, UUID companyId) {
        return new Employee(
                employeeId,
                "Funcionario",
                "12345678901",
                "12345678901",
                "Dev",
                "func@kts.com",
                1000.0,
                "11999999999",
                true,
                new Address("Rua A", "1", "12345678", "SP", "SP"),
                companyId,
                LocalDateTime.now(),
                false,
                null,
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0),
                null,
                null,
                null,
                null,
                null
        );
    }
}
