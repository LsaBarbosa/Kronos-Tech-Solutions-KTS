package com.kts.kronos.application.service;
import com.kts.kronos.adapter.in.web.dto.timerecord.GeolocationRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.RequestTimeOffRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.TimeOffApprovalRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.UpdateTimeRecordRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.vacation.RequestVacationRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.vacation.VacationApprovalRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.port.in.usecase.AdfUseCase;
import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.domain.model.*;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.RequestType;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static com.kts.kronos.constants.Logs.ERR_CHECKOUT_STATUS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeRecordServiceTest {

    @InjectMocks
    private TimeRecordService timeRecordService;

    @Mock private TimeRecordProvider timeRecordProvider;
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

    // Constantes de Teste
    private UUID employeeId;
    private UUID companyId;
    private UUID managerId;
    private Employee mockEmployee;
    private Company mockCompany;
    private User mockManagerUser;

    @BeforeEach
    void setUp() {
        employeeId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        managerId = UUID.randomUUID();

        // 1. Configuração do Funcionário com construtor completo (23 campos)
        mockEmployee = new Employee(
                employeeId, "João Silva", "12345678900", "1234567890", "Desenvolvedor",
                "joao@email.com", 5000.0, "11999999999", true, null, companyId,
                LocalDateTime.now(), true, null, LocalTime.of(9, 0), LocalTime.of(18, 0),
                LocalTime.of(12, 0), LocalTime.of(13, 0), null, null, null, null, null
        );

        // 2. Configuração da Empresa com construtor completo (9 campos)
        mockCompany = new Company(
                companyId, "Kronos Tech", "00.000.000/0001-00", "contato@kronos.com",
                true, null, null, 10L, 0L
        );

        // 3. Configuração de um Gestor para aprovações
        mockManagerUser = new User(
                employeeId, "gestor.silva", "password",Role.MANAGER, true,managerId
        );
    }

    // =================================================================================
    // 1. REGISTRO DE PONTO (CHECK-IN / CHECK-OUT)
    // =================================================================================

    @Test
    @DisplayName("Deve registrar Check-in com sucesso para funcionário em Home Office")
    void shouldRegisterCheckinSuccessfully() throws Exception {
        var base64Image = Base64.getEncoder().encodeToString("dummyImage".getBytes());
        var request = new GeolocationRequest(-22.9, -43.2, base64Image);

        doNothing().when(ntpTimeService).validateSystemTime(10);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(mockEmployee));
        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class))).thenReturn(employeeId);
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(mockCompany));

        when(timeRecordProvider.findOpenByEmployeeId(employeeId)).thenReturn(Optional.empty());
        when(timeRecordProvider.findFirstByEmployeeIdAndStartWorkBetweenAndStatusIn(eq(employeeId), any(), any(), anySet())).thenReturn(Optional.empty());
        when(timeRecordProvider.findTopByEmployeeIdOrderByStartWorkDesc(employeeId)).thenReturn(Optional.empty());

        when(nsrProvider.generateNextNsr(companyId)).thenReturn(100L);

        var savedRecord = new TimeRecord(1L, LocalDateTime.now(), null, StatusRecord.PENDING, false, true, employeeId, null, null, null, null, 100L, null, LocalDateTime.now(), null);
        when(timeRecordProvider.save(any(TimeRecord.class))).thenReturn(savedRecord);
        when(receiptPdfService.generateReceipt(any(), any(), any(), any())).thenReturn("pdfBytes".getBytes());

        var response = timeRecordService.registerTime(request);

        assertNotNull(response);
        assertEquals("CHECKIN", response.actionType());
        verify(timeRecordProvider).save(any(TimeRecord.class));
    }

    // =================================================================================
    // 2. ATUALIZAÇÃO E APROVAÇÃO MANUAL
    // =================================================================================

    @Test
    @DisplayName("Gestor deve atualizar o ponto diretamente sem aprovação")
    void shouldUpdateRecordByManagerSuccessfully() {
        Long recordId = 10L;
        var request = new UpdateTimeRecordRequest(LocalDate.now(), LocalDate.now(), "09:00", "18:00", managerId);
        var existingRecord = new TimeRecord(recordId, LocalDateTime.now().minusHours(8), LocalDateTime.now(), StatusRecord.CREATED, false, true, employeeId, null, null, null, null, 1L, 2L, null, null);

        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("MANAGER");
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(mockEmployee));
        when(timeRecordProvider.findById(recordId)).thenReturn(Optional.of(existingRecord));
        when(timeRecordProvider.findByEmployeeId(employeeId)).thenReturn(Collections.emptyList());

        timeRecordService.updateTimeRecord(recordId, request);

        verify(timeRecordProvider).save(argThat(record ->
                record.statusRecord() == StatusRecord.UPDATED && record.edited()
        ));
    }

    // =================================================================================
    // 3. FLUXO DE FÉRIAS (VACATION)
    // =================================================================================

    @Test
    @DisplayName("Deve solicitar férias com sucesso e criar registros pendentes em lote")
    void shouldRequestVacationSuccessfully() {
        var startDate = LocalDate.now().plusDays(5);
        var endDate = LocalDate.now().plusDays(7); // 3 dias de férias
        var request = new RequestVacationRequest(startDate, endDate, managerId);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(mockEmployee));
        when(userProvider.findById(managerId)).thenReturn(Optional.of(mockManagerUser));
        when(employeeProvider.findById(mockManagerUser.employeeId())).thenReturn(Optional.of(mockEmployee)); // Simula que são da mesma empresa
        when(timeRecordProvider.findByRange(eq(employeeId), any(), any())).thenReturn(Collections.emptyList()); // Sem conflitos

        // Simula o retorno do batch insert
        var mockSavedRecords = List.of(
                new TimeRecord(100L, startDate.atStartOfDay(), null, StatusRecord.REQUEST_VACATION, false, true, employeeId, null, null, null, null, null, null, null, null)
        );
        when(timeRecordProvider.saveAll(anyList())).thenReturn(mockSavedRecords);

        var response = timeRecordService.requestVacation(request);

        assertFalse(response.isEmpty());
        assertEquals(1, response.size());
        verify(timeRecordProvider).saveAll(argThat(list -> list.size() == 3)); // Verifica se mandou salvar os 3 dias
    }

    @Test
    @DisplayName("Gestor deve aprovar solicitações de férias em lote")
    void shouldApproveVacationInBatchSuccessfully() {
        var request = new VacationApprovalRequest(List.of(100L, 101L));
        var record1 = new TimeRecord(100L, LocalDateTime.now(), null, StatusRecord.REQUEST_VACATION, false, true, employeeId, null, null, null, null, null, null, null, null);
        var record2 = new TimeRecord(101L, LocalDateTime.now(), null, StatusRecord.REQUEST_VACATION, false, true, employeeId, null, null, null, null, null, null, null, null);

        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("MANAGER");
        when(timeRecordProvider.findByIdIn(anySet())).thenReturn(List.of(record1, record2));
        when(timeRecordProvider.saveAll(anyList())).thenReturn(List.of(record1, record2));

        timeRecordService.approveVacation(request);

        verify(timeRecordProvider).saveAll(argThat(list ->
                list.size() == 2 && list.get(0).statusRecord() == StatusRecord.VACATION
        ));
    }

    // =================================================================================
    // 4. FLUXO DE ABONOS / ESQUECIMENTOS (TIME OFF)
    // =================================================================================

    @Test
    @DisplayName("Deve solicitar abono com upload de documento em lote com sucesso")
    void shouldRequestTimeOffWithDocumentSuccessfully() throws Exception {
        var startDate = LocalDate.now();
        var endDate = LocalDate.now().plusDays(1); // 2 dias de atestado
        var request = new RequestTimeOffRequest(startDate, endDate, "09:00", "18:00", managerId, RequestType.TIME_OFF_REQUEST);
        MultipartFile document = new MockMultipartFile("file", "atestado.pdf", "application/pdf", "content".getBytes());

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(mockEmployee));
        when(userProvider.findById(managerId)).thenReturn(Optional.of(mockManagerUser));
        when(employeeProvider.findById(mockManagerUser.employeeId())).thenReturn(Optional.of(mockEmployee));

        var record1 = new TimeRecord(500L, startDate.atTime(9, 0), startDate.atTime(18,0), StatusRecord.TIME_OFF_REQUEST, true, true, employeeId, null, null, null, null, null, null, null, null);
        var record2 = new TimeRecord(501L, endDate.atTime(9, 0), endDate.atTime(18,0), StatusRecord.TIME_OFF_REQUEST, true, true, employeeId, null, null, null, null, null, null, null, null);

        when(timeRecordProvider.saveAll(anyList())).thenReturn(List.of(record1, record2));

        // Mock do documento associado ao primeiro dia
        var mockDoc = new Document(employeeId, DocumentType.TIME_OFF, "atestado.pdf", "application/pdf", "/path", LocalDateTime.now(), 500L, false, false);
        when(documentProvider.findByTimeRecordId(500L)).thenReturn(List.of(mockDoc));

        var resultId = timeRecordService.requestTimeOff(request, document);

        assertEquals(500L, resultId);
        verify(timeRecordProvider).saveAll(anyList());
        verify(documentService).uploadDocumentForTimeRecord(eq(DocumentType.TIME_OFF), eq(employeeId), eq(500L), eq(document));
        verify(documentProvider).saveAll(argThat(docs -> docs.size() == 1)); // Replicou o documento para o dia 2
    }

    @Test
    @DisplayName("Gestor deve aprovar solicitações de abono e esquecimento simultaneamente em lote")
    void shouldApproveTimeOffInBatchSuccessfully() {
        var request = new TimeOffApprovalRequest(List.of(600L, 601L));
        // Dia 1: Atestado Médico (TIME_OFF)
        var record1 = new TimeRecord(600L, LocalDateTime.now(), LocalDateTime.now(), StatusRecord.TIME_OFF_REQUEST, true, true, employeeId, null, null, null, null, null, null, null, null);
        // Dia 2: Esquecimento de Ponto (WORK_TIME_REQUEST)
        var record2 = new TimeRecord(601L, LocalDateTime.now(), LocalDateTime.now(), StatusRecord.WORK_TIME_REQUEST, true, true, employeeId, null, null, null, null, null, null, null, null);

        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("MANAGER");
        when(timeRecordProvider.findByIdIn(anySet())).thenReturn(List.of(record1, record2));

        timeRecordService.approveTimeOff(request);

        verify(timeRecordProvider).saveAll(argThat(list -> {
            boolean hasTimeOff = list.stream().anyMatch(r -> r.statusRecord() == StatusRecord.TIME_OFF);
            boolean hasUpdated = list.stream().anyMatch(r -> r.statusRecord() == StatusRecord.UPDATED);
            return list.size() == 2 && hasTimeOff && hasUpdated;
        }));
    }
    @Test
    @DisplayName("Deve falhar Check-in se o reconhecimento facial não corresponder ao funcionário")
    void shouldThrowExceptionWhenFaceRecognitionFails() throws Exception {
        var base64Image = Base64.getEncoder().encodeToString("dummyImage".getBytes());
        var request = new GeolocationRequest(-22.9, -43.2, base64Image);

        doNothing().when(ntpTimeService).validateSystemTime(10);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);

        // CORREÇÃO 1: Ensinar o mock a encontrar o funcionário para passar pela primeira validação
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(mockEmployee));

        // Simula a IA a reconhecer OUTRO UUID de funcionário
        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class))).thenReturn(UUID.randomUUID());

        // CORREÇÃO 2: Voltar para BadRequestException.class, que é o que o faceProvider lança
        var exception = assertThrows(com.kts.kronos.application.exceptions.BadRequestException.class,
                () -> timeRecordService.registerTime(request));

        assertEquals(com.kts.kronos.constants.Messages.FACE_MISMATCH, exception.getMessage());
        verify(timeRecordProvider, never()).save(any()); // Garante que não salvou nada
    }

    @Test
    @DisplayName("Deve falhar Check-in se a geolocalização estiver fora do raio permitido (Não Home Office)")
    void shouldThrowExceptionWhenGeolocationIsOutOfRange() throws Exception {
        var base64Image = Base64.getEncoder().encodeToString("dummyImage".getBytes());
        // Coordenadas absurdas (longe da empresa)
        var request = new GeolocationRequest(90.0, 180.0, base64Image);

        // Criamos um funcionário que NÃO é Home Office
        var officeEmployee = new Employee(
                employeeId, "João Presencial", "12345678900", "1234567890", "Desenvolvedor",
                "joao@email.com", 5000.0, "11999999999", true, null, companyId,
                LocalDateTime.now(), false, null, LocalTime.of(9, 0), LocalTime.of(18, 0),
                LocalTime.of(12, 0), LocalTime.of(13, 0), null, null, null, null, null
        );

        // Criamos uma empresa com localização fixa
        var companyLocation = new com.kts.kronos.adapter.in.web.dto.company.Location(-22.9, -43.2);
        var compWithLoc = new Company(companyId, "Kronos Tech", "cnpj", "e@m.com", true, null, companyLocation, 10, 0);

        doNothing().when(ntpTimeService).validateSystemTime(10);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(officeEmployee));
        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class))).thenReturn(employeeId);
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(compWithLoc));

        var exception = assertThrows(com.kts.kronos.application.exceptions.BadRequestException.class,
                () -> timeRecordService.registerTime(request));

        assertEquals(com.kts.kronos.constants.Messages.GEOLOCATION_OUT_OF_RANGE, exception.getMessage());
        verify(timeRecordProvider, never()).save(any());
    }

    @Test
    @DisplayName("Deve falhar Atualização Manual se houver inconsistência de horas (Início maior que Fim)")
    void shouldThrowExceptionWhenTimeInconsistencyOnUpdate() {
        Long recordId = 10L;
        // Início às 18:00 e Fim às 09:00 no MESMO DIA
        var request = new UpdateTimeRecordRequest(LocalDate.now(), LocalDate.now(), "18:00", "09:00", managerId);
        var existingRecord = new TimeRecord(recordId, LocalDateTime.now().minusHours(8), LocalDateTime.now(), StatusRecord.CREATED, false, true, employeeId, null, null, null, null, 1L, 2L, null, null);

        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("MANAGER");
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(mockEmployee));
        when(timeRecordProvider.findById(recordId)).thenReturn(Optional.of(existingRecord));

        assertThrows(com.kts.kronos.application.exceptions.BadRequestException.class,
                () -> timeRecordService.updateTimeRecord(recordId, request));

        verify(timeRecordProvider, never()).save(any());
    }

    @Test
    @DisplayName("Deve falhar Solicitação de Férias se já existirem pontos batidos (Conflito)")
    void shouldThrowExceptionWhenRequestVacationWithConflict() {
        var startDate = LocalDate.now().plusDays(5);
        var endDate = LocalDate.now().plusDays(7);
        var request = new RequestVacationRequest(startDate, endDate, managerId);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(mockEmployee));
        when(userProvider.findById(managerId)).thenReturn(Optional.of(mockManagerUser));
        when(employeeProvider.findById(mockManagerUser.employeeId())).thenReturn(Optional.of(mockEmployee));

        // Simula que JÁ EXISTE um registro de ponto nesses dias
        var conflictingRecord = new TimeRecord(employeeId);
        when(timeRecordProvider.findByRange(eq(employeeId), any(), any())).thenReturn(List.of(conflictingRecord));

        assertThrows(com.kts.kronos.application.exceptions.BadRequestException.class,
                () -> timeRecordService.requestVacation(request));

        verify(timeRecordProvider, never()).saveAll(any());
    }

    @Test
    @DisplayName("Deve falhar Aprovação de Férias se o utilizador não for MANAGER")
    void shouldThrowExceptionWhenNonManagerTriesToApproveVacation() {
        var request = new VacationApprovalRequest(List.of(100L));

        // Simula um funcionário comum tentando fazer a aprovação
        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("EMPLOYEE");

        assertThrows(com.kts.kronos.application.exceptions.ForbiddenException.class,
                () -> timeRecordService.approveVacation(request));

        verify(timeRecordProvider, never()).saveAll(any());
    }

    @Test
    @DisplayName("Deve falhar Aprovação em Lote (Abono) se a quantidade de IDs encontrados for menor que a solicitada")
    void shouldThrowExceptionWhenApproveTimeOffBatchMismatch() {
        // Pedido de aprovação para 2 registros
        var request = new TimeOffApprovalRequest(List.of(600L, 601L));

        // Mas a base de dados só encontrou 1 registro
        var record1 = new TimeRecord(600L, LocalDateTime.now(), LocalDateTime.now(), StatusRecord.TIME_OFF_REQUEST, true, true, employeeId, null, null, null, null, null, null, null, null);

        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("MANAGER");
        when(timeRecordProvider.findByIdIn(anySet())).thenReturn(List.of(record1));

        assertThrows(com.kts.kronos.application.exceptions.ResourceNotFoundException.class,
                () -> timeRecordService.approveTimeOff(request));

        verify(timeRecordProvider, never()).saveAll(any());
    }


    @Test
    @DisplayName("Deve falhar Atualização Manual se o registro pertencer a OUTRO funcionário")
    void shouldThrowExceptionWhenUpdatingRecordFromAnotherEmployee() {
        Long recordId = 15L;
        UUID anotherEmployeeId = UUID.randomUUID();
        var request = new UpdateTimeRecordRequest(LocalDate.now(), LocalDate.now(), "09:00", "18:00", managerId);

        // Criamos o registro pertencendo a "anotherEmployeeId" em vez do "employeeId" do utilizador logado
        var existingRecord = new TimeRecord(recordId, LocalDateTime.now().minusHours(8), LocalDateTime.now(), StatusRecord.CREATED, false, true, anotherEmployeeId, null, null, null, null, 1L, 2L, null, null);

        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("PARTNER");
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(mockEmployee));
        when(timeRecordProvider.findById(recordId)).thenReturn(Optional.of(existingRecord));

        var exception = assertThrows(com.kts.kronos.application.exceptions.BadRequestException.class,
                () -> timeRecordService.updateTimeRecord(recordId, request));

        assertEquals(com.kts.kronos.constants.Messages.RECORD_NOT_BELONGS_EMPLOYEE, exception.getMessage());
        verify(timeRecordProvider, never()).save(any());
    }

    @Test
    @DisplayName("Deve falhar Aprovação de Ajuste de Ponto se o registro não estiver PENDING_APPROVAL")
    void shouldThrowExceptionWhenApprovingRecordNotPendingApproval() {
        Long recordId = 25L;
        // Registro com status já atualizado, ou seja, alguém já o aprovou antes
        var recordAlreadyUpdated = new TimeRecord(recordId, LocalDateTime.now(), LocalDateTime.now().plusHours(8), StatusRecord.UPDATED, true, true, employeeId, null, null, null, null, 1L, 2L, null, null);

        when(timeRecordProvider.findById(recordId)).thenReturn(Optional.of(recordAlreadyUpdated));

        var exception = assertThrows(com.kts.kronos.application.exceptions.BadRequestException.class,
                () -> timeRecordService.approveTimeRecordChange(recordId));

        assertEquals(com.kts.kronos.constants.Messages.RECORD_IS_NOT_AWAITING_APPROVAL, exception.getMessage());
        verify(timeRecordProvider, never()).save(any());
    }

