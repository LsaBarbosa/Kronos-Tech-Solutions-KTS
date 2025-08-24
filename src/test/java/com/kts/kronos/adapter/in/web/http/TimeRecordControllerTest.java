package com.kts.kronos.adapter.in.web.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kts.kronos.adapter.in.web.dto.timerecord.*;
import com.kts.kronos.application.port.in.usecase.TimeRecordUseCase;
import com.kts.kronos.domain.model.StatusRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.kts.kronos.constants.ApiPaths.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TimeRecordController.class)
class TimeRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TimeRecordUseCase useCase;

    // Cenários de teste para os endpoints
    @Test
    @WithMockUser(roles = {"MANAGER", "PARTNER"})
    void checkin_Success() throws Exception {
        mockMvc.perform(post(RECORDS + CHECKIN).with(csrf()))
                .andExpect(status().isCreated());
        verify(useCase, times(1)).checkin();
    }

    @Test
    @WithMockUser(roles = {"MANAGER", "PARTNER"})
    void checkout_Success() throws Exception {
        mockMvc.perform(post(RECORDS + CHECKOUT).with(csrf()))
                .andExpect(status().isCreated());
        verify(useCase, times(1)).checkout();
    }

    @Test
    @WithMockUser(roles = {"MANAGER", "PARTNER"})
    void updateTimeRecord_Success() throws Exception {
        Long timeRecordId = 1L;
        UpdateTimeRecordRequest req = new UpdateTimeRecordRequest(
                LocalDate.now(),
                LocalDate.now(),
                "09:00",
                "18:00",
                UUID.randomUUID()
        );

        mockMvc.perform(put(RECORDS + UPDATE_TIME_RECORD, timeRecordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isOk());
        verify(useCase, times(1)).updateTimeRecord(eq(timeRecordId), any(UpdateTimeRecordRequest.class));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void updateStatus_Success() throws Exception {
        UUID employeeId = UUID.randomUUID();
        Long timeRecordId = 1L;
        UpdateTimeRecordStatusRequest req = new UpdateTimeRecordStatusRequest(StatusRecord.DAY_OFF);

        mockMvc.perform(put(RECORDS + UPDATE_STATUS, employeeId, timeRecordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isOk());
        verify(useCase, times(1)).updateStatus(eq(employeeId), eq(timeRecordId), any(UpdateTimeRecordStatusRequest.class));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void toggleActivate_Success() throws Exception {
        UUID employeeId = UUID.randomUUID();
        Long timeRecordId = 1L;

        mockMvc.perform(put(RECORDS + TOGGLE_ACTIVATE_RECORD, employeeId, timeRecordId).with(csrf()))
                .andExpect(status().isOk());
        verify(useCase, times(1)).toggleActivate(employeeId, timeRecordId);
    }

    @Test
    @WithMockUser(roles = {"MANAGER", "PARTNER"})
    void report_Success() throws Exception {
        UUID employeeId = UUID.randomUUID();
        ListReportRequest req = new ListReportRequest("08:00", true, null, new LocalDate[]{LocalDate.now()});
        List<TimeRecordResponse> mockResponse = Collections.emptyList();

        when(useCase.listReport(any(UUID.class), any(ListReportRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(get(RECORDS + REPORT)
                        .param("employeeId", employeeId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(mockResponse)));
        verify(useCase, times(1)).listReport(eq(employeeId), any(ListReportRequest.class));
    }

    @Test
    @WithMockUser(roles = {"MANAGER", "PARTNER"})
    void simpleReport_Success() throws Exception {
        UUID employeeId = UUID.randomUUID();
        SimpleReportRequest req = new SimpleReportRequest("08:00", new LocalDate[]{LocalDate.now()});
        SimpleReportResponse mockResponse = new SimpleReportResponse("John Doe", "Kronos", Collections.emptyList(), "08:00", "+00:00");

        when(useCase.simpleReport(any(UUID.class), any(SimpleReportRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(get(RECORDS + SIMPLE_REPORT)
                        .param("employeeId", employeeId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(mockResponse)));
        verify(useCase, times(1)).simpleReport(eq(employeeId), any(SimpleReportRequest.class));
    }


    @Test
    @WithMockUser(roles = "MANAGER")
    void approveChange_Success() throws Exception {
        Long timeRecordId = 1L;

        mockMvc.perform(patch(RECORDS + APPROVE_UPDATE, timeRecordId).with(csrf()))
                .andExpect(status().isOk());
        verify(useCase, times(1)).approveTimeRecordChange(timeRecordId);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void rejectChange_Success() throws Exception {
        Long timeRecordId = 1L;

        mockMvc.perform(patch(RECORDS + REJECT_UPDATE, timeRecordId).with(csrf()))
                .andExpect(status().isOk());
        verify(useCase, times(1)).rejectTimeRecordChange(timeRecordId);
    }
}