package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.timerecord.ActionResponse;
import com.kts.kronos.application.port.in.usecase.TimeRecordUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}