//    @Test
//    @DisplayName("Deve falhar Exclusão (Delete) de Ponto se o status estiver CLOSED")
//    void shouldThrowExceptionWhenDeletingClosedRecord() {
//        Long recordId = 30L;
//        // Registro fechado (provavelmente já foi para folha de pagamento)
//        var closedRecord = new TimeRecord(recordId, LocalDateTime.now(), LocalDateTime.now(), StatusRecord.CLOSED, false, true, employeeId, null, null, null, null, 1L, 2L, null, null);
//
//        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
//        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(mockEmployee));
//        when(timeRecordProvider.findById(recordId)).thenReturn(Optional.of(closedRecord));
//
//        var exception = assertThrows(com.kts.kronos.application.exceptions.BadRequestException.class,
//                () -> timeRecordService.deleteTimeRecord(employeeId, recordId));
//
//        assertTrue(exception.getMessage().contains("fechado"));
//        verify(timeRecordProvider, never()).deleteTimeRecord(any());
//    }

    @Test
    @DisplayName("Deve falhar Solicitação de Abono se a data de início for maior que a data de fim")
    void shouldThrowExceptionWhenTimeOffDatesAreInvalid() throws Exception {
        // Data de Início: Daqui a 10 dias. Data de Fim: Hoje (Inversão Lógica)
        var startDate = LocalDate.now().plusDays(10);
        var endDate = LocalDate.now();
        var request = new RequestTimeOffRequest(startDate, endDate, "09:00", "18:00", managerId, RequestType.TIME_OFF_REQUEST);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(mockEmployee));

        var exception = assertThrows(com.kts.kronos.application.exceptions.BadRequestException.class,
                () -> timeRecordService.requestTimeOff(request, null));

        assertEquals(com.kts.kronos.constants.Messages.START_DATE_BIGGER_THAN_END_DATE, exception.getMessage());
        verify(timeRecordProvider, never()).saveAll(any());
    }

    @Test
    @DisplayName("Deve falhar Check-out se o registro aberto não estiver com status PENDING")
    void shouldThrowExceptionWhenCheckoutStatusIsNotPending() throws Exception {
        var base64Image = Base64.getEncoder().encodeToString("dummyImage".getBytes());
        var request = new GeolocationRequest(-22.9, -43.2, base64Image);

        // Subtraímos apenas 30 minutos em vez de 8 horas para evitar o 'Midnight Bug'
        // e garantir que o teste seja executado no mesmo dia civil.
        var invalidOpenRecord = new TimeRecord(1L, LocalDateTime.now().minusMinutes(30), null, StatusRecord.UPDATED, false, true, employeeId, null, null, null, null, 99L, null, LocalDateTime.now().minusMinutes(30), null);

        doNothing().when(ntpTimeService).validateSystemTime(10);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(mockEmployee));
        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class))).thenReturn(employeeId);
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(mockCompany));

        when(timeRecordProvider.findOpenByEmployeeId(employeeId)).thenReturn(Optional.of(invalidOpenRecord));

        var exception = assertThrows(com.kts.kronos.application.exceptions.BadRequestException.class,
                () -> timeRecordService.registerTime(request));

        // CORREÇÃO: Valida utilizando a constante real da aplicação e o status que foi passado
        assertTrue(exception.getMessage().contains(ERR_CHECKOUT_STATUS));
        assertTrue(exception.getMessage().contains(StatusRecord.UPDATED.name()));
        verify(timeRecordProvider, never()).save(any());
    }

    @Test
    @DisplayName("Deve falhar Atualização Manual de Parceiro (PARTNER) se managerId for nulo")
    void shouldThrowExceptionWhenPartnerUpdatesWithoutManager() {
        Long recordId = 20L;
        // ManagerId enviado como null
        var request = new UpdateTimeRecordRequest(LocalDate.now(), LocalDate.now(), "09:00", "18:00", null);

        // CORREÇÃO: Usar minusMinutes(30) aqui também por precaução para manter consistência
        var existingRecord = new TimeRecord(recordId, LocalDateTime.now().minusMinutes(30), LocalDateTime.now(), StatusRecord.CREATED, false, true, employeeId, null, null, null, null, 1L, 2L, null, null);

        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("PARTNER");
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(mockEmployee));
        when(timeRecordProvider.findById(recordId)).thenReturn(Optional.of(existingRecord));
        when(timeRecordProvider.findByEmployeeId(employeeId)).thenReturn(Collections.emptyList());

        var exception = assertThrows(com.kts.kronos.application.exceptions.BadRequestException.class,
                () -> timeRecordService.updateTimeRecord(recordId, request));

        // CORREÇÃO: Verifica parte da string para não quebrar com mudanças de constantes literais
        assertTrue(exception.getMessage().contains("gestor é obrigatório"));
        verify(approvalProvider, never()).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar a alteração de um ponto com sucesso")
    void shouldRejectTimeRecordChangeSuccessfully() {
        Long recordId = 25L;
        var pendingRecord = new TimeRecord(recordId, LocalDateTime.now(), LocalDateTime.now().plusHours(8), StatusRecord.PENDING_APPROVAL, true, true, employeeId, null, null, null, null, 1L, 2L, null, null);
        var approvalData = new TimeRecordApprovalRequest(recordId, employeeId, managerId, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());

        when(timeRecordProvider.findById(recordId)).thenReturn(Optional.of(pendingRecord));
        when(approvalProvider.findByTimeRecordId(recordId)).thenReturn(Optional.of(approvalData));

        timeRecordService.rejectTimeRecordChange(recordId);

        verify(timeRecordProvider).save(argThat(record ->
                record.statusRecord() == StatusRecord.UPDATE_REJECTED && !record.edited()
        ));
        verify(approvalProvider).deleteByTimeRecordId(recordId);
    }


