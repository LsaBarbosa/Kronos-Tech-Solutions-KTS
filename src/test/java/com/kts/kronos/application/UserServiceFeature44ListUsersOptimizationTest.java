package com.kts.kronos.application;

import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.port.in.usecase.EmployeeUseCase;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.application.service.UserService;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceFeature44ListUsersOptimizationTest {

    @InjectMocks
    private UserService service;

    @Mock
    private UserProvider userProvider;
    @Mock
    private DocumentProvider documentProvider;
    @Mock
    private TimeRecordProvider timeRecordProvider;
    @Mock
    private EmployeeProvider employeeProvider;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtAuthenticatedUser jwtAuthenticatedUser;
    @Mock
    private EmployeeUseCase employeeUseCase;
    @Mock
    private DomainAuthorizationService domainAuthorizationService;

    @Test
    @DisplayName("listUsers: manager usa busca em lote por employeeIds quando active não é informado")
    void shouldBatchLoadUsersForTenantWithoutActiveFilter() {
        UUID managerEmployeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID employeeAId = UUID.randomUUID();
        UUID employeeBId = UUID.randomUUID();

        Employee manager = employee(managerEmployeeId, companyId, "Manager");
        Employee employeeA = employee(employeeAId, companyId, "Ana");
        Employee employeeB = employee(employeeBId, companyId, "Bruno");

        User userA = new User(UUID.randomUUID(), "ana", "x", Role.PARTNER, true, employeeAId);
        User userB = new User(UUID.randomUUID(), "bruno", "x", Role.MANAGER, true, employeeBId);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(manager));
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of(employeeA, employeeB));
        when(userProvider.findByEmployeeIds(Set.of(employeeAId, employeeBId))).thenReturn(List.of(userA, userB));

        var result = service.listUsers(null);

        assertEquals(List.of(userA, userB), result);
        verify(userProvider).findByEmployeeIds(Set.of(employeeAId, employeeBId));
        verify(userProvider, never()).findAll();
        verify(userProvider, never()).findByActive(true);
        verify(userProvider, never()).findByActive(false);
    }

    @Test
    @DisplayName("listUsers: manager usa busca em lote com filtro active")
    void shouldBatchLoadUsersForTenantWithActiveFilter() {
        UUID managerEmployeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID employeeAId = UUID.randomUUID();
        UUID employeeBId = UUID.randomUUID();

        Employee manager = employee(managerEmployeeId, companyId, "Manager");
        Employee employeeA = employee(employeeAId, companyId, "Ana");
        Employee employeeB = employee(employeeBId, companyId, "Bruno");

        User activeUser = new User(UUID.randomUUID(), "ana", "x", Role.PARTNER, true, employeeAId);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(manager));
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of(employeeA, employeeB));
        when(userProvider.findByEmployeeIdsAndActive(Set.of(employeeAId, employeeBId), true))
                .thenReturn(List.of(activeUser));

        var result = service.listUsers(true);

        assertEquals(List.of(activeUser), result);
        verify(userProvider).findByEmployeeIdsAndActive(Set.of(employeeAId, employeeBId), true);
        verify(userProvider, never()).findAll();
        verify(userProvider, never()).findByActive(true);
    }

    @Test
    @DisplayName("listUsers: manager retorna vazio quando empresa não possui funcionários")
    void shouldReturnEmptyWhenTenantHasNoEmployees() {
        UUID managerEmployeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee manager = employee(managerEmployeeId, companyId, "Manager");

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(manager));
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of());

        var result = service.listUsers(false);

        assertEquals(List.of(), result);
        verify(userProvider, never()).findByEmployeeIdsAndActive(java.util.Set.of(), false);
        verify(userProvider, never()).findByEmployeeIds(java.util.Set.of());
    }

    @Test
    @DisplayName("listUsers: CTO sem filtro active usa findAll")
    void shouldUseFindAllWhenCtoHasNoActiveFilter() {
        UUID ctoEmployeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee ctoEmployee = employee(ctoEmployeeId, companyId, "CTO");

        User userA = new User(UUID.randomUUID(), "a", "x", Role.MANAGER, true, UUID.randomUUID());
        User userB = new User(UUID.randomUUID(), "b", "x", Role.PARTNER, false, UUID.randomUUID());

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(ctoEmployeeId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(employeeProvider.findById(ctoEmployeeId)).thenReturn(Optional.of(ctoEmployee));
        when(userProvider.findAll()).thenReturn(List.of(userA, userB));

        var result = service.listUsers(null);

        assertEquals(List.of(userA, userB), result);
        verify(userProvider).findAll();
        verify(userProvider, never()).findByActive(true);
    }

    @Test
    @DisplayName("listUsers: CTO com filtro active usa findByActive")
    void shouldUseFindByActiveWhenCtoFiltersByStatus() {
        UUID ctoEmployeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee ctoEmployee = employee(ctoEmployeeId, companyId, "CTO");

        User activeUser = new User(UUID.randomUUID(), "a", "x", Role.MANAGER, true, UUID.randomUUID());

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(ctoEmployeeId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(employeeProvider.findById(ctoEmployeeId)).thenReturn(Optional.of(ctoEmployee));
        when(userProvider.findByActive(true)).thenReturn(List.of(activeUser));

        var result = service.listUsers(true);

        assertEquals(List.of(activeUser), result);
        verify(userProvider).findByActive(true);
        verify(userProvider, never()).findAll();
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
}
