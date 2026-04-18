package com.kts.kronos.application;

import com.kts.kronos.adapter.in.web.dto.address.AddressRequest;
import com.kts.kronos.adapter.in.web.dto.address.UpdateAddressRequest;
import com.kts.kronos.adapter.in.web.dto.employee.CreateEmployeeRequest;
import com.kts.kronos.adapter.in.web.dto.employee.UpdateEmployeePartnerRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.port.out.provider.AddressLookupProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.application.port.out.provider.FaceStorageProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.application.security.BiometricProtectionService;
import com.kts.kronos.application.service.EmployeeService;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @InjectMocks
    private EmployeeService service;

    @Mock
    private EmployeeProvider employeeProvider;
    @Mock
    private AddressLookupProvider viaCep;
    @Mock
    private JwtAuthenticatedUser jwtAuthenticatedUser;
    @Mock
    private UserProvider userProvider;
    @Mock
    private FaceStorageProvider faceStorageProvider;
    @Mock
    private FaceRecognitionProvider faceRecognitionProvider;
    @Mock
    private BiometricProtectionService biometricProtectionService;
    @Mock
    private AcceptTermsUseCase acceptTermsUseCase;

    private UUID loggedEmployeeId;
    private UUID companyId;
    private Address lookedUpAddress;
    private Employee loggedEmployee;

    @BeforeEach
    void setUp() {
        loggedEmployeeId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        lookedUpAddress = new Address("Rua A", "10", "12345678", "Rio", "RJ");
        loggedEmployee = buildEmployee(loggedEmployeeId, companyId);
    }

    @Test
    @DisplayName("createEmployee: CTO cria colaborador com defaults de jornada")
    void shouldCreateEmployeeAsCtoWithDefaultTimes() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "Maria Silva",
                "12345678901",
                "12345678901",
                "Dev",
                "maria@kts.com",
                null,
                "21999999999",
                new AddressRequest("12345678", "10"),
                companyId,
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
                Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)
        );

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(employeeProvider.findByCpf("12345678901")).thenReturn(Optional.empty());
        when(viaCep.lookup("12345678")).thenReturn(new Address("Rua A", "0", "12345678", "Rio", "RJ"));
        when(employeeProvider.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Employee created = service.createEmployee(request);

        assertEquals(companyId, created.companyId());
        assertEquals(0.0, created.salary());
        assertEquals(LocalTime.of(8, 0), created.workStartTime());
        assertEquals(LocalTime.of(17, 0), created.workEndTime());
        assertEquals(LocalTime.of(12, 0), created.breakStartTime());
        assertEquals(LocalTime.of(13, 0), created.breakEndTime());
        assertEquals("10", created.address().number());
        verify(faceStorageProvider, never()).uploadFaceImage(any(), any(), anyString());
    }

    @Test
    @DisplayName("createEmployee: CTO sem companyId falha")
    void shouldFailCreateEmployeeWhenCtoDoesNotProvideCompanyId() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "Maria Silva",
                "12345678901",
                null,
                "Dev",
                "maria@kts.com",
                1000.0,
                "21999999999",
                new AddressRequest("12345678", "10"),
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

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);

        assertThrows(BadRequestException.class, () -> service.createEmployee(request));
        verify(employeeProvider, never()).save(any());
    }

    @Test
    @DisplayName("createEmployee: MANAGER herda a company do colaborador logado")
    void shouldCreateEmployeeUsingManagersCompany() {
        UUID anotherCompanyId = UUID.randomUUID();

        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "Carlos Souza",
                "98765432100",
                null,
                "QA",
                "carlos@kts.com",
                2500.0,
                "21911111111",
                new AddressRequest("12345678", "20"),
                anotherCompanyId,
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

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(loggedEmployee));
        when(employeeProvider.findByCpf("98765432100")).thenReturn(Optional.empty());
        when(viaCep.lookup("12345678")).thenReturn(new Address("Rua A", "0", "12345678", "Rio", "RJ"));
        when(employeeProvider.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Employee created = service.createEmployee(request);

        assertEquals(companyId, created.companyId());
    }

    @Test
    @DisplayName("createEmployee: CPF órfão reaproveita employee existente")
    void shouldUpdateOrphanEmployeeWhenCpfAlreadyExistsWithoutUser() {
        UUID orphanEmployeeId = UUID.randomUUID();
        Employee orphan = buildEmployee(orphanEmployeeId, UUID.randomUUID());

        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "Novo Nome",
                orphan.cpf(),
                "12345678901",
                "Tech Lead",
                "novo@kts.com",
                7000.0,
                "21922222222",
                new AddressRequest("12345678", "99"),
                companyId,
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                DayOfWeek.FRIDAY,
                2,
                Set.of(DayOfWeek.MONDAY)
        );

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(employeeProvider.findByCpf(orphan.cpf())).thenReturn(Optional.of(orphan));
        when(userProvider.existsByEmployeeId(orphanEmployeeId)).thenReturn(false);
        when(viaCep.lookup("12345678")).thenReturn(new Address("Rua B", "0", "12345678", "Rio", "RJ"));
        when(employeeProvider.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Employee updated = service.createEmployee(request);

        assertEquals(orphanEmployeeId, updated.employeeId());
        assertEquals(companyId, updated.companyId());
        assertEquals("Novo Nome", updated.fullName());
        assertEquals("99", updated.address().number());
    }

    @Test
    @DisplayName("createEmployee: CPF já vinculado a user falha")
    void shouldFailWhenCpfAlreadyLinkedToAUser() {
        Employee existing = buildEmployee(UUID.randomUUID(), companyId);

        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "Nome",
                existing.cpf(),
                null,
                "Dev",
                "nome@kts.com",
                1000.0,
                "21933333333",
                new AddressRequest("12345678", "10"),
                companyId,
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

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(employeeProvider.findByCpf(existing.cpf())).thenReturn(Optional.of(existing));
        when(userProvider.existsByEmployeeId(existing.employeeId())).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.createEmployee(request));
        verify(employeeProvider, never()).save(any());
    }

    @Test
    @DisplayName("createEmployee: remove imagem recém-enviada quando não detecta face")
    void shouldDeleteNewUploadedImageWhenNoFaceIsDetected() {
        String validBase64 = Base64.getEncoder().encodeToString("face".getBytes());

        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "Face Test",
                "12345678901",
                null,
                "Dev",
                "face@kts.com",
                1000.0,
                "21944444444",
                new AddressRequest("12345678", "10"),
                companyId,
                false,
                validBase64,
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

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(employeeProvider.findByCpf("12345678901")).thenReturn(Optional.empty());
        when(viaCep.lookup("12345678")).thenReturn(new Address("Rua A", "0", "12345678", "Rio", "RJ"));
        when(employeeProvider.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(faceStorageProvider.uploadFaceImage(any(), any(), eq("image/jpeg"))).thenReturn("faces/new-key.jpg");
        when(faceRecognitionProvider.indexFace(eq("faces/new-key.jpg"), any())).thenReturn(null);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.createEmployee(request)
        );

        assertEquals("Falha ao registrar face no Rekognition.", exception.getMessage());
        assertNotNull(exception.getCause());
        assertInstanceOf(BadRequestException.class, exception.getCause());

        verify(faceStorageProvider, times(2)).deleteFaceImage("faces/new-key.jpg");
        }

    @Test
    @DisplayName("createEmployee: base64 inválido remove upload parcial e falha")
    void shouldFailWhenBase64IsInvalidDuringFaceRegistration() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "Face Test",
                "12345678901",
                null,
                "Dev",
                "face@kts.com",
                1000.0,
                "21944444444",
                new AddressRequest("12345678", "10"),
                companyId,
                false,
                "%%%invalid%%%",
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

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(employeeProvider.findByCpf("12345678901")).thenReturn(Optional.empty());
        when(viaCep.lookup("12345678")).thenReturn(new Address("Rua A", "0", "12345678", "Rio", "RJ"));
        when(employeeProvider.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(BadRequestException.class, () -> service.createEmployee(request));
        verify(faceStorageProvider, never()).deleteFaceImage(anyString());
    }

    @Test
    @DisplayName("updateOwnProfile: atualiza email, telefone e endereço")
    void shouldUpdateOwnProfile() {
        UpdateEmployeePartnerRequest request = new UpdateEmployeePartnerRequest(
                "novo@kts.com",
                "21988888888",
                new UpdateAddressRequest("12345678", "77")
        );

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(loggedEmployee));
        when(viaCep.lookup("12345678")).thenReturn(new Address("Rua Nova", "0", "12345678", "Rio", "RJ"));

        service.updateOwnProfile(request);

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeProvider).save(captor.capture());

        Employee saved = captor.getValue();
        assertEquals("novo@kts.com", saved.email());
        assertEquals("21988888888", saved.phone());
        assertEquals("77", saved.address().number());
        assertEquals("Rua Nova", saved.address().street());
    }

    @Test
    @DisplayName("markMessagesAsSeen: grava timestamp")
    void shouldMarkMessagesAsSeen() {
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(loggedEmployee));

        service.markMessagesAsSeen();

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeProvider).save(captor.capture());
        assertNotNull(captor.getValue().lastSeenMessageTimestamp());
    }

    @Test
    @DisplayName("deleteEmployee: bloqueia exclusão quando há user vinculado")
    void shouldBlockDeleteWhenEmployeeHasLinkedUser() {
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(loggedEmployee));
        when(userProvider.existsByEmployeeId(loggedEmployeeId)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.deleteEmployee(loggedEmployeeId));
        verify(employeeProvider, never()).deleteById(any());
    }


    private Employee buildEmployee(UUID employeeId, UUID companyId) {
        return new Employee(
                employeeId,
                "Gestor Teste",
                "12345678901",
                "12345678901",
                "Manager",
                "gestor@kts.com",
                5000.0,
                "21999999999",
                true,
                lookedUpAddress,
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
                DayOfWeek.MONDAY,
                null,
                null
        );
    }
}