//    @Test
//    @DisplayName("Deve falhar Exclusão (Delete) de Ponto se o status estiver CLOSED")
//    void shouldThrowExceptionWhenDeletingClosedRecord() {
//        Long recordId = 30L;
//        var closedRecord = new TimeRecord(recordId, LocalDateTime.now(), LocalDateTime.now(), StatusRecord.CLOSED, false, true, employeeId, null, null, null, null, 1L, 2L, null, null);
//
//        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
//        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(mockEmployee));
//        when(timeRecordProvider.findById(recordId)).thenReturn(Optional.of(closedRecord));
//
//        var exception = assertThrows(com.kts.kronos.application.exceptions.BadRequestException.class,
//                () -> timeRecordService.deleteTimeRecord(employeeId, recordId));
//
//        // Valida parte da mensagem
//        assertTrue(exception.getMessage().contains("fechado") || exception.getMessage().contains(StatusRecord.CLOSED.name()));
//        verify(timeRecordProvider, never()).deleteTimeRecord(any());
//    }

    @Test
    @DisplayName("Deve ativar/desativar (Toggle) um registro com sucesso")
    void shouldToggleActivateSuccessfully() {
        Long recordId = 40L;
        var activeRecord = new TimeRecord(recordId, LocalDateTime.now(), LocalDateTime.now(), StatusRecord.CREATED, false, true, employeeId, null, null, null, null, 1L, 2L, null, null);

        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(mockEmployee));
        when(timeRecordProvider.findById(recordId)).thenReturn(Optional.of(activeRecord));

        timeRecordService.toggleActivate(employeeId, recordId);

        verify(timeRecordProvider).save(argThat(record -> !record.active())); // Deve salvar como false
    }

    @Test
    @DisplayName("Deve atualizar o status diretamente com sucesso")
    void shouldUpdateStatusSuccessfully() {
        Long recordId = 50L;
        var record = new TimeRecord(recordId, LocalDateTime.now(), LocalDateTime.now(), StatusRecord.CREATED, false, true, employeeId, null, null, null, null, 1L, 2L, null, null);
        var request = new com.kts.kronos.adapter.in.web.dto.timerecord.UpdateTimeRecordStatusRequest(StatusRecord.DAY_OFF);

        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(mockEmployee));
        when(timeRecordProvider.findById(recordId)).thenReturn(Optional.of(record));

        timeRecordService.updateStatus(employeeId, recordId, request);

        verify(timeRecordProvider).save(argThat(r -> r.statusRecord() == StatusRecord.DAY_OFF));
    }

    @Test
    @DisplayName("Gestor deve rejeitar solicitações de férias em lote")
    void shouldRejectVacationInBatchSuccessfully() {
        var request = new VacationApprovalRequest(List.of(100L));
        var record = new TimeRecord(100L, LocalDateTime.now(), null, StatusRecord.REQUEST_VACATION, false, true, employeeId, null, null, null, null, null, null, null, null);

        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("MANAGER");
        when(timeRecordProvider.findByIdIn(anySet())).thenReturn(List.of(record));

        timeRecordService.rejectVacation(request);

        verify(timeRecordProvider).saveAll(argThat(list ->
                list.size() == 1 && list.getFirst().statusRecord() == StatusRecord.VACATION_REJECTED
        ));
    }

    @Test
    @DisplayName("Gestor deve rejeitar solicitações de abono/esquecimento em lote")
    void shouldRejectTimeOffInBatchSuccessfully() {
        var request = new TimeOffApprovalRequest(List.of(600L));
        var record = new TimeRecord(600L, LocalDateTime.now(), LocalDateTime.now(), StatusRecord.TIME_OFF_REQUEST, true, true, employeeId, null, null, null, null, null, null, null, null);

        when(jwtAuthenticatedUser.getRoleFromToken()).thenReturn("MANAGER");
        when(timeRecordProvider.findByIdIn(anySet())).thenReturn(List.of(record));

        timeRecordService.rejectTimeOff(request);

        verify(timeRecordProvider).saveAll(argThat(list ->
                list.size() == 1 && list.getFirst().statusRecord() == StatusRecord.TIME_OFF_REJECTED
        ));
    }

    @Test
    @DisplayName("Deve gerar o Relatório Simplificado com cálculos corretos")
    void shouldGenerateSimpleReportSuccessfully() {
        var requestDate = LocalDate.now();
        var request = new com.kts.kronos.adapter.in.web.dto.timerecord.SimpleReportRequest("08:00", new LocalDate[]{requestDate});

        // Registro de 9 horas de trabalho (1 hora a mais que a referência)
        var record = new TimeRecord(1L, requestDate.atTime(8, 0), requestDate.atTime(17, 0), StatusRecord.CREATED, false, true, employeeId, null, null, null, null, null, null, null, null);

        when(jwtAuthenticatedUser.isWithEmployeeId(employeeId)).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(mockEmployee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(mockCompany));
        when(timeRecordProvider.findByEmployeeAndDatesAndStatuses(eq(employeeId), anySet(), anySet()))
                .thenReturn(List.of(record));

        var response = timeRecordService.simpleReport(employeeId, request);

        assertNotNull(response);
        assertEquals(mockEmployee.fullName(), response.employeeName());
        assertEquals(mockCompany.name(), response.companyName());
        assertEquals(1, response.days().size());

        // Verifica se calculou corretamente: 9 horas trabalhadas - 8 horas referência = +01:00 de saldo
        assertEquals("09:00", response.totalHoursWorked());
        assertEquals("+01:00", response.totalBalance());
    }

    @Test
    @DisplayName("Deve gerar o Relatório Detalhado com sucesso")
    void shouldGenerateListReportSuccessfully() {
        var requestDate = LocalDate.now();
        var request = new com.kts.kronos.adapter.in.web.dto.timerecord.ListReportRequest("08:00", true, null, new LocalDate[]{requestDate});

        var record = new TimeRecord(1L, requestDate.atTime(8, 0), requestDate.atTime(17, 0), StatusRecord.CREATED, false, true, employeeId, null, null, null, null, null, null, null, null);

        when(jwtAuthenticatedUser.isWithEmployeeId(employeeId)).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(mockEmployee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(mockCompany));
        when(timeRecordProvider.findByEmployeeAndDatesAndStatuses(eq(employeeId), anySet(), anySet()))
                .thenReturn(List.of(record));

        // Mock do documento em lote
        when(documentProvider.findByTimeRecordIdIn(anySet())).thenReturn(Collections.emptyList());

        var response = timeRecordService.listReport(employeeId, request);

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("+01:00", response.getFirst().balance()); // Valida o cálculo matemático mapeado
    }
}