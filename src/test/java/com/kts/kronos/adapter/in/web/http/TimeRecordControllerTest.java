package com.kts.kronos.adapter.in.web.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kts.kronos.adapter.in.web.dto.timerecord.*;
import com.kts.kronos.adapter.in.web.dto.timerecord.vacation.RequestVacationRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.vacation.VacationApprovalRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.vacation.VacationRequestResponse;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ForbiddenException;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TimeRecordController.class)
@AutoConfigureMockMvc(addFilters = false)
class TimeRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TimeRecordUseCase useCase;

    private static final String BASE_URL = "/records";
    private static final UUID EMPLOYEE_ID = UUID.randomUUID();
    private static final UUID MANAGER_ID = UUID.randomUUID();
    private static final Long RECORD_ID = 1L;

    @Test
    void registerTime_shouldReturn200_whenValid() throws Exception {
        when(useCase.registerTime(any())).thenReturn(new ActionResponse("ok", "CHECKIN"));

        mockMvc.perform(post(BASE_URL + "/checkin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GeolocationRequest(-22.9, -43.2, "base64"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actionType").value("CHECKIN"));
    }

    @Test
    void registerTime_shouldReturn400_whenInvalidBody() throws Exception {
        mockMvc.perform(post(BASE_URL + "/checkin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GeolocationRequest(-22.9, -43.2, ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].name").value("faceImageBase64"));
    }

    @Test
    void registerTime_shouldReturn404_whenUseCaseThrowsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Funcionário não encontrado"))
                .when(useCase).registerTime(any());

        mockMvc.perform(post(BASE_URL + "/checkin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GeolocationRequest(-22.9, -43.2, "base64"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Funcionário não encontrado"));
    }

    @Test
    void updateTimeRecord_shouldReturn204_whenValid() throws Exception {
        doNothing().when(useCase).updateTimeRecord(eq(RECORD_ID), any());

        mockMvc.perform(put(BASE_URL + "/update/time-record/{timeRecordId}", RECORD_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdateRequest())))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateTimeRecord_shouldReturn404_whenUseCaseThrowsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Registro não encontrado"))
                .when(useCase).updateTimeRecord(eq(RECORD_ID), any());

        mockMvc.perform(put(BASE_URL + "/update/time-record/{timeRecordId}", RECORD_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdateRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Registro não encontrado"));
    }

    @Test
    void updateStatus_shouldReturn204_whenValid() throws Exception {
        doNothing().when(useCase).updateStatus(eq(EMPLOYEE_ID), eq(RECORD_ID), any());

        mockMvc.perform(put(BASE_URL + "/update/status/{employeeId}/{timeRecordId}", EMPLOYEE_ID, RECORD_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTimeRecordStatusRequest(StatusRecord.CREATED))))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateStatus_shouldReturn400_whenValidationFails() throws Exception {
        mockMvc.perform(put(BASE_URL + "/update/status/{employeeId}/{timeRecordId}", EMPLOYEE_ID, RECORD_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStatus_shouldReturn404_whenUseCaseThrowsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Registro não encontrado"))
                .when(useCase).updateStatus(eq(EMPLOYEE_ID), eq(RECORD_ID), any());

        mockMvc.perform(put(BASE_URL + "/update/status/{employeeId}/{timeRecordId}", EMPLOYEE_ID, RECORD_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateTimeRecordStatusRequest(StatusRecord.CREATED))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Registro não encontrado"));
    }

    @Test
    void toggleActivate_shouldReturn204_whenValid() throws Exception {
        mockMvc.perform(put(BASE_URL + "/toggle-activate/{employeeId}/{timeRecordId}", EMPLOYEE_ID, RECORD_ID))
                .andExpect(status().isNoContent());

        verify(useCase).toggleActivate(EMPLOYEE_ID, RECORD_ID);
    }

    @Test
    void toggleActivate_shouldReturn403_whenUseCaseThrowsForbidden() throws Exception {
        doThrow(new ForbiddenException("Acesso negado")).when(useCase).toggleActivate(EMPLOYEE_ID, RECORD_ID);

        mockMvc.perform(put(BASE_URL + "/toggle-activate/{employeeId}/{timeRecordId}", EMPLOYEE_ID, RECORD_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Delete usa rota sem barra inicial por constante DELETE_RECORD")
    void deleteTimeRecord_shouldReturn204_whenValid() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/records/{employeeId}/{timeRecordId}", EMPLOYEE_ID, RECORD_ID))
                .andExpect(status().isNoContent());

        verify(useCase).deleteTimeRecord(EMPLOYEE_ID, RECORD_ID);
    }

    @Test
    void deleteTimeRecord_shouldReturn404_whenUseCaseThrowsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Registro não encontrado"))
                .when(useCase).deleteTimeRecord(EMPLOYEE_ID, RECORD_ID);

        mockMvc.perform(delete(BASE_URL + "/records/{employeeId}/{timeRecordId}", EMPLOYEE_ID, RECORD_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void report_shouldReturn200_whenValid() throws Exception {
        when(useCase.listReport(any(), any())).thenReturn(List.of());

        var request = new ListReportRequest("08:00", true, List.of(StatusRecord.CREATED), new LocalDate[]{LocalDate.now()});
        mockMvc.perform(post(BASE_URL + "/report")
                        .param("employeeId", EMPLOYEE_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void report_shouldReturn400_whenBodyMalformed() throws Exception {
        mockMvc.perform(post(BASE_URL + "/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void report_shouldReturn403_whenUseCaseThrowsForbidden() throws Exception {
        doThrow(new ForbiddenException("Sem permissão para relatório"))
                .when(useCase).listReport(any(), any());

        var request = new ListReportRequest("08:00", true, List.of(StatusRecord.CREATED), new LocalDate[]{LocalDate.now()});
        mockMvc.perform(post(BASE_URL + "/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void simpleReport_shouldReturn200_whenValid() throws Exception {
        when(useCase.simpleReport(any(), any())).thenReturn(new SimpleReportResponse("Ana", "KTS", List.of(), "10:00", "01:00", "+01:00"));

        var req = new SimpleReportRequest("08:00", new LocalDate[]{LocalDate.now()});
        mockMvc.perform(post(BASE_URL + "/report/simple")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHoursWorked").value("10:00"));
    }

    @Test
    void simpleReport_shouldReturn400_whenValidationFails() throws Exception {
        var req = new SimpleReportRequest("", new LocalDate[]{});
        mockMvc.perform(post(BASE_URL + "/report/simple")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void simpleReport_shouldReturn403_whenUseCaseThrowsForbidden() throws Exception {
        doThrow(new ForbiddenException("Sem permissão para relatório simplificado"))
                .when(useCase).simpleReport(any(), any());

        var req = new SimpleReportRequest("08:00", new LocalDate[]{LocalDate.now()});
        mockMvc.perform(post(BASE_URL + "/report/simple")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void approveChange_shouldReturn204_whenValid() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/approve/{timeRecordId}", RECORD_ID))
                .andExpect(status().isNoContent());

        verify(useCase).approveTimeRecordChange(RECORD_ID);
    }

    @Test
    void approveChange_shouldReturn404_whenUseCaseThrowsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Registro não encontrado")).when(useCase).approveTimeRecordChange(RECORD_ID);

        mockMvc.perform(patch(BASE_URL + "/approve/{timeRecordId}", RECORD_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectChange_shouldReturn204_whenValid() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/reject/{timeRecordId}", RECORD_ID))
                .andExpect(status().isNoContent());

        verify(useCase).rejectTimeRecordChange(RECORD_ID);
    }

    @Test
    void rejectChange_shouldReturn400_whenUseCaseThrowsBadRequest() throws Exception {
        doThrow(new BadRequestException("Não é possível rejeitar")).when(useCase).rejectTimeRecordChange(RECORD_ID);

        mockMvc.perform(patch(BASE_URL + "/reject/{timeRecordId}", RECORD_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Não é possível rejeitar"));
    }

    @Test
    void listPendingApprovals_shouldReturn200_whenValid() throws Exception {
        var response = new TimeRecordApprovalPageResponse(List.of(), 0, 0, 0, true, true);
        when(useCase.listPendingApprovals(0, 5, null)).thenReturn(response);

        mockMvc.perform(get(BASE_URL + "/pending-approvals").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void listPendingApprovals_shouldReturn400_whenInvalidPageType() throws Exception {
        mockMvc.perform(get(BASE_URL + "/pending-approvals").param("page", "x"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestVacation_shouldReturn201_whenValid() throws Exception {
        when(useCase.requestVacation(any())).thenReturn(List.of(10L, 11L));

        var request = new RequestVacationRequest(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), MANAGER_ID);
        mockMvc.perform(post(BASE_URL + "/vacation-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0]").value(10L));
    }

    @Test
    void requestVacation_shouldReturn400_whenInvalidBody() throws Exception {
        var invalid = new RequestVacationRequest(LocalDate.now().minusDays(5), LocalDate.now().plusDays(1), MANAGER_ID);

        mockMvc.perform(post(BASE_URL + "/vacation-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void approveVacation_shouldReturn204_whenValid() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/vacation-request/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VacationApprovalRequest(List.of(RECORD_ID)))))
                .andExpect(status().isNoContent());

        verify(useCase).approveVacation(any(VacationApprovalRequest.class));
    }

    @Test
    void approveVacation_shouldReturn400_whenInvalidBody() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/vacation-request/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VacationApprovalRequest(List.of()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void approveVacation_shouldReturn403_whenUseCaseThrowsForbidden() throws Exception {
        doThrow(new ForbiddenException("Apenas gestor pode aprovar"))
                .when(useCase).approveVacation(any(VacationApprovalRequest.class));

        mockMvc.perform(patch(BASE_URL + "/vacation-request/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VacationApprovalRequest(List.of(RECORD_ID)))))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectVacation_shouldReturn204_whenValid() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/vacation-request/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VacationApprovalRequest(List.of(RECORD_ID)))))
                .andExpect(status().isNoContent());

        verify(useCase).rejectVacation(any(VacationApprovalRequest.class));
    }

    @Test
    void rejectVacation_shouldReturn400_whenUseCaseThrowsBadRequest() throws Exception {
        doThrow(new BadRequestException("Pedido inválido")).when(useCase).rejectVacation(any(VacationApprovalRequest.class));

        mockMvc.perform(patch(BASE_URL + "/vacation-request/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VacationApprovalRequest(List.of(RECORD_ID)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Pedido inválido"));
    }

    @Test
    void listVacationRequests_shouldReturn200_whenValid() throws Exception {
        var response = new VacationRequestResponse(EMPLOYEE_ID, "Ana", LocalDate.now(), LocalDate.now().plusDays(1), "PENDING", List.of(RECORD_ID));
        when(useCase.listVacationRequests(anyString(), any(), anyInt(), anyInt())).thenReturn(List.of(response));

        mockMvc.perform(get(BASE_URL + "/vacation-request")
                        .param("status", "PENDING")
                        .param("employeeName", "Ana")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employeeName").value("Ana"));
    }

    @Test
    void listVacationRequests_shouldReturn400_whenPageInvalid() throws Exception {
        mockMvc.perform(get(BASE_URL + "/vacation-request").param("page", "not-int"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listVacationRequests_shouldReturn400_whenUseCaseThrowsBadRequest() throws Exception {
        doThrow(new BadRequestException("Status inválido"))
                .when(useCase).listVacationRequests(anyString(), any(), anyInt(), anyInt());

        mockMvc.perform(get(BASE_URL + "/vacation-request").param("status", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Status inválido"));
    }

    @Test
    void requestTimeOff_shouldReturn201_whenWithOrWithoutDocument() throws Exception {
        when(useCase.requestTimeOff(any(), any())).thenReturn(RECORD_ID);

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(validTimeOffRequest())
        );
        MockMultipartFile document = new MockMultipartFile(
                "document", "atestado.pdf", MediaType.APPLICATION_PDF_VALUE, "pdf".getBytes()
        );

        mockMvc.perform(multipart(BASE_URL + "/time-off/request").file(requestPart).file(document))
                .andExpect(status().isCreated())
                .andExpect(content().string(RECORD_ID.toString()));
    }

    @Test
    void requestTimeOff_shouldReturn400_whenValidationFails() throws Exception {
        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(new RequestTimeOffRequest(LocalDate.now(), LocalDate.now(), "9", "18:00", MANAGER_ID, null))
        );

        mockMvc.perform(multipart(BASE_URL + "/time-off/request").file(requestPart))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestTimeOff_shouldReturn404_whenUseCaseThrowsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Gestor não encontrado")).when(useCase).requestTimeOff(any(), any());

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(validTimeOffRequest())
        );

        mockMvc.perform(multipart(BASE_URL + "/time-off/request").file(requestPart))
                .andExpect(status().isNotFound());
    }

    @Test
    void approveTimeOff_shouldReturn204_whenValid() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/time-off/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TimeOffApprovalRequest(List.of(RECORD_ID)))))
                .andExpect(status().isNoContent());

        verify(useCase).approveTimeOff(any(TimeOffApprovalRequest.class));
    }

    @Test
    void approveTimeOff_shouldReturn400_whenInvalidBody() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/time-off/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TimeOffApprovalRequest(List.of()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void approveTimeOff_shouldReturn403_whenUseCaseThrowsForbidden() throws Exception {
        doThrow(new ForbiddenException("Acesso negado para aprovar"))
                .when(useCase).approveTimeOff(any(TimeOffApprovalRequest.class));

        mockMvc.perform(patch(BASE_URL + "/time-off/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TimeOffApprovalRequest(List.of(RECORD_ID)))))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectTimeOff_shouldReturn204_whenValid() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/time-off/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TimeOffApprovalRequest(List.of(RECORD_ID)))))
                .andExpect(status().isNoContent());

        verify(useCase).rejectTimeOff(any(TimeOffApprovalRequest.class));
    }

    @Test
    void rejectTimeOff_shouldReturn404_whenUseCaseThrowsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Registro não encontrado")).when(useCase).rejectTimeOff(any(TimeOffApprovalRequest.class));

        mockMvc.perform(patch(BASE_URL + "/time-off/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TimeOffApprovalRequest(List.of(RECORD_ID)))))
                .andExpect(status().isNotFound());
    }

    @Test
    void listTimeOffRequests_shouldReturn200_whenValid() throws Exception {
        when(useCase.listTimeOffRequests(anyString(), any(), anyInt(), anyInt()))
                .thenReturn(new TimeRecordPageResponse(List.of(), 1, 0, 0, true, true));

        mockMvc.perform(get(BASE_URL + "/time-off/requests")
                        .param("status", "PENDING")
                        .param("employeeName", "Ana")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void listTimeOffRequests_shouldReturn400_whenInvalidSizeType() throws Exception {
        mockMvc.perform(get(BASE_URL + "/time-off/requests").param("size", "x"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listTimeOffRequests_shouldReturn400_whenUseCaseThrowsBadRequest() throws Exception {
        doThrow(new BadRequestException("Filtro de status inválido"))
                .when(useCase).listTimeOffRequests(anyString(), any(), anyInt(), anyInt());

        mockMvc.perform(get(BASE_URL + "/time-off/requests").param("status", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Filtro de status inválido"));
    }

    private UpdateTimeRecordRequest validUpdateRequest() {
        return new UpdateTimeRecordRequest(LocalDate.now(), LocalDate.now(), "09:00", "18:00", MANAGER_ID);
    }

    private RequestTimeOffRequest validTimeOffRequest() {
        return new RequestTimeOffRequest(LocalDate.now(), LocalDate.now(), "09:00", "18:00", MANAGER_ID, null);
    }
}
