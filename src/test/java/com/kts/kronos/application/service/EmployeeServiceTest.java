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
import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.application.port.out.provider.FaceStorageProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.domain.model.enuns.WorkScheduleType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
    void createEmployeeCreatesNewWithDefaultsWithoutFace() {
        UUID managerId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("MANAGER");
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerId);
        when(employeeProvider.findCompanyIdByEmployeeId(managerId)).thenReturn(Optional.of(companyId));
        when(employeeProvider.findByCpf("12345678901")).thenReturn(Optional.empty());
        when(viaCep.lookup("12345678")).thenReturn(new Address("Rua A", "0", "12345678", "Cidade", "ST"));
        when(employeeProvider.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var saved = service.createEmployee(baseCreateRequest(null, null, null));

        assertEquals(companyId, saved.companyId());
        assertEquals(LocalTime.of(8, 0), saved.workStartTime());
        assertEquals(LocalTime.of(17, 0), saved.workEndTime());
        assertEquals(LocalTime.of(12, 0), saved.breakStartTime());
        assertEquals(LocalTime.of(13, 0), saved.breakEndTime());
        verify(faceStorageProvider, never()).uploadFaceImage(any(), any(), anyString());
    }

    @Test
    void createEmployeeCtoUsesRequestCompanyAndFaceSuccess() {
        UUID companyId = UUID.randomUUID();
        String b64 = Base64.getEncoder().encodeToString("img".getBytes());

        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("CTO");
        when(employeeProvider.findByCpf("12345678901")).thenReturn(Optional.empty());
        when(viaCep.lookup("12345678")).thenReturn(new Address("Rua", "0", "12345678", "Cidade", "ST"));
        when(employeeProvider.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(faceStorageProvider.uploadFaceImage(any(), any(), eq("image/jpeg"))).thenReturn("new-key");
        when(faceRecognitionProvider.indexFace(eq("new-key"), any())).thenReturn("face-id");

        var req = baseCreateRequest(companyId, b64, null);
        var saved = service.createEmployee(req);

        assertEquals(companyId, saved.companyId());
        verify(employeeProvider, atLeast(2)).save(any(Employee.class));
    }

    @Test
    void createEmployeeThrowsForCtoWithoutCompanyId() {
        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("CTO");
        assertThrows(BadRequestException.class, () -> service.createEmployee(baseCreateRequest(null, null, null)));
    }

    @Test
    void createEmployeeThrowsWhenManagerCompanyNotFound() {
        UUID managerId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("MANAGER");
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerId);
        when(employeeProvider.findCompanyIdByEmployeeId(managerId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.createEmployee(baseCreateRequest(null, null, null)));
    }

    @Test
    void createEmployeeThrowsWhenCpfAlreadyExistsAndLinkedToUser() {
        UUID existingEmployeeId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("CTO");
        when(employeeProvider.findByCpf("12345678901")).thenReturn(Optional.of(sampleEmployee(existingEmployeeId, UUID.randomUUID(), true)));
        when(userProvider.existsByEmployeeId(existingEmployeeId)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.createEmployee(baseCreateRequest(UUID.randomUUID(), null, null)));
    }

    @Test
    void createEmployeeUpdatesOrphanEmployee() {
        UUID companyId = UUID.randomUUID();
        UUID existingEmployeeId = UUID.randomUUID();

        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("CTO");
        when(employeeProvider.findByCpf("12345678901")).thenReturn(Optional.of(sampleEmployee(existingEmployeeId, UUID.randomUUID(), true)));
        when(userProvider.existsByEmployeeId(existingEmployeeId)).thenReturn(false);
        when(viaCep.lookup("12345678")).thenReturn(new Address("Rua B", "0", "12345678", "Cidade", "ST"));
        when(employeeProvider.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var saved = service.createEmployee(baseCreateRequest(companyId, null, null));

        assertEquals(existingEmployeeId, saved.employeeId());
        assertEquals(companyId, saved.companyId());
    }

    @Test
    void createEmployeeFaceRegistrationDeletesNewImageAndThrowsWhenNoFaceDetected() {
        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("CTO");
        when(employeeProvider.findByCpf("12345678901")).thenReturn(Optional.empty());
        when(viaCep.lookup("12345678")).thenReturn(new Address("Rua", "0", "12345678", "Cidade", "ST"));
        when(employeeProvider.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(faceStorageProvider.uploadFaceImage(any(), any(), anyString())).thenReturn("key");
        when(faceRecognitionProvider.indexFace(eq("key"), any())).thenReturn(null);

        String b64 = Base64.getEncoder().encodeToString("img".getBytes());
        assertThrows(BadRequestException.class, () -> service.createEmployee(baseCreateRequest(UUID.randomUUID(), b64, null)));
        verify(faceStorageProvider).deleteFaceImage("key");
    }

    @Test
    void createEmployeeThrowsWhenFaceBase64Invalid() {
        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("CTO");
        when(employeeProvider.findByCpf("12345678901")).thenReturn(Optional.empty());
        when(viaCep.lookup("12345678")).thenReturn(new Address("Rua", "0", "12345678", "Cidade", "ST"));
        when(employeeProvider.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(BadRequestException.class, () -> service.createEmployee(baseCreateRequest(UUID.randomUUID(), "%%%", null)));
    }

    @Test
    void createEmployeePropagatesRuntimeExceptionFromFaceUpload() {
        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("CTO");
        when(employeeProvider.findByCpf("12345678901")).thenReturn(Optional.empty());
        when(viaCep.lookup("12345678")).thenReturn(new Address("Rua", "0", "12345678", "Cidade", "ST"));
        when(employeeProvider.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(faceStorageProvider.uploadFaceImage(any(), any(), anyString())).thenThrow(new RuntimeException("boom"));

        String b64 = Base64.getEncoder().encodeToString("img".getBytes());
        assertThrows(RuntimeException.class, () -> service.createEmployee(baseCreateRequest(UUID.randomUUID(), b64, null)));
    }

    @Test
    void createEmployeeWrapsCheckedExceptionFromFaceUpload() {
        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("CTO");
        when(employeeProvider.findByCpf("12345678901")).thenReturn(Optional.empty());
        when(viaCep.lookup("12345678")).thenReturn(new Address("Rua", "0", "12345678", "Cidade", "ST"));
        when(employeeProvider.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doAnswer(invocation -> { throw new IOException("io-fail"); })
                .when(faceStorageProvider).uploadFaceImage(any(), any(), anyString());

        String b64 = Base64.getEncoder().encodeToString("img".getBytes());
        assertThrows(RuntimeException.class, () -> service.createEmployee(baseCreateRequest(UUID.randomUUID(), b64, null)));
    }

    @Test
    void listEmployeesReturnsByCompanyWhenActiveIsNull() {
        UUID managerId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerId);
        when(employeeProvider.findCompanyIdByEmployeeId(managerId)).thenReturn(Optional.of(companyId));
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of(sampleEmployee(UUID.randomUUID(), companyId, true)));

        var result = service.listEmployees(null);
        assertEquals(1, result.size());
    }

    @Test
    void listEmployeesReturnsByCompanyAndActiveWhenFilterPresent() {
        UUID managerId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerId);
        when(employeeProvider.findCompanyIdByEmployeeId(managerId)).thenReturn(Optional.of(companyId));
        when(employeeProvider.findByCompanyIdAndActive(companyId, false)).thenReturn(List.of());

        var result = service.listEmployees(false);
        assertTrue(result.isEmpty());
    }

    @Test
    void getEmployeeReturnsEmployeeWhenSameCompany() {
        UUID managerId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerId);
        when(employeeProvider.findCompanyIdByEmployeeId(managerId)).thenReturn(Optional.of(companyId));
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(sampleEmployee(employeeId, companyId, true)));

        var employee = service.getEmployee(employeeId);
        assertEquals(employeeId, employee.employeeId());
    }

    @Test
    void getEmployeeThrowsWhenNotFound() {
        UUID managerId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerId);
        when(employeeProvider.findCompanyIdByEmployeeId(managerId)).thenReturn(Optional.of(companyId));
        when(employeeProvider.findById(any())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getEmployee(UUID.randomUUID()));
    }

    @Test
    void getEmployeeThrowsWhenFromDifferentCompany() {
        UUID managerId = UUID.randomUUID();
        UUID managerCompany = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerId);
        when(employeeProvider.findCompanyIdByEmployeeId(managerId)).thenReturn(Optional.of(managerCompany));
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(sampleEmployee(employeeId, UUID.randomUUID(), true)));

        assertThrows(ResourceNotFoundException.class, () -> service.getEmployee(employeeId));
    }

    @Test
    void updateEmployeeUpdatesDataAddressAndFaceAndDeletesOldFace() {
        UUID managerId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        Employee existing = sampleEmployee(employeeId, companyId, true).withFaceS3ObjectKey("old-key");
        String b64 = Base64.getEncoder().encodeToString("new-image".getBytes());

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerId);
        when(employeeProvider.findCompanyIdByEmployeeId(managerId)).thenReturn(Optional.of(companyId));
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(existing));
        when(viaCep.lookup("12345678")).thenReturn(new Address("Rua Atualizada", "0", "12345678", "Cidade", "ST"));
        when(faceStorageProvider.uploadFaceImage(any(), any(), anyString())).thenReturn("new-key");
        when(faceRecognitionProvider.indexFace(eq("new-key"), eq(employeeId))).thenReturn("face-id");

        service.updateEmployee(employeeId, baseManagerUpdateRequest(b64));

        verify(faceStorageProvider).deleteFaceImage("old-key");
        verify(employeeProvider).save(argThat(saved ->
                saved.fullName().equals("Novo Nome") &&
                        saved.address().street().equals("Rua Atualizada") &&
                        "new-key".equals(saved.faceS3ObjectKey())));
    }

    @Test
    void updateEmployeeWithoutAddressAndFaceKeepsCurrent() {
        UUID managerId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        Employee existing = sampleEmployee(employeeId, companyId, true);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerId);
        when(employeeProvider.findCompanyIdByEmployeeId(managerId)).thenReturn(Optional.of(companyId));
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(existing));

        service.updateEmployee(employeeId, new UpdateEmployeeManagerRequest(null, null, null, null, null, null, null, null,
                null, "   ", null, null, null, null, null, null, null, null, null));

        verify(viaCep, never()).lookup(anyString());
        verify(faceStorageProvider, never()).uploadFaceImage(any(), any(), anyString());
        verify(employeeProvider).save(any(Employee.class));
    }

    @Test
    void deleteEmployeeRemovesEmployee() {
        UUID managerId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerId);
        when(employeeProvider.findCompanyIdByEmployeeId(managerId)).thenReturn(Optional.of(companyId));
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(sampleEmployee(employeeId, companyId, true)));

        service.deleteEmployee(employeeId);
        verify(employeeProvider).deleteById(employeeId);
    }

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
    void getOwnProfileThrowsWhenEmployeeNotFound() {
        UUID employeeId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getOwnProfile());
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
    void updateOwnProfileWithoutAddressPreservesAddress() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = sampleEmployee(employeeId, UUID.randomUUID(), true);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));

        service.updateOwnProfile(new UpdateEmployeePartnerRequest(null, null, null));

        verify(viaCep, never()).lookup(anyString());
        verify(employeeProvider).save(argThat(saved -> saved.address().equals(employee.address())));
    }

    @Test
    void updateOwnProfileThrowsWhenEmployeeNotFound() {
        UUID employeeId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateOwnProfile(new UpdateEmployeePartnerRequest("a@a.com", "1", null)));
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
    void markMessagesAsSeenThrowsWhenEmployeeNotFound() {
        UUID employeeId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, service::markMessagesAsSeen);
    }

    @Test
    void toggleActivateFlipsEmployeeStatus() {
        UUID companyId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
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

    private CreateEmployeeRequest baseCreateRequest(UUID companyId, String base64, Double salary) {
        return new CreateEmployeeRequest(
                "Nome", "12345678901", "12345678901", "Dev", "mail@kts.com", salary,
                "11999999999", new AddressRequest("12345678", "10"), companyId, false, base64,
                null, null, null, null,
                WorkScheduleType.SIX_BY_ONE_ONE_WEEKEND,
                LocalDate.of(2025, 1, 1),
                DayOfWeek.FRIDAY,
                1,
                Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)
        );
    }

    private UpdateEmployeeManagerRequest baseManagerUpdateRequest(String base64) {
        return new UpdateEmployeeManagerRequest(
                "Novo Nome",
                null,
                "10987654321",
                "QA",
                "novo@kts.com",
                5000.0,
                "11988888888",
                true,
                new UpdateAddressRequest("12345678", "999"),
                base64,
                LocalTime.of(7, 0),
                LocalTime.of(16, 0),
                LocalTime.of(11, 30),
                LocalTime.of(12, 30),
                WorkScheduleType.TRADITIONAL_5X2,
                LocalDate.of(2025, 2, 1),
                DayOfWeek.WEDNESDAY,
                2,
                Set.of(DayOfWeek.MONDAY)
        );
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
                WorkScheduleType.TRADITIONAL_5X2,
                LocalDate.of(2025, 1, 1),
                DayOfWeek.MONDAY,
                1,
                Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)
        );
    }
}
