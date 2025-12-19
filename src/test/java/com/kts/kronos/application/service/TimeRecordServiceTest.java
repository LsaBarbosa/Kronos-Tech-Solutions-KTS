package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.timerecord.ActionResponse;
import com.kts.kronos.adapter.in.web.dto.timerecord.GeolocationRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeRecordServiceTest {

    @Mock private TimeRecordProvider recordRepository;
    @Mock private EmployeeProvider employeeProvider;
    @Mock private CompanyProvider companyProvider;
    @Mock private JwtAuthenticatedUser jwtAuthenticatedUser;
    @Mock private DocumentService documentService;
    @Mock private AfdService afdService;
    @Mock private ReceiptPdfService receiptPdfService;
    @Mock private FaceRecognitionProvider faceRecognitionProvider;

    @InjectMocks private TimeRecordService timeRecordService;

    private UUID empId;
    private UUID compId;
    private Employee employee;
    private Company company;

    @BeforeEach
    void setup() {
        empId = UUID.randomUUID();
        compId = UUID.randomUUID();
        employee = mock(Employee.class);
        company = mock(Company.class);

        // --- Configuração dos Mocks ---
        when(employee.employeeId()).thenReturn(empId);
        when(employee.companyId()).thenReturn(compId);

        // Define Home Office como TRUE para pular a validação de GPS
        when(employee.homeOffice()).thenReturn(true);

        // REMOVIDO: when(company.companyId()).thenReturn(compId); -> Desnecessário pois o serviço apenas repassa o objeto

        // Mocks dos Providers
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(empId);
        when(employeeProvider.findById(empId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(compId)).thenReturn(Optional.of(company));

        // Mock do Reconhecimento Facial (Sucesso)
        when(faceRecognitionProvider.searchFaceByImage(any())).thenReturn(empId);
    }

    @Test
    @DisplayName("Check-in: Deve gerar NSR sequencial, logar no AFD e salvar comprovante")
    void shouldRegisterCheckinCorrectly() {
        when(recordRepository.findOpenByEmployeeId(empId)).thenReturn(Optional.empty());
        when(recordRepository.findMaxNsrByCompanyId(compId)).thenReturn(50L);

        when(recordRepository.save(any(TimeRecord.class))).thenAnswer(i -> {
            TimeRecord tr = i.getArgument(0);
            return new TimeRecord(100L, tr.startWork(), null, tr.statusRecord(), false, true, empId, null, null, null, null, tr.nsrCheckin(), null, tr.originalStartWork(), null);
        });

        when(receiptPdfService.generateReceipt(any(), any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        GeolocationRequest request = new GeolocationRequest(0.0, 0.0, "SGVsbG8=");
        ActionResponse response = timeRecordService.registerTime(request);

        ArgumentCaptor<TimeRecord> recordCaptor = ArgumentCaptor.forClass(TimeRecord.class);
        verify(recordRepository).save(recordCaptor.capture());
        TimeRecord savedRecord = recordCaptor.getValue();

        assertEquals(51L, savedRecord.nsrCheckin());

        verify(afdService).logMarking(eq(company), eq(employee), any(LocalDateTime.class), eq(51L));

        verify(documentService).uploadGeneratedDocument(
                eq(DocumentType.POINT_RECORD_RECEIPT),
                eq(empId),
                eq(100L),
                any(byte[].class),
                contains("comprovante_51_ENTRADA")
        );

        assertEquals("CHECKIN", response.actionType());
    }

    @Test
    @DisplayName("Check-out: Deve fechar ponto, gerar novo NSR e comprovante de saída")
    void shouldRegisterCheckoutCorrectly() {
        LocalDateTime now = LocalDateTime.now();
        TimeRecord openRecord = new TimeRecord(55L, now.minusHours(4), null, StatusRecord.PENDING, false, true, empId, 0.0, 0.0, null, null, 10L, null, now.minusHours(4), null);

        when(recordRepository.findOpenByEmployeeId(empId)).thenReturn(Optional.of(openRecord));
        when(recordRepository.findMaxNsrByCompanyId(compId)).thenReturn(10L);

        when(recordRepository.save(any(TimeRecord.class))).thenAnswer(i -> i.getArgument(0));

        GeolocationRequest request = new GeolocationRequest(0.0, 0.0, "SGVsbG8=");
        ActionResponse response = timeRecordService.registerTime(request);

        ArgumentCaptor<TimeRecord> recordCaptor = ArgumentCaptor.forClass(TimeRecord.class);
        verify(recordRepository).save(recordCaptor.capture());
        TimeRecord updatedRecord = recordCaptor.getValue();

        assertEquals(11L, updatedRecord.nsrCheckout());

        verify(afdService).logMarking(eq(company), eq(employee), any(LocalDateTime.class), eq(11L));

        verify(documentService).uploadGeneratedDocument(
                eq(DocumentType.POINT_RECORD_RECEIPT),
                eq(empId),
                eq(55L),
                any(),
                contains("comprovante_11_SAIDA")
        );

        assertEquals("CHECKOUT", response.actionType());
    }
}