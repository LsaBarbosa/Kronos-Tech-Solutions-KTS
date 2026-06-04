package com.kts.kronos.adapter.in.web.http.webmvc;

import com.kts.kronos.adapter.in.web.dto.timerecord.ActionResponse;
import com.kts.kronos.adapter.in.web.dto.timerecord.EmployeeData;
import com.kts.kronos.adapter.in.web.dto.timerecord.MyRequestItemResponse;
import com.kts.kronos.adapter.in.web.dto.timerecord.MyRequestsResponse;
import com.kts.kronos.adapter.in.web.dto.timerecord.RecentTimeRecordItemResponse;
import com.kts.kronos.adapter.in.web.dto.timerecord.RecentTimeRecordsResponse;
import com.kts.kronos.adapter.in.web.dto.timerecord.TimeRecordApprovalPageResponse;
import com.kts.kronos.adapter.in.web.dto.timerecord.TimeRecordApprovalResponse;
import com.kts.kronos.adapter.in.web.dto.timerecord.TimeRecordPageResponse;
import com.kts.kronos.adapter.in.web.dto.timerecord.TimeRecordResponse;
import com.kts.kronos.adapter.in.web.dto.timerecord.TodayTimeRecordStatusResponse;
import com.kts.kronos.adapter.in.web.dto.timerecord.vacation.VacationRequestResponse;
import com.kts.kronos.adapter.in.web.http.TimeRecordController;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TimeRecordController.class)
@AutoConfigureMockMvc(addFilters = false)
class TimeRecordControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TimeRecordUseCase useCase;

    @Test
    @DisplayName("registerTime: deve delegar check-in e retornar payload")
    void shouldRegisterTime() throws Exception {
        when(useCase.registerTime(any())).thenReturn(new ActionResponse("Checkin realizado com sucesso", "CHECKIN"));

        mockMvc.perform(post("/records/checkin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "latitude": -22.90,
                                  "longitude": -43.20,
                                  "faceImageBase64": "base64-face"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Checkin realizado com sucesso"))
                .andExpect(jsonPath("$.actionType").value("CHECKIN"));

        verify(useCase).registerTime(any());
    }

    @Test
    @DisplayName("registerTime: deve retornar 400 quando faceImageBase64 vier em branco")
    void shouldReturnBadRequestWhenFaceImageIsBlank() throws Exception {
        mockMvc.perform(post("/records/checkin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "latitude": -22.90,
                                  "longitude": -43.20,
                                  "faceImageBase64": ""
                                }
                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("registerTime: deve traduzir exceção de negócio")
    void shouldTranslateExceptionWhenRegisteringTime() throws Exception {
        when(useCase.registerTime(any()))
                .thenThrow(new BadRequestException("Registro fora da área permitida"));

        mockMvc.perform(post("/records/checkin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "latitude": -22.90,
                                  "longitude": -43.20,
                                  "faceImageBase64": "base64-face"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Registro fora da área permitida"));
    }

    @Test
    @DisplayName("updateTimeRecord: deve delegar atualização")
    void shouldUpdateTimeRecord() throws Exception {
        mockMvc.perform(put("/records/update/time-record/{timeRecordId}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateTimeRecordJson()))
                .andExpect(status().isNoContent());

        verify(useCase).updateTimeRecord(eq(10L), any());
    }

    @Test
    @DisplayName("updateTimeRecord: deve retornar 400 para payload inválido")
    void shouldReturnBadRequestWhenUpdateTimeRecordIsInvalid() throws Exception {
        mockMvc.perform(put("/records/update/time-record/{timeRecordId}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startDate": null,
                                  "endDate": "2026-04-20",
                                  "startHour": "8h",
                                  "endHour": "",
                                  "managerId": null
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("updateTimeRecord: deve traduzir registro inexistente")
    void shouldTranslateExceptionWhenUpdatingTimeRecord() throws Exception {
        doThrow(new ResourceNotFoundException("Registro não encontrado"))
                .when(useCase).updateTimeRecord(eq(10L), any());

        mockMvc.perform(put("/records/update/time-record/{timeRecordId}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateTimeRecordJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Registro não encontrado"));
    }

    @Test
    @DisplayName("updateStatus: deve delegar atualização de status")
    void shouldUpdateStatus() throws Exception {
        UUID employeeId = UUID.randomUUID();

        mockMvc.perform(put("/records/update/status/{employeeId}/{timeRecordId}", employeeId, 11L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "statusRecord": "UPDATED"
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(useCase).updateStatus(eq(employeeId), eq(11L), any());
    }

    @Test
    @DisplayName("updateStatus: deve retornar 400 para status ausente")
    void shouldReturnBadRequestWhenStatusIsMissing() throws Exception {
        mockMvc.perform(put("/records/update/status/{employeeId}/{timeRecordId}", UUID.randomUUID(), 11L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("toggleActivate: deve delegar ativação/desativação")
    void shouldToggleActivate() throws Exception {
        UUID employeeId = UUID.randomUUID();

        mockMvc.perform(put("/records/toggle-activate/{employeeId}/{timeRecordId}", employeeId, 12L))
                .andExpect(status().isNoContent());

        verify(useCase).toggleActivate(employeeId, 12L);
    }

    @Test
    @DisplayName("deleteTimeRecord: deve delegar exclusão")
    void shouldDeleteTimeRecord() throws Exception {
        UUID employeeId = UUID.randomUUID();

        mockMvc.perform(delete("/records/{employeeId}/{timeRecordId}", employeeId, 13L))
                .andExpect(status().isNoContent());

        verify(useCase).deleteTimeRecord(employeeId, 13L);
    }

    @Test
    @DisplayName("deleteTimeRecord: deve traduzir registro inexistente")
    void shouldTranslateExceptionWhenDeletingTimeRecord() throws Exception {
        UUID employeeId = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Registro não encontrado"))
                .when(useCase).deleteTimeRecord(employeeId, 13L);

        mockMvc.perform(delete("/records/{employeeId}/{timeRecordId}", employeeId, 13L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Registro não encontrado"));
    }

    @Test
    @DisplayName("report: deve listar registros do período")
    void shouldListReport() throws Exception {
        UUID employeeId = UUID.randomUUID();
        when(useCase.listReport(eq(employeeId), any()))
                .thenReturn(List.of(timeRecordResponse(employeeId, 1L)));

        mockMvc.perform(post("/records/report")
                        .param("employeeId", employeeId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reference": "08:00",
                                  "active": true,
                                  "statuses": ["CREATED"],
                                  "dates": ["01-04-2026", "30-04-2026"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].timeRecordId").value(1L))
                .andExpect(jsonPath("$[0].employeeId").value(employeeId.toString()));

        verify(useCase).listReport(eq(employeeId), any());
    }

    @Test
    @DisplayName("report: deve traduzir erro de regra")
    void shouldTranslateExceptionWhenListingReport() throws Exception {
        when(useCase.listReport(eq(null), any()))
                .thenThrow(new BadRequestException("Período inválido"));

        mockMvc.perform(post("/records/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reference": "08:00",
                                  "dates": ["30-04-2026", "01-04-2026"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Período inválido"));
    }

    @Test
    @DisplayName("report: não deve expor coordenadas precisas no response (SPEC-001)")
    void shouldNotExposeGeolocationInReport() throws Exception {
        UUID employeeId = UUID.randomUUID();
        when(useCase.listReport(eq(employeeId), any()))
                .thenReturn(List.of(timeRecordResponse(employeeId, 1L)));

        mockMvc.perform(post("/records/report")
                        .param("employeeId", employeeId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reference": "08:00",
                                  "active": true,
                                  "statuses": ["CREATED"],
                                  "dates": ["01-04-2026", "30-04-2026"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].timeRecordId").value(1L))
                .andExpect(jsonPath("$[0].employeeId").value(employeeId.toString()))
                // SPEC-001: Não deve expor coordenadas precisas
                .andExpect(jsonPath("$[0].latitude").doesNotExist())
                .andExpect(jsonPath("$[0].longitude").doesNotExist())
                .andExpect(jsonPath("$[0].endLatitude").doesNotExist())
                .andExpect(jsonPath("$[0].endLongitude").doesNotExist());
    }

    @Test
    @DisplayName("approveChange/rejectChange: devem delegar decisões de ajuste")
    void shouldApproveAndRejectTimeRecordChange() throws Exception {
        mockMvc.perform(patch("/records/approve/{timeRecordId}", 14L))
                .andExpect(status().isNoContent());

        mockMvc.perform(patch("/records/reject/{timeRecordId}", 15L))
                .andExpect(status().isNoContent());

        verify(useCase).approveTimeRecordChange(14L);
        verify(useCase).rejectTimeRecordChange(15L);
    }

    @Test
    @DisplayName("listPendingApprovals: deve listar aprovações pendentes com paginação")
    void shouldListPendingApprovals() throws Exception {
        when(useCase.listPendingApprovals(2, 5, "ana"))
                .thenReturn(new TimeRecordApprovalPageResponse(
                        List.of(new TimeRecordApprovalResponse(
                                16L,
                                "Ana Paula",
                                "manager",
                                LocalDateTime.of(2026, 4, 20, 8, 0),
                                LocalDateTime.of(2026, 4, 20, 17, 0),
                                LocalDateTime.of(2026, 4, 20, 9, 0),
                                LocalDateTime.of(2026, 4, 20, 18, 0),
                                "/documents/1"
                        )),
                        3,
                        11,
                        2,
                        false,
                        true
                ));

        mockMvc.perform(get("/records/pending-approvals")
                        .param("page", "2")
                        .param("employeeName", "ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvals[0].timeRecordId").value(16L))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    @DisplayName("requestVacation: deve criar solicitações de férias com datas ISO")
    void shouldRequestVacation() throws Exception {
        UUID managerId = UUID.randomUUID();
        when(useCase.requestVacation(any())).thenReturn(List.of(20L, 21L));

        mockMvc.perform(post("/records/vacation-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startDate": "2026-12-01",
                                  "endDate": "2026-12-05",
                                  "managerId": "%s"
                                }
                                """.formatted(managerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0]").value(20L))
                .andExpect(jsonPath("$[1]").value(21L));
    }

    @Test
    @DisplayName("requestVacation: deve retornar 400 para período inválido")
    void shouldReturnBadRequestWhenVacationRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/records/vacation-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startDate": "2020-01-01",
                                  "endDate": null,
                                  "managerId": null
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("approveVacation/rejectVacation: devem retornar 204")
    void shouldApproveAndRejectVacation() throws Exception {
        mockMvc.perform(patch("/records/vacation-request/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "timeRecordIds": [20, 21]
                                }
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(patch("/records/vacation-request/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "timeRecordIds": [22]
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(useCase).approveVacation(any());
        verify(useCase).rejectVacation(any());
    }

    @Test
    @DisplayName("approveVacation: deve retornar 400 para lista vazia")
    void shouldReturnBadRequestWhenVacationApprovalIsEmpty() throws Exception {
        mockMvc.perform(patch("/records/vacation-request/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "timeRecordIds": []
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("listVacationRequests: deve listar solicitações de férias")
    void shouldListVacationRequests() throws Exception {
        UUID employeeId = UUID.randomUUID();
        when(useCase.listVacationRequests("PENDING", "ana", 1, 20))
                .thenReturn(List.of(new VacationRequestResponse(
                        employeeId,
                        "Ana Paula",
                        LocalDate.of(2026, 12, 1),
                        LocalDate.of(2026, 12, 5),
                        "REQUEST_VACATION",
                        List.of(20L, 21L)
                )));

        mockMvc.perform(get("/records/vacation-request")
                        .param("status", "PENDING")
                        .param("employeeName", "ana")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employeeId").value(employeeId.toString()))
                .andExpect(jsonPath("$[0].timeRecordIdsForApproval[0]").value(20L));
    }

    @Test
    @DisplayName("requestTimeOff: deve criar solicitação com documento opcional")
    void shouldRequestTimeOff() throws Exception {
        UUID managerId = UUID.randomUUID();
        when(useCase.requestTimeOff(any(), any())).thenReturn(30L);
        MockMultipartFile request = jsonPart("request", """
                {
                  "startDate": "2026-04-20",
                  "endDate": "2026-04-20",
                  "startHour": "09:00",
                  "endHour": "12:00",
                  "managerId": "%s",
                  "type": "TIME_OFF_REQUEST"
                }
                """.formatted(managerId));
        MockMultipartFile document = new MockMultipartFile(
                "document",
                "atestado.pdf",
                "application/pdf",
                "pdf".getBytes()
        );

        mockMvc.perform(multipart("/records/time-off/request")
                        .file(request)
                        .file(document))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.timeRecordId").value(30L));
    }

    @Test
    @DisplayName("requestTimeOff: deve retornar 400 para parte request inválida")
    void shouldReturnBadRequestWhenTimeOffRequestIsInvalid() throws Exception {
        MockMultipartFile request = jsonPart("request", """
                {
                  "startDate": null,
                  "endDate": "2026-04-20",
                  "startHour": "",
                  "endHour": "12h",
                  "managerId": null
                }
                """);

        mockMvc.perform(multipart("/records/time-off/request")
                        .file(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("approveTimeOff/rejectTimeOff: devem retornar 204")
    void shouldApproveAndRejectTimeOff() throws Exception {
        mockMvc.perform(patch("/records/time-off/approve/{timeRecordId}", 31L))
                .andExpect(status().isNoContent());

        mockMvc.perform(patch("/records/time-off/reject/{timeRecordId}", 32L))
                .andExpect(status().isNoContent());

        verify(useCase).approveTimeOff(31L);
        verify(useCase).rejectTimeOff(32L);
    }

    @Test
    @DisplayName("approveTimeOff: deve traduzir solicitação inexistente")
    void shouldTranslateExceptionWhenApprovingTimeOff() throws Exception {
        doThrow(new ResourceNotFoundException("Solicitação não encontrada"))
                .when(useCase).approveTimeOff(31L);

        mockMvc.perform(patch("/records/time-off/approve/{timeRecordId}", 31L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Solicitação não encontrada"));
    }

    @Test
    @DisplayName("listTimeOffRequests: deve listar solicitações paginadas")
    void shouldListTimeOffRequests() throws Exception {
        UUID employeeId = UUID.randomUUID();
        when(useCase.listTimeOffRequests("PENDING", "ana", 1, 5))
                .thenReturn(new TimeRecordPageResponse(
                        List.of(timeRecordResponse(employeeId, 33L)),
                        2,
                        6,
                        1,
                        false,
                        true
                ));

        mockMvc.perform(get("/records/time-off/requests")
                        .param("status", "PENDING")
                        .param("employeeName", "ana")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].timeRecordId").value(33L))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    @DisplayName("getTodayStatus: deve retornar status persistido do dia")
    void shouldReturnTodayStatus() throws Exception {
        when(useCase.getTodayStatus()).thenReturn(new TodayTimeRecordStatusResponse(
                LocalDate.of(2026, 6, 3),
                "READY_TO_CHECKIN",
                "CHECK_IN",
                null,
                null,
                List.of(),
                "PERSISTED",
                "America/Sao_Paulo"
        ));

        mockMvc.perform(get("/records/me/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY_TO_CHECKIN"))
                .andExpect(jsonPath("$.nextAction").value("CHECK_IN"))
                .andExpect(jsonPath("$.source").value("PERSISTED"));
    }

    @Test
    @DisplayName("listMyRecentRecords: deve retornar lista dos últimos registros")
    void shouldReturnMyRecentRecords() throws Exception {
        when(useCase.listMyRecentRecords(5)).thenReturn(new RecentTimeRecordsResponse(
                List.of(new RecentTimeRecordItemResponse(
                        1L,
                        "CHECK_IN",
                        LocalDateTime.of(2026, 6, 3, 8, 0).atOffset(java.time.ZoneOffset.of("-03:00")),
                        "REGISTERED",
                        "BIOMETRIC",
                        "Empresa principal",
                        true,
                        "/documents/1"
                )),
                "PERSISTED"
        ));

        mockMvc.perform(get("/records/me/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].actionType").value("CHECK_IN"))
                .andExpect(jsonPath("$.items[0].receiptGenerated").value(true));
    }

    @Test
    @DisplayName("listMyRequests: deve retornar solicitações do colaborador autenticado")
    void shouldReturnMyRequests() throws Exception {
        when(useCase.listMyRequests(5)).thenReturn(new MyRequestsResponse(
                List.of(new MyRequestItemResponse(
                        "vacation-1",
                        "VACATION",
                        "Solicitação de férias",
                        LocalDateTime.of(2026, 6, 1, 10, 0).atOffset(java.time.ZoneOffset.of("-03:00")),
                        "PENDING",
                        "2026-07-01 a 2026-07-15"
                )),
                "PERSISTED"
        ));

        mockMvc.perform(get("/records/me/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].type").value("VACATION"))
                .andExpect(jsonPath("$.items[0].status").value("PENDING"));
    }

    private static String validUpdateTimeRecordJson() {
        return """
                {
                  "startDate": "2026-04-20",
                  "endDate": "2026-04-20",
                  "startHour": "08:00",
                  "endHour": "17:00",
                  "managerId": "%s"
                }
                """.formatted(UUID.randomUUID());
    }

    private static TimeRecordResponse timeRecordResponse(UUID employeeId, long timeRecordId) {
        return new TimeRecordResponse(
                timeRecordId,
                LocalDateTime.of(2026, 4, 20, 8, 0),
                "08:00",
                LocalDateTime.of(2026, 4, 20, 17, 0),
                "17:00",
                "09:00",
                "+01:00",
                StatusRecord.CREATED,
                false,
                true,
                employeeId,
                new EmployeeData("Ana Paula", "Kronos Tech"),
                null
        );
    }

    private static MockMultipartFile jsonPart(String name, String json) {
        return new MockMultipartFile(name, name + ".json", MediaType.APPLICATION_JSON_VALUE, json.getBytes());
    }
}
