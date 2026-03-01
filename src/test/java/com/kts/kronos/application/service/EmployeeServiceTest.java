package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.address.UpdateAddressRequest;
import com.kts.kronos.adapter.in.web.dto.employee.UpdateEmployeePartnerRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock EmployeeProvider employeeProvider;
    @Mock AddressLookupProvider viaCep;
    @Mock JwtAuthenticatedUser jwtAuthenticatedUser;
    @Mock UserProvider userProvider;
    @Mock FaceStorageProvider faceStorageProvider;
    @Mock FaceRecognitionProvider faceRecognitionProvider;

    @InjectMocks EmployeeService service;

    @Test
    void getOwnProfileReturnsEmployeeAndRole() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = sampleEmployee(employeeId, UUID.randomUUID(), true);
        User user = new User(UUID.randomUUID(), "john", "hash", Role.MANAGER, true, employeeId);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.of(user));

        var profile = service.getOwnProfile();

        assertEquals(employeeId, profile.employee().employeeId());
        assertEquals("MANAGER", profile.role());
    }

    @Test
    void getOwnProfileThrowsWhenUserNotLinked() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = sampleEmployee(employeeId, UUID.randomUUID(), true);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getOwnProfile());
    }

    @Test
    void updateOwnProfileUpdatesAddressAndContactData() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = sampleEmployee(employeeId, UUID.randomUUID(), true);
        Address lookedUp = new Address("Rua Nova", "10", "12345678", "Cidade", "ST");

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(viaCep.lookup("12345678")).thenReturn(lookedUp);

        service.updateOwnProfile(new UpdateEmployeePartnerRequest("novo@kts.com", "11999999999", new UpdateAddressRequest("12345678", "99")));

        verify(employeeProvider).save(argThat(saved ->
                saved.email().equals("novo@kts.com") &&
                saved.phone().equals("11999999999") &&
                saved.address().number().equals("99") &&
                saved.address().street().equals("Rua Nova")
        ));
    }

    @Test
    void markMessagesAsSeenSavesUpdatedTimestamp() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = sampleEmployee(employeeId, UUID.randomUUID(), true);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));

        service.markMessagesAsSeen();

        verify(employeeProvider).save(argThat(saved -> saved.lastSeenMessageTimestamp() != null));
    }

    @Test
    void toggleActivateFlipsEmployeeStatus() {
        UUID companyId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        Employee manager = sampleEmployee(managerId, companyId, true);
        Employee target = sampleEmployee(employeeId, companyId, true);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerId);
        when(employeeProvider.findCompanyIdByEmployeeId(managerId)).thenReturn(Optional.of(companyId));
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(target));

        service.toggleActivate(employeeId);

        verify(employeeProvider).save(argThat(saved -> !saved.active()));
    }

    @Test
    void cpfExistsDelegatesToProvider() {
        when(employeeProvider.cpfExists("123")).thenReturn(true);
        assertTrue(service.cpfExists("123"));
    }

    @Test
    void createEmployeeThrowsForCtoWithoutCompanyId() {
        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("CTO");

        var req = new com.kts.kronos.adapter.in.web.dto.employee.CreateEmployeeRequest(
                "Nome", "12345678901", "12345678901", "Dev", "mail@kts.com", 1000.0,
                "11999999999", new com.kts.kronos.adapter.in.web.dto.address.AddressRequest("12345678", "10"),
                null, false, null, LocalTime.of(8,0), LocalTime.of(17,0), LocalTime.of(12,0), LocalTime.of(13,0),
                null, null, null, null, null
        );

        assertThrows(BadRequestException.class, () -> service.createEmployee(req));
    }

    private Employee sampleEmployee(UUID employeeId, UUID companyId, boolean active) {
        return new Employee(
                employeeId,
                "Nome",
                "12345678901",
                "12345678901",
                "Dev",
                "mail@kts.com",
                1000.0,
                "11999999999",
                active,
                new Address("Rua", "1", "12345678", "Cidade", "ST"),
                companyId,
                LocalDateTime.now(),
                false,
                null,
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
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
