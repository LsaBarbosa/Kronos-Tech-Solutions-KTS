package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.address.AddressRequest;
import com.kts.kronos.adapter.in.web.dto.address.UpdateAddressRequest;
import com.kts.kronos.adapter.in.web.dto.employee.CreateEmployeeRequest;
import com.kts.kronos.adapter.in.web.dto.employee.UpdateEmployeeManagerRequest;
import com.kts.kronos.adapter.in.web.dto.employee.UpdateEmployeePartnerRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.AddressLookupProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.CPF_ALREADY_EXIST;
import static com.kts.kronos.constants.Messages.EMPLOYEE_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeService Unit Tests")
class EmployeeServiceTest {

    @Mock
    private EmployeeProvider employeeProvider;
    @Mock
    private AddressLookupProvider viaCep;
    @Mock
    private JwtAuthenticatedUser jwtAuthenticatedUser;
    @InjectMocks
    private EmployeeService employeeService;

    private UUID employeeId;
    private UUID companyId;
    private Employee employee;
    private Address address;

    @BeforeEach
    void setUp() {
        employeeId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        address = new Address("Street Test", "123", "12345678", "City Test", "State Test");
        employee = new Employee(employeeId, "Test Employee", "12345678901", "Developer", "test@test.com", 5000.0, "11999999999", true, address, companyId);
    }

    @Nested
    @DisplayName("Manager Operations")
    class ManagerTests {
        @Test
        @DisplayName("Should create an employee successfully")
        void createEmployee_Success() {
            var managerEmployee = new Employee(UUID.randomUUID(), "Manager", "11111111111", "Manager", "manager@test.com", 8000.0, "11999999999", true, address, companyId);
            var request = new CreateEmployeeRequest(
                    "New Employee",
                    "98765432101",
                    "Analyst",
                    "new@test.com",
                    4000.0,
                    "11888888888",
                    new AddressRequest("87654321", "456")
            );

            when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployee.employeeId());
            when(employeeProvider.findById(managerEmployee.employeeId())).thenReturn(Optional.of(managerEmployee));
            when(employeeProvider.findByCpf(request.cpf())).thenReturn(Optional.empty());
            when(viaCep.lookup(request.address().postalCode())).thenReturn(address);
            doNothing().when(employeeProvider).save(any(Employee.class));

            assertDoesNotThrow(() -> employeeService.createEmployee(request));

            verify(employeeProvider, times(1)).save(any(Employee.class));
        }

        @Test
        @DisplayName("Should throw BadRequestException if CPF already exists")
        void createEmployee_CpfAlreadyExists_ThrowsException() {
            var managerEmployee = new Employee(UUID.randomUUID(), "Manager", "11111111111", "Manager", "manager@test.com", 8000.0, "11999999999", true, address, companyId);
            var request = new CreateEmployeeRequest(
                    "New Employee",
                    employee.cpf(),
                    "Analyst",
                    "new@test.com",
                    4000.0,
                    "11888888888",
                    new AddressRequest("87654321", "456")
            );

            when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployee.employeeId());
            when(employeeProvider.findById(managerEmployee.employeeId())).thenReturn(Optional.of(managerEmployee));
            when(employeeProvider.findByCpf(request.cpf())).thenReturn(Optional.of(employee));

            BadRequestException thrown = assertThrows(BadRequestException.class, () -> employeeService.createEmployee(request));

            assertEquals(CPF_ALREADY_EXIST, thrown.getMessage());
            verify(employeeProvider, never()).save(any(Employee.class));
        }

        @Test
        @DisplayName("Should return a list of all employees when active is null")
        void listEmployees_ActiveIsNull_ReturnsAllEmployees() {
            when(employeeProvider.findAll()).thenReturn(Collections.singletonList(employee));

            List<Employee> employees = employeeService.listEmployees(null);

            assertNotNull(employees);
            assertEquals(1, employees.size());
            verify(employeeProvider, times(1)).findAll();
        }

        @Test
        @DisplayName("Should return an employee by ID")
        void getEmployee_Success() {
            when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));

            Employee foundEmployee = employeeService.getEmployee(employeeId);

            assertNotNull(foundEmployee);
            assertEquals(employee, foundEmployee);
            verify(employeeProvider, times(1)).findById(employeeId);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when employee is not found")
        void getEmployee_NotFound_ThrowsException() {
            when(employeeProvider.findById(employeeId)).thenReturn(Optional.empty());

            ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class, () -> employeeService.getEmployee(employeeId));

            assertEquals(EMPLOYEE_NOT_FOUND + employeeId, thrown.getMessage());
            verify(employeeProvider, times(1)).findById(employeeId);
        }

        @Test
        @DisplayName("Should update an employee successfully")
        void updateEmployee_Success() {
            var updateRequest = new UpdateEmployeeManagerRequest(
                    "Updated Name",
                    null,
                    "New Job",
                    "updated@test.com",
                    null,
                    "11777777777",
                    null
            );

            when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
            doNothing().when(employeeProvider).save(any(Employee.class));

            assertDoesNotThrow(() -> employeeService.updateEmployee(employeeId, updateRequest));

            verify(employeeProvider, times(1)).save(any(Employee.class));
        }

        @Test
        @DisplayName("Should delete an employee successfully")
        void deleteEmployee_Success() {
            when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
            doNothing().when(employeeProvider).deleteById(employeeId);

            assertDoesNotThrow(() -> employeeService.deleteEmployee(employeeId));

            verify(employeeProvider, times(1)).deleteById(employeeId);
        }
    }

    @Nested
    @DisplayName("Partner Operations")
    class PartnerTests {
        @Test
        @DisplayName("Should get own profile successfully")
        void getOwnProfile_Success() {
            when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
            when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));

            Employee ownProfile = employeeService.getOwnProfile();

            assertNotNull(ownProfile);
            assertEquals(employeeId, ownProfile.employeeId());
            verify(employeeProvider, times(1)).findById(employeeId);
        }

        @Test
        @DisplayName("Should update own profile successfully")
        void updateOwnProfile_Success() {
            var updateRequest = new UpdateEmployeePartnerRequest(
                    "updated_partner@test.com",
                    "11988888888",
                    null
            );

            when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
            when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
            doNothing().when(employeeProvider).save(any(Employee.class));

            assertDoesNotThrow(() -> employeeService.updateOwnProfile(updateRequest));

            verify(employeeProvider, times(1)).save(any(Employee.class));
        }

        @Test
        @DisplayName("Should update own profile with new address")
        void updateOwnProfile_WithNewAddress_Success() {
            var newAddressRequest = new UpdateAddressRequest("99999999", "789");
            var updateRequest = new UpdateEmployeePartnerRequest(
                    null,
                    null,
                    newAddressRequest
            );
            var newAddress = new Address("New Street", "789", "99999999", "New City", "New State");

            when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
            when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
            when(viaCep.lookup(newAddressRequest.postalCode())).thenReturn(newAddress);
            doNothing().when(employeeProvider).save(any(Employee.class));

            assertDoesNotThrow(() -> employeeService.updateOwnProfile(updateRequest));

            verify(viaCep, times(1)).lookup(newAddressRequest.postalCode());
            verify(employeeProvider, times(1)).save(any(Employee.class));
        }
    }
}