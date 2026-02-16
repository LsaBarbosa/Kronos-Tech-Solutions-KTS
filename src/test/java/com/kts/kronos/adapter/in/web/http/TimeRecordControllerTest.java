package com.kts.kronos.adapter.in.web.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kts.kronos.adapter.in.web.dto.timerecord.*;
import com.kts.kronos.adapter.in.web.dto.timerecord.vacation.RequestVacationRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.vacation.VacationApprovalRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.vacation.VacationRequestResponse;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.TimeRecordUseCase;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TimeRecordController.class)
@AutoConfigureMockMvc(addFilters = false) // Desativa Spring Security para focar na lógica do Controller
class TimeRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TimeRecordUseCase timeRecordUseCase;

    private static final String BASE_URL = "/records";
    private static final UUID EMPLOYEE_ID = UUID.randomUUID();
    private static final Long RECORD_ID = 1L;

    // ==================================================================================
    // 1. REGISTRO DE PONTO (CHECK-IN/CHECK-OUT)
    // ==================================================================================

    @Test
    @DisplayName("Deve registrar ponto com sucesso (200 OK)")
    void shouldRegisterTimeSuccessfully() throws Exception {
        GeolocationRequest request = new GeolocationRequest(-22.9, -43.2, "base64image");
        ActionResponse response = new ActionResponse("Ponto registrado", "CHECKIN");

        when(timeRecordUseCase.registerTime(any(GeolocationRequest.class))).thenReturn(response);

        mockMvc.perform(post(BASE_URL + "/checkin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actionType").value("CHECKIN"));
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request se a imagem facial não for enviada")
    void shouldReturn400WhenFaceImageIsMissing() throws Exception {
        GeolocationRequest invalidRequest = new GeolocationRequest(-22.9, -43.2, ""); // Imagem vazia

        mockMvc.perform(post(BASE_URL + "/checkin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    // ==================================================================================
    // 2. ATUALIZAÇÃO DE REGISTRO (UPDATE)
    // ==================================================================================

    @Test
    @DisplayName("Deve atualizar registro de ponto com sucesso (200 OK)")
    void shouldUpdateTimeRecordSuccessfully() throws Exception {
        UpdateTimeRecordRequest request = new UpdateTimeRecordRequest(
                LocalDate.now(), LocalDate.now(), "09:00", "18:00", UUID.randomUUID()
        );

        doNothing().when(timeRecordUseCase).updateTimeRecord(eq(RECORD_ID), any());

        mockMvc.perform(put(BASE_URL + "/update/time-record/{timeRecordId}", RECORD_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found se o registro não existir ao atualizar")
    void shouldReturn404WhenUpdatingNonExistentRecord() throws Exception {
        UpdateTimeRecordRequest request = new UpdateTimeRecordRequest(
                LocalDate.now(), LocalDate.now(), "09:00", "18:00", UUID.randomUUID()
        );

        doThrow(new ResourceNotFoundException(RECORD_NOT_FOUND))
                .when(timeRecordUseCase).updateTimeRecord(eq(RECORD_ID), any());

        mockMvc.perform(put(BASE_URL + "/update/time-record/{timeRecordId}", RECORD_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(RECORD_NOT_FOUND));
    }

    @Test
    @DisplayName("Deve rejeitar solicitação de ajuste com sucesso (200 OK)")
    void shouldRejectUpdateSuccessfully() throws Exception {
        doNothing().when(timeRecordUseCase).rejectTimeRecordChange(RECORD_ID);

        mockMvc.perform(patch(BASE_URL + "/reject/{timeRecordId}", RECORD_ID))
                .andExpect(status().isOk());

        verify(timeRecordUseCase).rejectTimeRecordChange(RECORD_ID);
    }

    // ==================================================================================
    // 3. RELATÓRIOS (REPORT & SIMPLE REPORT)
    // ==================================================================================

    @Test
    @DisplayName("Deve gerar relatório detalhado com sucesso (200 OK)")
    void shouldGenerateReportSuccessfully() throws Exception {
        ListReportRequest request = new ListReportRequest(
                "08:00", true, null, new LocalDate[]{LocalDate.now()}
        );

        when(timeRecordUseCase.listReport(any(), any())).thenReturn(Collections.emptyList());

        mockMvc.perform(post(BASE_URL + "/report")
                        .param("employeeId", EMPLOYEE_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Deve gerar relatório simplificado com sucesso (200 OK)")
    void shouldGenerateSimpleReportSuccessfully() throws Exception {
        SimpleReportRequest request = new SimpleReportRequest(
                "08:00", new LocalDate[]{LocalDate.now()}
        );
        SimpleReportResponse response = new SimpleReportResponse(
                "João", "KTS", List.of(), "08:00", "01:00", "+00:00"
        );

        when(timeRecordUseCase.simpleReport(eq(EMPLOYEE_ID), any())).thenReturn(response);

        mockMvc.perform(post(BASE_URL + "/report/simple")
                        .param("employeeId", EMPLOYEE_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeName").value("João"));
    }

    // ==================================================================================
    // 4. APROVAÇÕES (MANAGER)
    // ==================================================================================

    @Test
    @DisplayName("Deve aprovar solicitação de ajuste com sucesso (200 OK)")
    void shouldApproveUpdateSuccessfully() throws Exception {
        doNothing().when(timeRecordUseCase).approveTimeRecordChange(RECORD_ID);

        mockMvc.perform(patch(BASE_URL + "/approve/{timeRecordId}", RECORD_ID))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve listar aprovações pendentes com paginação (200 OK)")
    void shouldListPendingApprovalsSuccessfully() throws Exception {
        TimeRecordApprovalPageResponse response = new TimeRecordApprovalPageResponse(
                List.of(), 1, 0, 0, true, true
        );

        when(timeRecordUseCase.listPendingApprovals(0, 5, null)).thenReturn(response);

        mockMvc.perform(get(BASE_URL + "/pending-approvals")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ==================================================================================
    // 5. FÉRIAS (VACATION)
    // ==================================================================================

    @Test
    @DisplayName("Deve solicitar férias com sucesso (201 Created)")
    void shouldRequestVacationSuccessfully() throws Exception {
        RequestVacationRequest request = new RequestVacationRequest(
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(20), UUID.randomUUID()
        );

        when(timeRecordUseCase.requestVacation(any())).thenReturn(List.of(100L, 101L));

        mockMvc.perform(post(BASE_URL + "/vacation-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0]").value(100L));
    }

    @Test
    @DisplayName("Deve listar solicitações de férias (200 OK)")
    void shouldListVacationRequestsSuccessfully() throws Exception {
        // Mock da resposta
        VacationRequestResponse responseDto = new VacationRequestResponse(
                EMPLOYEE_ID, "João", LocalDate.now(), LocalDate.now().plusDays(5), "PENDING", List.of(1L)
        );

        when(timeRecordUseCase.listVacationRequests(anyString(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(responseDto));

        mockMvc.perform(get(BASE_URL + "/vacation-request")
                        .param("status", "PENDING")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employeeName").value("João"));
    }

    @Test
    @DisplayName("Deve aprovar férias com sucesso (204 No Content)")
    void shouldApproveVacationSuccessfully() throws Exception {
        VacationApprovalRequest request = new VacationApprovalRequest(List.of(100L, 101L));

        doNothing().when(timeRecordUseCase).approveVacation(any());

        mockMvc.perform(patch(BASE_URL + "/vacation-request/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Deve rejeitar férias com sucesso (204 No Content)")
    void shouldRejectVacationSuccessfully() throws Exception {
        VacationApprovalRequest request = new VacationApprovalRequest(List.of(100L, 101L));

        doNothing().when(timeRecordUseCase).rejectVacation(any());

        mockMvc.perform(patch(BASE_URL + "/vacation-request/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request se tentar rejeitar férias com lista vazia")
    void shouldReturn400WhenRejectVacationWithEmptyList() throws Exception {
        // Lista vazia viola a validação @NotEmpty no DTO VacationApprovalRequest
        VacationApprovalRequest request = new VacationApprovalRequest(List.of());

        mockMvc.perform(patch(BASE_URL + "/vacation-request/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================================================================================
    // 6. ABONOS E ATESTADOS (TIME OFF)
    // ==================================================================================

    @Test
    @DisplayName("Deve solicitar abono com upload de documento (201 Created)")
    void shouldRequestTimeOffWithDocumentSuccessfully() throws Exception {
        // Criar as partes do Multipart Request
        MockMultipartFile document = new MockMultipartFile(
                "document", "atestado.pdf", MediaType.APPLICATION_PDF_VALUE, "conteudo".getBytes()
        );

        // O objeto JSON deve ser enviado como uma "Part" chamada "request"
        RequestTimeOffRequest requestDto = new RequestTimeOffRequest(
                LocalDate.now(), LocalDate.now(), "09:00", "18:00", UUID.randomUUID(), null
        );
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(requestDto)
        );

        when(timeRecordUseCase.requestTimeOff(any(), any())).thenReturn(RECORD_ID);

        mockMvc.perform(multipart(BASE_URL + "/time-off/request")
                        .file(document)
                        .file(requestPart))
                .andExpect(status().isCreated())
                .andExpect(content().string(RECORD_ID.toString()));
    }

    @Test
    @DisplayName("Deve listar solicitações de abono (200 OK)")
    void shouldListTimeOffRequestsSuccessfully() throws Exception {
        // Mock da resposta paginada
        TimeRecordPageResponse pageResponse = new TimeRecordPageResponse(
                List.of(), 1, 0, 0, true, true
        );

        when(timeRecordUseCase.listTimeOffRequests(anyString(), any(), anyInt(), anyInt()))
                .thenReturn(pageResponse);

        mockMvc.perform(get(BASE_URL + "/time-off/requests")
                        .param("status", "PENDING")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("Deve aprovar abono/time-off (204 No Content)")
    void shouldApproveTimeOffSuccessfully() throws Exception {
        doNothing().when(timeRecordUseCase).approveTimeOff(RECORD_ID);

        mockMvc.perform(patch(BASE_URL + "/time-off/approve/{timeRecordId}", RECORD_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Deve rejeitar abono/time-off (204 No Content)")
    void shouldRejectTimeOffSuccessfully() throws Exception {
        doNothing().when(timeRecordUseCase).rejectTimeOff(RECORD_ID);

        mockMvc.perform(patch(BASE_URL + "/time-off/reject/{timeRecordId}", RECORD_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found ao tentar rejeitar abono inexistente")
    void shouldReturn404WhenRejectingNonExistentTimeOff() throws Exception {
        doThrow(new ResourceNotFoundException(RECORD_NOT_FOUND))
                .when(timeRecordUseCase).rejectTimeOff(RECORD_ID);

        mockMvc.perform(patch(BASE_URL + "/time-off/reject/{timeRecordId}", RECORD_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request se houver erro de validação no abono")
    void shouldReturn400WhenTimeOffRequestInvalid() throws Exception {
        // Datas invertidas (Inicio > Fim) - Validação de Negócio
        RequestTimeOffRequest requestDto = new RequestTimeOffRequest(
                LocalDate.now().plusDays(5), LocalDate.now(), "09:00", "18:00", UUID.randomUUID(), null
        );
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(requestDto)
        );

        doThrow(new BadRequestException(START_DATE_BIGGER_THAN_END_DATE))
                .when(timeRecordUseCase).requestTimeOff(any(), any());

        mockMvc.perform(multipart(BASE_URL + "/time-off/request")
                        .file(requestPart)) // Sem arquivo (opcional ou erro, dependendo do caso)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(START_DATE_BIGGER_THAN_END_DATE));
    }

    // ==================================================================================
    // 7. CENÁRIOS DE EXCLUSÃO E STATUS
    // ==================================================================================

    @Test
    @DisplayName("Deve deletar registro de ponto com sucesso (200 OK)")
    void shouldDeleteTimeRecordSuccessfully() throws Exception {
        doNothing().when(timeRecordUseCase).deleteTimeRecord(EMPLOYEE_ID, RECORD_ID);

        mockMvc.perform(delete(BASE_URL + "/records/{employeeId}/{timeRecordId}", EMPLOYEE_ID, RECORD_ID))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve atualizar status do ponto manualmente (200 OK)")
    void shouldUpdateStatusSuccessfully() throws Exception {
        UpdateTimeRecordStatusRequest request = new UpdateTimeRecordStatusRequest(StatusRecord.CREATED);

        doNothing().when(timeRecordUseCase).updateStatus(eq(EMPLOYEE_ID), eq(RECORD_ID), any());

        mockMvc.perform(put(BASE_URL + "/update/status/{employeeId}/{timeRecordId}", EMPLOYEE_ID, RECORD_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve alternar ativação do registro (Toggle Activate) (200 OK)")
    void shouldToggleActivateRecordSuccessfully() throws Exception {
        doNothing().when(timeRecordUseCase).toggleActivate(EMPLOYEE_ID, RECORD_ID);

        mockMvc.perform(put(BASE_URL + "/toggle-activate/{employeeId}/{timeRecordId}", EMPLOYEE_ID, RECORD_ID))
                .andExpect(status().isOk());

        verify(timeRecordUseCase).toggleActivate(EMPLOYEE_ID, RECORD_ID);
    }
}