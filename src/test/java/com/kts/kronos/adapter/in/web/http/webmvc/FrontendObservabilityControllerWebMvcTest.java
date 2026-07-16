package com.kts.kronos.adapter.in.web.http.webmvc;

import com.kts.kronos.adapter.in.web.http.FrontendObservabilityController;
import com.kts.kronos.application.service.FrontendObservabilityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FrontendObservabilityController.class)
@AutoConfigureMockMvc(addFilters = false)
class FrontendObservabilityControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FrontendObservabilityService frontendObservabilityService;

    @Test
    void shouldAcceptObservabilityEvent() throws Exception {
        mockMvc.perform(post("/observability/frontend/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventType\":\"PAGE_VIEW\",\"level\":\"INFO\"}"))
                .andExpect(status().isAccepted());

        verify(frontendObservabilityService).accept(any());
    }
}
