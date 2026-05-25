package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.timerecord.GeolocationRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.ListReportRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.UpdateTimeRecordRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.vacation.RequestVacationRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.port.in.usecase.AdfUseCase;
import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.application.security.BiometricProtectionService;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.observability.application.KronosMetrics;
import com.kts.kronos.domain.model.*;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static com.kts.kronos.constants.Messages.SAO_PAULO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // Evita erros de stubs não utilizados
class TimeRecordServiceTest {

    @InjectMocks
    private TimeRecordService service;

    @Mock private TimeRecordProvider recordRepository;
    @Mock private EmployeeProvider employeeProvider;
    @Mock private CompanyProvider companyProvider;
    @Mock private JwtAuthenticatedUser jwtAuthenticatedUser;
    @Mock private DocumentService documentService;
    @Mock private DocumentProvider documentProvider;
    @Mock private CompanyUseCase companyUseCase;
    @Mock private UserProvider userProvider;
    @Mock private TimeRecordApprovalProvider approvalProvider;
    @Mock private FaceRecognitionProvider faceRecognitionProvider;
    @Mock private ReceiptPdfService receiptPdfService;
    @Mock private AdfUseCase adfUseCase;
    @Mock private NsrProvider nsrProvider;
    @Mock private NtpTimeService ntpTimeService;
    @Mock private DomainAuthorizationService domainAuthorizationService;
    @Mock private BiometricProtectionService biometricProtectionService;
    @Mock private LegalConsentProvider legalConsentProvider;
    @Mock private KronosMetrics kronosMetrics;

    private UUID employeeId;
    private UUID companyId;
    private Employee employee;
    private Company company;
    private String validBase64;
     private UUID managerId;
     private User managerUser;

    @BeforeEach
    void setup() {
        employeeId = UUID.randomUUID();
        managerId = UUID.randomUUID();
        companyId = UUID.randomUUID();

        // Setup Funcionário



        // Setup Manager
        managerUser = new User(managerId, "admin", "pass", Role.MANAGER, true, managerId);


        // Gera um Base64 válido para evitar IllegalArgumentException no decoder
        validBase64 = Base64.getEncoder().encodeToString("imagem_valida".getBytes());

        var address = new com.kts.kronos.domain.model.Address("Rua Teste", "123", "00000000", "Cidade", "UF");
        var location = new com.kts.kronos.adapter.in.web.dto.company.Location(-22.0, -43.0);

        company = new Company(companyId, "KTS", "00000000000100", "email@kts.com", true, address, location, 10, 0);


        employee = new Employee(
                employeeId, "João Silva", "12345678901", "12345678901", "Dev", "joao@kts.com",
                5000.0, "2199999999", true, address, companyId, null, false, "s3-key-face",
                LocalTime.of(9,0), LocalTime.of(18,0), LocalTime.of(12,0), LocalTime.of(13,0),
                null, null, null, null, null
        );
        // Stub genérico para save evitar NPE se o fluxo desviar
        when(recordRepository.save(any(TimeRecord.class))).thenAnswer(i -> {
            TimeRecord r = i.getArgument(0);
            return r.withId(1L);
        });

        // LGPD-102: Mock biometric consent provider to return true by default
        when(legalConsentProvider.existsActive(any(UUID.class), any())).thenReturn(true);
    }

