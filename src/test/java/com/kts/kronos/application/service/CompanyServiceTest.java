package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.address.AddressRequest;
import com.kts.kronos.adapter.in.web.dto.company.CreateCompanyRequest;
import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.application.exceptions.BadRequestException;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock CompanyProvider companyProvider;
    @Mock AddressLookupProvider viaCep;
    @Mock EmployeeProvider employeeProvider;
    @Mock UserProvider userProvider;

    @InjectMocks CompanyService service;

    @Test
    void getCompanyNameByIdReturnsNameFromProvider() {
        UUID companyId = UUID.randomUUID();
        Company company = new Company(companyId, "Kronos", "12345678000100", "mail@kts.com", true,
                new Address("A", "1", "12345678", "SP", "SP"), new Location(1.0, 2.0), 1, 0);
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));

        assertEquals("Kronos", service.getCompanyNameById(companyId));
    }

    @Test
    void createCompanyThrowsWhenCnpjAlreadyExists() {
        when(companyProvider.existsByCnpj("12345678000100")).thenReturn(true);

        CreateCompanyRequest req = new CreateCompanyRequest(
                "KTS",
                "12345678000100",
                "mail@kts.com",
                new AddressRequest("12345678", "10"),
                null,
                new Location(1.0, 2.0)
        );

        assertThrows(BadRequestException.class, () -> service.createCompany(req));
    }

    @Test
    void createCompanySavesWithLookedUpAddress() {
        when(companyProvider.existsByCnpj("12345678000100")).thenReturn(false);
        when(viaCep.lookup("12345678")).thenReturn(new Address("Rua", "0", "12345678", "Cidade", "ST"));

        CreateCompanyRequest req = new CreateCompanyRequest(
                "KTS",
                "12345678000100",
                "mail@kts.com",
                new AddressRequest("12345678", "10"),
                null,
                new Location(1.0, 2.0)
        );

        service.createCompany(req);

        verify(companyProvider).save(argThat(c -> c.address().street().equals("Rua") && c.address().number().equals("10")));
    }

    @Test
    void listCompaniesReturnsWithEmployeeCounts() {
        UUID companyId = UUID.randomUUID();
        Company company = new Company(companyId, "KTS", "12345678000100", "mail@kts.com", true,
                new Address("Rua", "1", "12345678", "Cidade", "ST"), new Location(1.0, 2.0), 0, 0);

        when(companyProvider.findAll()).thenReturn(List.of(company));
        when(employeeProvider.countByCompanyIdsAndActive(List.of(companyId), true)).thenReturn(Map.of(companyId, 5L));
        when(employeeProvider.countByCompanyIdsAndActive(List.of(companyId), false)).thenReturn(Map.of(companyId, 2L));

        var result = service.listCompanies(null);

        assertEquals(5L, result.get(0).activeEmployees());
        assertEquals(2L, result.get(0).inactiveEmployees());
    }

    @Test
    void toggleActivateAlsoUpdatesUsersFromCompany() {
        UUID companyId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Company company = new Company(companyId, "KTS", "12345678000100", "mail@kts.com", true,
                new Address("Rua", "1", "12345678", "Cidade", "ST"), new Location(1.0, 2.0), 0, 0);
        Employee employee = mock(Employee.class);
        when(employee.employeeId()).thenReturn(empId);
        User user = new User(userId, "john", "hash", Role.PARTNER, true, empId);

        when(companyProvider.findByCnpj("12345678000100")).thenReturn(Optional.of(company));
        when(employeeProvider.countByCompanyIdAndActive(companyId, true)).thenReturn(1L);
        when(employeeProvider.countByCompanyIdAndActive(companyId, false)).thenReturn(0L);
        when(employeeProvider.updateActiveByCompanyId(companyId, false)).thenReturn(1);
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of(employee));
        when(userProvider.findByEmployeeIdIn(List.of(empId))).thenReturn(List.of(user));

        service.toggleActivate("12345678000100");

        verify(companyProvider).save(argThat(c -> !c.active()));
        verify(userProvider).save(argThat(u -> !u.active()));
    }
}
