package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.address.AddressRequest;
import com.kts.kronos.adapter.in.web.dto.address.UpdateAddressRequest;
import com.kts.kronos.adapter.in.web.dto.employee.CreateEmployeeRequest;
import com.kts.kronos.adapter.in.web.dto.employee.UpdateEmployeePartnerRequest;
import com.kts.kronos.adapter.in.web.dto.employee.EmployeeProfile;
import com.kts.kronos.adapter.in.web.dto.employee.UpdateEmployeeManagerRequest;
import com.kts.kronos.adapter.in.web.dto.employee.RegisterFaceRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ConflictException;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.AddressLookupProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.application.port.out.provider.FaceStorageProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.application.port.out.provider.LegalConsentProvider;
import com.kts.kronos.application.security.AuthenticationRateLimitService;
import com.kts.kronos.application.security.BiometricProtectionService;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.domain.model.enuns.WorkScheduleType;
import com.kts.kronos.domain.model.enuns.ConsentType;
import com.kts.kronos.domain.model.BiometricConsentStatus;
import com.kts.kronos.domain.model.enuns.AuditAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

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
    @Mock
    private AuthenticationRateLimitService authenticationRateLimitService;
    @Mock
    private com.kts.kronos.observability.application.KronosMetrics kronosMetrics;
    @Mock
    private com.kts.kronos.application.port.out.provider.LegalConsentProvider legalConsentProvider;
    @Mock
    private AuditService auditService;

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
        when(employeeProvider.cpfExistsInCompany(companyId, "12345678901")).thenReturn(false);
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
    @DisplayName("createEmployee: corrida de CPF duplicado deve virar 409")
    void shouldTranslateDuplicateCpfRaceToConflict() {
        CreateEmployeeRequest request = createRequest("Maria Silva", "12345678901", companyId, null);

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(employeeProvider.cpfExistsInCompany(companyId, "12345678901")).thenReturn(false);
        when(viaCep.lookup("12345678")).thenReturn(new Address("Rua A", "0", "12345678", "Rio", "RJ"));
        when(employeeProvider.save(any(Employee.class))).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThrows(ConflictException.class, () -> service.createEmployee(request));
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
    @DisplayName("createEmployee: role sem permissão falha")
    void shouldRejectCreateEmployeeForUnauthorizedRole() {
        CreateEmployeeRequest request = createRequest("Maria Silva", "12345678901", null, null);

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);

        assertThrows(ForbiddenException.class, () -> service.createEmployee(request));
        verify(employeeProvider, never()).findByCpf(anyString());
        verify(employeeProvider, never()).save(any());
    }

    @Test
    @DisplayName("createEmployee: manager inexistente falha antes de criar")
    void shouldFailCreateEmployeeWhenLoggedManagerIsMissing() {
        CreateEmployeeRequest request = createRequest("Maria Silva", "12345678901", null, null);

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.createEmployee(request));
        verify(employeeProvider, never()).findByCpf(anyString());
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
        when(employeeProvider.cpfExistsInCompany(companyId, "98765432100")).thenReturn(false);
        when(viaCep.lookup("12345678")).thenReturn(new Address("Rua A", "0", "12345678", "Rio", "RJ"));
        when(employeeProvider.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Employee created = service.createEmployee(request);

        assertEquals(companyId, created.companyId());
    }

    @Test
    @DisplayName("createEmployee: rejeita quando face é incluída no request (LGPD-S01-01)")
    void shouldRejectWhenFaceIncludedInCreateRequest() {
        String validBase64 = Base64.getEncoder().encodeToString("face".getBytes());
        CreateEmployeeRequest request = createRequest("Face Test", "12345678901", companyId, validBase64);

        assertThrows(BadRequestException.class, () -> service.createEmployee(request));
        verify(employeeProvider, never()).findByCpf(anyString());
        verify(viaCep, never()).lookup(anyString());
        verify(employeeProvider, never()).save(any());
    }

    @Test
    @DisplayName("createEmployee: CPF já cadastrado na mesma empresa lança ConflictException (R-002)")
    void shouldThrowConflictWhenCpfAlreadyExistsInSameCompany() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "Novo Nome",
                "12345678901",
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
        when(employeeProvider.cpfExistsInCompany(companyId, "12345678901")).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.createEmployee(request));
        verify(employeeProvider, never()).save(any());
    }

    @Test
    @DisplayName("createEmployee: mesmo CPF em empresa diferente deve ser permitido (R-001, R-002)")
    void shouldAllowSameCpfInDifferentCompany() {
        UUID outraEmpresaId = UUID.randomUUID();
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "Mesmo CPF Outra Empresa",
                "12345678901",
                null,
                "Dev",
                "outro@kts.com",
                5000.0,
                "21933333333",
                new AddressRequest("12345678", "10"),
                outraEmpresaId,
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
        // CPF existe na empresa A mas NÃO na outraEmpresaId
        when(employeeProvider.cpfExistsInCompany(outraEmpresaId, "12345678901")).thenReturn(false);
        when(viaCep.lookup("12345678")).thenReturn(new Address("Rua B", "0", "12345678", "Rio", "RJ"));
        when(employeeProvider.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Employee created = service.createEmployee(request);

        assertEquals(outraEmpresaId, created.companyId());
        verify(employeeProvider).save(any(Employee.class));
    }

    @Test
    @DisplayName("createEmployee: CPF já cadastrado na empresa (com ou sem user) lança ConflictException")
    void shouldFailWhenCpfExistsInCompanyRegardlessOfUserLink() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "Nome",
                "12345678901",
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
        when(employeeProvider.cpfExistsInCompany(companyId, "12345678901")).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.createEmployee(request));
        verify(employeeProvider, never()).save(any());
    }

    @Test
    @DisplayName("createEmployee: cria colaborador sem face por bloqueio LGPD-S01-01")
    void shouldCreateEmployeeWithoutFaceByDefault() {
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
        when(employeeProvider.cpfExistsInCompany(companyId, "12345678901")).thenReturn(false);
        when(viaCep.lookup("12345678")).thenReturn(new Address("Rua A", "0", "12345678", "Rio", "RJ"));
        when(employeeProvider.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Employee created = service.createEmployee(request);

        assertNull(created.faceS3ObjectKey());
        verify(faceStorageProvider, never()).uploadFaceImage(any(), any(), anyString());
        verify(faceRecognitionProvider, never()).indexFace(anyString(), any());
        }

    @Test
    @DisplayName("createEmployee: rejeita base64 de face por LGPD-S01-01")
    void shouldRejectWhenBase64IsProvidedForFace() {
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

        assertThrows(BadRequestException.class, () -> service.createEmployee(request));
        verify(employeeProvider, never()).save(any());
        verify(faceStorageProvider, never()).deleteFaceImage(anyString());
    }

    @Test
    @DisplayName("createEmployee: sem face cria colaborador normalmente")
    void shouldCreateEmployeeWithoutFaceSuccessfully() {
        CreateEmployeeRequest request = createRequest("Face Test", "12345678901", companyId, null);

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(employeeProvider.cpfExistsInCompany(companyId, "12345678901")).thenReturn(false);
        when(viaCep.lookup("12345678")).thenReturn(new Address("Rua A", "0", "12345678", "Rio", "RJ"));
        when(employeeProvider.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Employee created = service.createEmployee(request);

        assertNotNull(created.employeeId());
        assertNull(created.faceS3ObjectKey());
        verify(faceStorageProvider, never()).uploadFaceImage(any(), any(), anyString());
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
    @DisplayName("listEmployees: lista por empresa com e sem filtro active")
    void shouldListEmployeesByLoggedCompany() {
        Employee another = buildEmployee(UUID.randomUUID(), companyId);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(loggedEmployee));
        when(employeeProvider.findByCompanyId(companyId)).thenReturn(List.of(loggedEmployee, another));

        assertEquals(2, service.listEmployees(null).size());

        when(employeeProvider.findByCompanyIdAndActive(companyId, true)).thenReturn(List.of(loggedEmployee));
        assertEquals(1, service.listEmployees(true).size());
    }

    @Test
    @DisplayName("getEmployee: retorna apenas colaborador da mesma empresa")
    void shouldGetEmployeeOnlyFromLoggedCompany() {
        UUID targetId = UUID.randomUUID();
        Employee target = buildEmployee(targetId, companyId);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(loggedEmployee));
        when(employeeProvider.findById(targetId)).thenReturn(Optional.of(target));

        assertEquals(targetId, service.getEmployee(targetId).employeeId());

        UUID otherCompanyEmployeeId = UUID.randomUUID();
        Employee otherCompanyEmployee = buildEmployee(otherCompanyEmployeeId, UUID.randomUUID());
        when(employeeProvider.findById(otherCompanyEmployeeId)).thenReturn(Optional.of(otherCompanyEmployee));
        assertThrows(ResourceNotFoundException.class, () -> service.getEmployee(otherCompanyEmployeeId));
    }

    @Test
    @DisplayName("getEmployee: falha quando colaborador não existe")
    void shouldFailWhenEmployeeIsMissing() {
        UUID targetId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(loggedEmployee));
        when(employeeProvider.findById(targetId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getEmployee(targetId));
    }

    @Test
    @DisplayName("updateEmployee: atualiza campos gerenciais, endereço e preserva face")
    void shouldUpdateEmployeeWithManagerFieldsAddressAndPreserveFace() {
        UUID targetId = UUID.randomUUID();
        Employee target = buildEmployee(targetId, companyId).withFaceS3ObjectKey("faces/old.jpg");
        UpdateEmployeeManagerRequest request = new UpdateEmployeeManagerRequest(
                "Nome Atualizado",
                null,
                "98765432100",
                "Lead",
                "lead@kts.com",
                9000.0,
                "21977777777",
                true,
                new UpdateAddressRequest("12345678", "88"),
                null,
                LocalTime.of(7, 0),
                LocalTime.of(16, 0),
                LocalTime.of(11, 30),
                LocalTime.of(12, 30),
                WorkScheduleType.TRADITIONAL_5X2,
                LocalDate.of(2026, 4, 1),
                DayOfWeek.FRIDAY,
                1,
                Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)
        );

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(loggedEmployee));
        when(employeeProvider.findById(targetId)).thenReturn(Optional.of(target));
        when(viaCep.lookup("12345678")).thenReturn(new Address("Rua Nova", "0", "12345678", "Rio", "RJ"));

        service.updateEmployee(targetId, request);

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeProvider).save(captor.capture());
        Employee saved = captor.getValue();
        assertEquals("Nome Atualizado", saved.fullName());
        assertEquals("98765432100", saved.pis());
        assertEquals("Lead", saved.jobPosition());
        assertEquals(9000.0, saved.salary());
        assertEquals(LocalDate.of(2026, 4, 1), saved.scaleStartDate());
        assertEquals("88", saved.address().number());
        assertEquals("faces/old.jpg", saved.faceS3ObjectKey());
        verify(faceStorageProvider, never()).uploadFaceImage(any(), any(), anyString());
        verify(biometricProtectionService, never()).protectEnrollment(any(), anyString(), any());
    }

    @Test
    @DisplayName("updateEmployee: preserva campos quando request vem vazio")
    void shouldPreserveEmployeeFieldsWhenManagerRequestIsEmpty() {
        UUID targetId = UUID.randomUUID();
        Employee target = buildEmployee(targetId, companyId);
        UpdateEmployeeManagerRequest request = new UpdateEmployeeManagerRequest(
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null
        );

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(loggedEmployee));
        when(employeeProvider.findById(targetId)).thenReturn(Optional.of(target));

        service.updateEmployee(targetId, request);

        verify(employeeProvider).save(argThat(saved ->
                saved.fullName().equals(target.fullName())
                        && saved.email().equals(target.email())
                        && saved.address().equals(target.address())
                        && saved.faceS3ObjectKey() == null
        ));
        verify(viaCep, never()).lookup(anyString());
        verify(faceStorageProvider, never()).uploadFaceImage(any(), any(), anyString());
    }

    @Test
    @DisplayName("deleteEmployee: inativa colaborador sem apagar documentos ou historico")
    void shouldDeactivateEmployeeWithoutDeletingLegalHistory() {
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(loggedEmployee));
        when(userProvider.existsByEmployeeId(loggedEmployeeId)).thenReturn(false);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(UUID.randomUUID());

        service.deleteEmployee(loggedEmployeeId);

        verify(employeeProvider).save(argThat(saved ->
                saved.employeeId().equals(loggedEmployeeId)
                        && !saved.active()
                        && "EMPLOYEE_DELETE".equals(saved.deactivationReason())
                        && saved.deletedAt() != null
        ));
        verify(acceptTermsUseCase, never()).revokeBiometricTerms(any(), any(), any());
        verify(employeeProvider, never()).deleteById(loggedEmployeeId);
    }

    @Test
    @DisplayName("getOwnProfile: retorna colaborador e role atual")
    void shouldReturnOwnProfileWithRole() {
        User user = new User(UUID.randomUUID(), "partner", "x", Role.PARTNER, true, loggedEmployeeId);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(loggedEmployee));
        when(userProvider.findByEmployeeId(loggedEmployeeId)).thenReturn(Optional.of(user));

        EmployeeProfile profile = service.getOwnProfile();

        assertEquals(loggedEmployee, profile.employee());
        assertEquals("PARTNER", profile.role());
    }

    @Test
    @DisplayName("getOwnProfile: falha quando user vinculado não existe")
    void shouldFailOwnProfileWhenUserIsMissing() {
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(loggedEmployee));
        when(userProvider.findByEmployeeId(loggedEmployeeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getOwnProfile());
    }

    @Test
    @DisplayName("updateOwnProfile: preserva campos quando request vem vazio")
    void shouldPreserveOwnProfileFieldsWhenRequestIsEmpty() {
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(loggedEmployee));

        service.updateOwnProfile(new UpdateEmployeePartnerRequest(null, null, null));

        verify(employeeProvider).save(argThat(saved ->
                saved.email().equals(loggedEmployee.email())
                        && saved.phone().equals(loggedEmployee.phone())
                        && saved.address().equals(loggedEmployee.address())
        ));
        verify(viaCep, never()).lookup(anyString());
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
    @DisplayName("markMessagesAsSeen: falha quando colaborador logado não existe")
    void shouldFailMarkMessagesAsSeenWhenEmployeeIsMissing() {
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.markMessagesAsSeen());
    }

    @Test
    @DisplayName("listEmployees: falha quando gestor autenticado não existe")
    void shouldFailListWhenLoggedManagerDoesNotExist() {
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.listEmployees(null));
    }

    @Test
    @DisplayName("cpfExists: delega ao provider")
    void shouldDelegateCpfExists() {
        when(employeeProvider.cpfExists("12345678901")).thenReturn(true);

        assertTrue(service.cpfExists("12345678901"));
    }

    @Test
    @DisplayName("toggleActivate: inverte status do colaborador da empresa")
    void shouldToggleEmployeeActivation() {
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(loggedEmployee));

        service.toggleActivate(loggedEmployeeId);

        verify(employeeProvider).save(argThat(saved ->
                saved.employeeId().equals(loggedEmployeeId) && !saved.active()
        ));
    }

    @Test
    @DisplayName("toggleActivate: reativa colaborador inativo")
    void shouldToggleInactiveEmployeeBackToActive() {
        Employee inactive = new Employee(
                loggedEmployee.employeeId(),
                loggedEmployee.fullName(),
                loggedEmployee.cpf(),
                loggedEmployee.pis(),
                loggedEmployee.jobPosition(),
                loggedEmployee.email(),
                loggedEmployee.salary(),
                loggedEmployee.phone(),
                false,
                loggedEmployee.address(),
                loggedEmployee.companyId(),
                loggedEmployee.lastSeenMessageTimestamp(),
                loggedEmployee.homeOffice(),
                loggedEmployee.faceS3ObjectKey(),
                loggedEmployee.workStartTime(),
                loggedEmployee.workEndTime(),
                loggedEmployee.breakStartTime(),
                loggedEmployee.breakEndTime(),
                loggedEmployee.scheduleType(),
                loggedEmployee.scaleStartDate(),
                loggedEmployee.preferredDayOff(),
                loggedEmployee.weekendOffIndex(),
                loggedEmployee.fixedWorkDays()
        );

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(loggedEmployeeId);
        when(employeeProvider.findById(loggedEmployeeId)).thenReturn(Optional.of(inactive));

        service.toggleActivate(loggedEmployeeId);

        verify(employeeProvider).save(argThat(saved ->
                saved.employeeId().equals(loggedEmployeeId) && saved.active()
        ));
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

    @Test
    @DisplayName("createEmployee: rejeita face por LGPD-S01-01 mesmo com provider")
    void shouldRejectFaceEvenWhenProviderIsAvailable() {
        String validBase64 = Base64.getEncoder().encodeToString("face".getBytes());
        CreateEmployeeRequest request = createRequest("Face Test", "12345678901", companyId, validBase64);

        assertThrows(BadRequestException.class, () -> service.createEmployee(request));
        verify(faceStorageProvider, never()).uploadFaceImage(any(), any(), anyString());
        verify(faceRecognitionProvider, never()).indexFace(anyString(), any());
    }

    @Test
    @DisplayName("createEmployee: salary null usa valor default 0.0")
    void shouldCreateEmployeeWithDefaultSalaryWhenNull() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "Novo Nome",
                "12345678901",
                "12345678901",
                "Tech Lead",
                "novo@kts.com",
                null, // salary null
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
        when(employeeProvider.cpfExistsInCompany(companyId, "12345678901")).thenReturn(false);
        when(viaCep.lookup("12345678")).thenReturn(new Address("Rua B", "0", "12345678", "Rio", "RJ"));
        when(employeeProvider.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Employee created = service.createEmployee(request);

        assertEquals(0.0, created.salary());
    }

    private CreateEmployeeRequest createRequest(String name, String cpf, UUID requestCompanyId, String faceImageBase64) {
        return new CreateEmployeeRequest(
                name,
                cpf,
                "12345678901",
                "Dev",
                name.toLowerCase().replace(" ", ".") + "@kts.com",
                3000.0,
                "21999999999",
                new AddressRequest("12345678", "10"),
                requestCompanyId,
                false,
                faceImageBase64,
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0),
                WorkScheduleType.TRADITIONAL_5X2,
                null,
                DayOfWeek.FRIDAY,
                1,
                Set.of(DayOfWeek.MONDAY)
        );
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

    @Test
    @DisplayName("LGPD-S01-01: createEmployee rejeita faceImageBase64")
    void shouldRejectBiometricEnrollmentInCreateEmployee() {
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

        assertThrows(BadRequestException.class, () -> service.createEmployee(request));
        verify(employeeProvider, never()).save(any());
        verify(biometricProtectionService, never()).protectEnrollment(any(), anyString(), any());
    }

    @Test
    @DisplayName("LGPD-S01-01: updateEmployee rejeita faceImageBase64")
    void shouldRejectBiometricEnrollmentInUpdateEmployee() {
        String validBase64 = Base64.getEncoder().encodeToString("face".getBytes());
        UUID employeeId = UUID.randomUUID();

        UpdateEmployeeManagerRequest request = new UpdateEmployeeManagerRequest(
                "Updated Name",
                "12345678901",
                null,
                "Dev",
                "email@kts.com",
                1000.0,
                "21933333333",
                false,
                new UpdateAddressRequest("12345678", "99"),
                validBase64,
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

        assertThrows(BadRequestException.class, () -> service.updateEmployee(employeeId, request));
        verify(employeeProvider, never()).save(any());
        verify(biometricProtectionService, never()).protectEnrollment(any(), anyString(), any());
    }

    @Test
    @DisplayName("LGPD-S01-03: enrollBiometricByManager rejeita quando livenessPassed é false")
    void shouldRejectManagerBiometricEnrollmentWithoutLiveness() {
        UUID managerId = UUID.randomUUID();
        UUID targetEmployeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        String validBase64 = Base64.getEncoder().encodeToString("facedata".getBytes());

        var manager = buildEmployee(managerId, companyId);
        var targetEmployee = new Employee(
                targetEmployeeId, "John Doe", "12345678901", "1234567890",
                "Dev", "john@kts.com", 1000.0, "21999999999", true,
                null, companyId, null, false,
                null, LocalTime.of(8, 0), LocalTime.of(17, 0),
                LocalTime.of(12, 0), LocalTime.of(13, 0),
                null, null, null, null, null
        );

        RegisterFaceRequest request = new RegisterFaceRequest(validBase64, targetEmployeeId, false);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerId);
        when(employeeProvider.findById(managerId)).thenReturn(Optional.of(manager));
        when(employeeProvider.findById(targetEmployeeId)).thenReturn(Optional.of(targetEmployee));
        when(acceptTermsUseCase.getBiometricConsentStatus(targetEmployeeId))
                .thenReturn(new BiometricConsentStatus(true, "v1", "hash1", "v1", "hash1", false));
        doThrow(new ForbiddenException("Validação de liveness obrigatória para esta operação."))
                .when(biometricProtectionService)
                .protectEnrollment(targetEmployeeId, validBase64, false);

        assertThrows(ForbiddenException.class, () -> service.enrollBiometricByManager(targetEmployeeId, request));
        verify(employeeProvider, never()).save(any());
    }

    @Test
    @DisplayName("LGPD-S01-03: enrollBiometricByManager aceita quando livenessPassed é true")
    void shouldAllowManagerToEnrollBiometricWithLiveness() {
        UUID managerId = UUID.randomUUID();
        UUID targetEmployeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        String validBase64 = Base64.getEncoder().encodeToString("facedata".getBytes());

        var manager = buildEmployee(managerId, companyId);
        var targetEmployee = new Employee(
                targetEmployeeId, "John Doe", "12345678901", "1234567890",
                "Dev", "john@kts.com", 1000.0, "21999999999", true,
                null, companyId, null, false,
                null, LocalTime.of(8, 0), LocalTime.of(17, 0),
                LocalTime.of(12, 0), LocalTime.of(13, 0),
                null, null, null, null, null
        );

        RegisterFaceRequest request = new RegisterFaceRequest(validBase64, targetEmployeeId, true);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerId);
        when(employeeProvider.findById(managerId)).thenReturn(Optional.of(manager));
        when(employeeProvider.findById(targetEmployeeId)).thenReturn(Optional.of(targetEmployee));
        when(acceptTermsUseCase.getBiometricConsentStatus(targetEmployeeId))
                .thenReturn(new BiometricConsentStatus(true, "v1", "hash1", "v1", "hash1", false));
        when(faceStorageProvider.uploadFaceImage(any(), any(), anyString()))
                .thenReturn("s3-key-123");
        when(faceRecognitionProvider.indexFace("s3-key-123", targetEmployeeId))
                .thenReturn("face-id-123");
        when(employeeProvider.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtAuthenticatedUser.getuserId()).thenReturn(UUID.randomUUID());

        service.enrollBiometricByManager(targetEmployeeId, request);

        verify(biometricProtectionService).protectEnrollment(targetEmployeeId, validBase64, true);
        verify(faceStorageProvider).uploadFaceImage(eq(targetEmployeeId), any(), eq("image/jpeg"));
        verify(faceRecognitionProvider).indexFace("s3-key-123", targetEmployeeId);
        verify(employeeProvider).save(any(Employee.class));
        verify(auditService).register(
                eq(AuditAction.BIOMETRIC_ENROLLMENT_BY_MANAGER),
                any(UUID.class),
                eq(targetEmployeeId),
                eq(companyId),
                anyString(),
                anyString(),
                anyString(),
                any(),
                any(),
                anyString()
        );
        verify(kronosMetrics).employeeCreated();
    }

    @Test
    @DisplayName("enrollBiometricByManager: rejeita quando colaborador não aceitou o termo")
    void shouldRejectManagerEnrollmentWhenNoConsent() {
        UUID managerId = UUID.randomUUID();
        UUID targetEmployeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        String validBase64 = Base64.getEncoder().encodeToString("facedata".getBytes());

        var manager = buildEmployee(managerId, companyId);
        var targetEmployee = new Employee(
                targetEmployeeId, "John Doe", "12345678901", "1234567890",
                "Dev", "john@kts.com", 1000.0, "21999999999", true,
                null, companyId, null, false,
                null, LocalTime.of(8, 0), LocalTime.of(17, 0),
                LocalTime.of(12, 0), LocalTime.of(13, 0),
                null, null, null, null, null
        );

        RegisterFaceRequest request = new RegisterFaceRequest(validBase64, targetEmployeeId, true);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerId);
        when(employeeProvider.findById(managerId)).thenReturn(Optional.of(manager));
        when(employeeProvider.findById(targetEmployeeId)).thenReturn(Optional.of(targetEmployee));
        when(acceptTermsUseCase.getBiometricConsentStatus(targetEmployeeId))
                .thenReturn(new BiometricConsentStatus(false, null, null, "v1", "hash1", true));

        assertThrows(ConflictException.class, () -> service.enrollBiometricByManager(targetEmployeeId, request));
        verify(employeeProvider, never()).save(any());
        verify(biometricProtectionService, never()).protectEnrollment(any(), anyString(), any());
    }

    @Test
    @DisplayName("enrollBiometricByManager: rejeita manager de empresa diferente")
    void shouldRejectManagerEnrollingFromDifferentCompany() {
        UUID managerId = UUID.randomUUID();
        UUID targetEmployeeId = UUID.randomUUID();
        UUID companyId1 = UUID.randomUUID();
        UUID companyId2 = UUID.randomUUID();
        String validBase64 = Base64.getEncoder().encodeToString("facedata".getBytes());

        var manager = buildEmployee(managerId, companyId1);
        var targetEmployee = buildEmployee(targetEmployeeId, companyId2);

        RegisterFaceRequest request = new RegisterFaceRequest(validBase64, targetEmployeeId, true);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerId);
        when(employeeProvider.findById(managerId)).thenReturn(Optional.of(manager));
        when(employeeProvider.findById(targetEmployeeId)).thenReturn(Optional.of(targetEmployee));

        assertThrows(ResourceNotFoundException.class, () -> service.enrollBiometricByManager(targetEmployeeId, request));
        verify(acceptTermsUseCase, never()).getBiometricConsentStatus(any());
        verify(biometricProtectionService, never()).protectEnrollment(any(), anyString(), any());
    }

    @Test
    @DisplayName("enrollBiometricByManager: registra auditoria com REPLACED quando há biometria anterior")
    void shouldRegisterAuditOnBiometricReplacement() {
        UUID managerId = UUID.randomUUID();
        UUID targetEmployeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        String validBase64 = Base64.getEncoder().encodeToString("facedata".getBytes());

        var manager = buildEmployee(managerId, companyId);
        var targetEmployee = new Employee(
                targetEmployeeId, "John Doe", "12345678901", "1234567890",
                "Dev", "john@kts.com", 1000.0, "21999999999", true,
                null, companyId, null, false,
                "faces/old.jpg", LocalTime.of(8, 0), LocalTime.of(17, 0),
                LocalTime.of(12, 0), LocalTime.of(13, 0),
                null, null, null, null, null
        );

        RegisterFaceRequest request = new RegisterFaceRequest(validBase64, targetEmployeeId, true);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerId);
        when(employeeProvider.findById(managerId)).thenReturn(Optional.of(manager));
        when(employeeProvider.findById(targetEmployeeId)).thenReturn(Optional.of(targetEmployee));
        when(acceptTermsUseCase.getBiometricConsentStatus(targetEmployeeId))
                .thenReturn(new BiometricConsentStatus(true, "v1", "hash1", "v1", "hash1", false));
        when(faceStorageProvider.uploadFaceImage(any(), any(), anyString()))
                .thenReturn("s3-key-new");
        when(faceRecognitionProvider.indexFace("s3-key-new", targetEmployeeId))
                .thenReturn("face-id-new");
        when(employeeProvider.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtAuthenticatedUser.getuserId()).thenReturn(UUID.randomUUID());

        service.enrollBiometricByManager(targetEmployeeId, request);

        verify(auditService).register(
                eq(AuditAction.BIOMETRIC_ENROLLMENT_REPLACED_BY_MANAGER),
                any(UUID.class),
                eq(targetEmployeeId),
                eq(companyId),
                anyString(),
                anyString(),
                anyString(),
                any(),
                any(),
                anyString()
        );
    }
}
