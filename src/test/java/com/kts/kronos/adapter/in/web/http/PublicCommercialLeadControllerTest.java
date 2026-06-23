package com.kts.kronos.adapter.in.web.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kts.kronos.adapter.in.web.dto.public_commercial.CommercialLeadRequest;
import com.kts.kronos.application.security.ClientIpResolver;
import com.kts.kronos.application.service.CommercialLeadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicCommercialLeadController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicCommercialLeadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommercialLeadService commercialLeadService;

    @MockitoBean
    private ClientIpResolver clientIpResolver;

    @Test
    void shouldReturn204ForValidRequest() throws Exception {
        when(clientIpResolver.resolve(any())).thenReturn("127.0.0.1");
        doNothing().when(commercialLeadService).submitLead(any(), anyString());

        var request = new CommercialLeadRequest("Ana Silva", "Empresa Teste", "ana@empresa.com.br");

        mockMvc.perform(post("/public/commercial-leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn400WhenNameIsBlank() throws Exception {
        var request = new CommercialLeadRequest("", "Empresa Teste", "ana@empresa.com.br");

        mockMvc.perform(post("/public/commercial-leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenEmailIsInvalid() throws Exception {
        var request = new CommercialLeadRequest("Ana Silva", "Empresa Teste", "not-an-email");

        mockMvc.perform(post("/public/commercial-leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenCompanyIsBlank() throws Exception {
        var request = new CommercialLeadRequest("Ana Silva", "", "ana@empresa.com.br");

        mockMvc.perform(post("/public/commercial-leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenEmailIsBlank() throws Exception {
        var request = new CommercialLeadRequest("Ana Silva", "Empresa Teste", "");

        mockMvc.perform(post("/public/commercial-leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn429WhenRateLimitExceeded() throws Exception {
        when(clientIpResolver.resolve(any())).thenReturn("10.0.0.1");
        doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS))
                .when(commercialLeadService).submitLead(any(), anyString());

        var request = new CommercialLeadRequest("Ana Silva", "Empresa Teste", "ana@empresa.com.br");

        mockMvc.perform(post("/public/commercial-leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void shouldReturn400WhenBodyIsMissing() throws Exception {
        mockMvc.perform(post("/public/commercial-leads")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