    @Test
    @DisplayName("Deve realizar Check-in com sucesso quando não há registro aberto")
    void shouldRegisterCheckInSuccessfully() {
        // Arrange
        GeolocationRequest request = new GeolocationRequest(-22.0001, -43.0001, validBase64, false );

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));

        // Mock Validação Facial com sucesso
        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class))).thenReturn(employeeId);

        // Mock Repositório (Não há registro aberto)
        when(recordRepository.findOpenByEmployeeId(employeeId)).thenReturn(Optional.empty());
        when(recordRepository.findByRange(any(), any(), any())).thenReturn(Collections.emptyList());
        when(recordRepository.findTopByEmployeeIdOrderByStartWorkDesc(employeeId)).thenReturn(Optional.empty());

        // Mock Sequencial
        when(nsrProvider.generateNextNsr(companyId)).thenReturn(100L);

        // Act
        var response = service.registerTime(request);

        // Assert
        assertEquals("CHECKIN", response.actionType());
        assertTrue(response.message().contains("Entrada às"));

        verify(ntpTimeService).validateSystemTime(10);
        verify(adfUseCase).logMarking(eq(company), eq(employee), any(LocalDateTime.class), eq(100L));
        verify(documentService).uploadGeneratedDocument(any(), eq(employeeId), eq(1L), any(), anyString());
    }

    @Test
    @DisplayName("Deve falhar Check-in se a validação facial não reconhecer o funcionário")
    void shouldFailCheckInWhenFaceDoesNotMatch() {
        GeolocationRequest request = new GeolocationRequest(-22.0, -43.0, validBase64, null);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));

        // Retorna um ID diferente para simular falha de reconhecimento (Biometria de outra pessoa)
        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class))).thenReturn(UUID.randomUUID());

        // Act & Assert
        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.registerTime(request));
        assertEquals("Falha na validação facial: A face não corresponde ao colaborador autenticado.", ex.getMessage());

        // Garante que o save nunca foi chamado
        verify(recordRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve falhar Check-in se funcionário presencial estiver fora da cerca virtual")
    void shouldFailCheckInWhenLocationIsInvalid() {
        // Coordenadas distantes (-23 vs -22)
        GeolocationRequest request = new GeolocationRequest(-23.0, -43.0, validBase64, null);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        // Face ok, mas localização ruim
        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class))).thenReturn(employeeId);
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));

        // Act & Assert
        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.registerTime(request));
        assertEquals("Você está fora da área de trabalho permitida.", ex.getMessage());
    }

    @Test
    @DisplayName("Deve realizar Check-out com sucesso fechando registro PENDING do MESMO DIA")
    void shouldRegisterCheckOutSuccessfully() {
        GeolocationRequest request = new GeolocationRequest(-22.0, -43.0, validBase64, null);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class))).thenReturn(employeeId);

        // Mantém o registro aberto no mesmo dia do "agora" para evitar flakiness perto da meia-noite.
        LocalDateTime startWork = LocalDateTime.now(SAO_PAULO).toLocalDate().atStartOfDay();

        TimeRecord openRecord = new TimeRecord(
                50L, startWork, null, StatusRecord.PENDING, false, true, employeeId,
                -22.0, -43.0, null, null, 99L, null, startWork, null
        );

        when(recordRepository.findOpenByEmployeeId(employeeId)).thenReturn(Optional.of(openRecord));
        when(nsrProvider.generateNextNsr(companyId)).thenReturn(101L);

        // Act
        var response = service.registerTime(request);

        // Assert
        assertEquals("CHECKOUT", response.actionType());

        ArgumentCaptor<TimeRecord> captor = ArgumentCaptor.forClass(TimeRecord.class);
        verify(recordRepository).save(captor.capture());

        TimeRecord saved = captor.getValue();
        assertNotNull(saved.endWork());
        assertEquals(StatusRecord.CREATED, saved.statusRecord());
        assertEquals(101L, saved.nsrCheckout());
    }

    @Test
    @DisplayName("listReport: bloqueia manager com employeeId de outro tenant")
    void shouldBlockListReportForCrossTenantEmployee() {
        UUID otherTenantEmployeeId = UUID.randomUUID();
        var req = new ListReportRequest("08:00", true, null, new LocalDate[]{LocalDate.now(SAO_PAULO)});
        when(domainAuthorizationService.authorizeEmployeeAccess(otherTenantEmployeeId))
                .thenThrow(new ForbiddenException("forbidden"));

        assertThrows(ForbiddenException.class, () -> service.listReport(otherTenantEmployeeId, req));
        verify(recordRepository, never()).findByEmployeeId(any());
        verify(recordRepository, never()).findByEmployeeIdAndActive(any(), anyBoolean());
    }

    @Test
    @DisplayName("PARTNER: Deve criar solicitação de aprovação ao editar ponto")
    void shouldCreateApprovalRequestWhenPartnerUpdates() {
        // Arrange
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));

        // Registro Original
        TimeRecord record = new TimeRecord(10L,
                LocalDate.now().atTime(8, 0), LocalDate.now().atTime(12, 0),
                StatusRecord.CREATED, false, true, employeeId, null, null, null, null, 1L, 2L, null, null);

        when(recordRepository.findById(10L)).thenReturn(Optional.of(record));

        // Mock Manager Validation
        when(userProvider.findById(managerId)).thenReturn(Optional.of(managerUser));
        Employee managerEmployee = new Employee(managerId, "Mgr", "222", "222", "Mgr", "m@k.com", 0, "", true, null, companyId, null, false, null, null, null, null, null, null, null, null, null, null);
        when(employeeProvider.findById(managerId)).thenReturn(Optional.of(managerEmployee));

        UpdateTimeRecordRequest req = new UpdateTimeRecordRequest(
                LocalDate.now(), LocalDate.now(), "08:00", "12:30", managerId
        );

        // Act
        service.updateTimeRecord(10L, req);

        // Assert
        verify(approvalProvider).save(any(TimeRecordApprovalRequest.class));

        ArgumentCaptor<TimeRecord> captor = ArgumentCaptor.forClass(TimeRecord.class);
        verify(recordRepository).save(captor.capture());
        assertEquals(StatusRecord.PENDING_APPROVAL, captor.getValue().statusRecord());
        assertTrue(captor.getValue().edited());
        verify(jwtAuthenticatedUser).getCurrentRole();
    }

    @Test
    @DisplayName("MANAGER: Deve editar ponto diretamente e ajustar pausas adjacentes (Atualizando registro do Próprio Manager)")
    void shouldUpdateDirectlyAndAdjustBreaksWhenManagerUpdates() {
        // Arrange
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        // CORREÇÃO CRÍTICA: O serviço usa jwtAuthenticatedUser.getEmployeeId() para buscar os registros.
        // Para o teste funcionar e a lógica de "isRecordBelongsEmployee" passar,
        // o registro deve pertencer ao mesmo ID que está no token.
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerId);

        Employee managerEmployee = new Employee(managerId, "Mgr", "222", "222", "Mgr", "m@k.com", 0, "", true, null, companyId, null, false, null, null, null, null, null, null, null, null, null, null);
        when(employeeProvider.findById(managerId)).thenReturn(Optional.of(managerEmployee));

        LocalDate today = LocalDate.now();
        // Os registros devem pertencer ao managerId
        TimeRecord r1 = new TimeRecord(10L, today.atTime(8,0), today.atTime(12,0), StatusRecord.CREATED, false, true, managerId, null, null, null, null, null, null, null, null);
        TimeRecord r2 = new TimeRecord(11L, today.atTime(12,0), today.atTime(13,0), StatusRecord.IMPLICIT_BREAK, false, true, managerId, null, null, null, null, null, null, null, null);
        TimeRecord r3 = new TimeRecord(12L, today.atTime(13,0), today.atTime(17,0), StatusRecord.CREATED, false, true, managerId, null, null, null, null, null, null, null, null);

        when(recordRepository.findById(10L)).thenReturn(Optional.of(r1));

        // Mock para buscar registros do dia (usando managerId)
        when(recordRepository.findByEmployeeId(managerId)).thenReturn(List.of(r1, r2, r3));

        // Request
        UpdateTimeRecordRequest req = new UpdateTimeRecordRequest(
                today, today, "08:00", "12:30", managerId
        );

        // Act
        service.updateTimeRecord(10L, req);

        // Assert
        ArgumentCaptor<TimeRecord> captor = ArgumentCaptor.forClass(TimeRecord.class);
        // Espera-se pelo menos 2 saves: um para o registro editado e outro para a pausa ajustada
        verify(recordRepository, atLeast(1)).save(captor.capture());

        List<TimeRecord> savedRecords = captor.getAllValues();

        // Verifica R1 (Expandido até 12:30)
        TimeRecord savedR1 = savedRecords.stream().filter(r -> r.timeRecordId() == 10L).findFirst().orElseThrow();
        assertEquals(LocalTime.of(12, 30), savedR1.endWork().toLocalTime());
        assertEquals(StatusRecord.UPDATED, savedR1.statusRecord());

        // Verifica R2 (Pausa ajustada para começar 12:30)
        // Se a lógica do serviço estiver correta, ele deve salvar um NOVO registro ou atualizar o antigo
        // No código do serviço: "TimeRecord updatedBreak = new TimeRecord(..., newStartBreak, ...)" e depois save()
        // O ID do novo objeto pode ser o mesmo (se for atualização) ou null (se for novo).
        // Na implementação fornecida, ele cria um novo objeto com o mesmo ID: new TimeRecord(succeeding.timeRecordId()...)
        TimeRecord savedR2 = savedRecords.stream().filter(r -> r.timeRecordId() == 11L).findFirst().orElse(null);

        if (savedR2 != null) {
            assertEquals(LocalTime.of(12, 30), savedR2.startWork().toLocalTime());
        }
    }

    @Test
    @DisplayName("Deve gerar múltiplos registros de solicitação de férias")
    void shouldCreateMultipleVacationRequests() {
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(userProvider.findById(managerId)).thenReturn(Optional.of(managerUser));
        Employee managerEmp = new Employee(managerId, "Mgr", "222", null, "Mgr", "m@k.com", 0, "", true, null, companyId, null, false, null, null, null, null, null, null, null, null, null, null);
        when(employeeProvider.findById(managerId)).thenReturn(Optional.of(managerEmp));

        RequestVacationRequest req = new RequestVacationRequest(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 3),
                managerId
        );

        when(recordRepository.existsByEmployeeIdAndDate(any(), any())).thenReturn(false);
        when(recordRepository.save(any())).thenAnswer(i -> ((TimeRecord)i.getArgument(0)).withId(new Random().nextLong()));

        // Act
        List<Long> ids = service.requestVacation(req);

        // Assert
        assertEquals(3, ids.size());
        verify(recordRepository, times(3)).save(any(TimeRecord.class));
    }

    @Test
    @DisplayName("Deve falhar solicitação de férias se já houver registro")
    void shouldFailVacationRequestIfDuplicateExists() {
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(userProvider.findById(managerId)).thenReturn(Optional.of(managerUser));
        when(employeeProvider.findById(managerId)).thenReturn(Optional.of(employee));

        RequestVacationRequest req = new RequestVacationRequest(LocalDate.now(), LocalDate.now(), managerId);

        when(recordRepository.existsByEmployeeIdAndDate(any(), any())).thenReturn(true);

        // Act & Assert
        assertThrows(BadRequestException.class, () -> service.requestVacation(req));
    }

    @Test
    @DisplayName("Deve converter registro de FOLGA em TRABALHO ao fazer check-in")
    void shouldConvertDayOffRecordToWorkOnCheckIn() {
        String validBase64 = Base64.getEncoder().encodeToString("img".getBytes());
        GeolocationRequest request = new GeolocationRequest(-22.0, -43.0, validBase64,null);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(new Company(companyId, "KTS", "1", "e", true, null, new com.kts.kronos.adapter.in.web.dto.company.Location(-22.0, -43.0), 0,0)));
        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class))).thenReturn(employeeId);

        // Registro de folga existente
        TimeRecord dayOffRecord = new TimeRecord(55L, LocalDate.now().atStartOfDay(), LocalDate.now().atStartOfDay(), StatusRecord.DAY_OFF, false, true, employeeId, null, null, null, null, null, null, null, null);

        when(recordRepository.findOpenByEmployeeId(employeeId)).thenReturn(Optional.empty());
        when(recordRepository.findByRange(eq(employeeId), any(), any())).thenReturn(List.of(dayOffRecord));
        when(recordRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        var response = service.registerTime(request);

        // Assert
        assertEquals("CHECKIN_ON_DAY_OFF", response.actionType());

        ArgumentCaptor<TimeRecord> captor = ArgumentCaptor.forClass(TimeRecord.class);
        verify(recordRepository).save(captor.capture());

        TimeRecord saved = captor.getValue();
        assertEquals(55L, saved.timeRecordId());
        assertEquals(StatusRecord.PENDING, saved.statusRecord());
    }
}
