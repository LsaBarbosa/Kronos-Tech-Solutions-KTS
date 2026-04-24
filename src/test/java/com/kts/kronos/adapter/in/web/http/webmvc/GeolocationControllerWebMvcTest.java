package com.kts.kronos.adapter.in.web.http.webmvc;

import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.adapter.in.web.http.GeolocationController;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.GeolocationUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GeolocationController.class)
@AutoConfigureMockMvc(addFilters = false)
class GeolocationControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GeolocationUseCase useCase;

    @Test
    @DisplayName("resolve: deve retornar latitude e longitude")
    void shouldResolveCoordinates() throws Exception {
        when(useCase.resolve(any())).thenReturn(new Location(-22.509804, -43.177544));

        mockMvc.perform(post("/geolocation/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "postalCode": "25900000",
                                  "number": "123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude").value(-22.509804))
                .andExpect(jsonPath("$.longitude").value(-43.177544));

        verify(useCase).resolve(any());
    }

    @Test
    @DisplayName("resolve: deve retornar 400 para payload inválido")
    void shouldReturnBadRequestForInvalidPayload() throws Exception {
        mockMvc.perform(post("/geolocation/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "postalCode": "123",
                                  "number": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test
    @DisplayName("resolve: deve traduzir CEP/localização não encontrados")
    void shouldTranslateNotFoundException() throws Exception {
        doThrow(new ResourceNotFoundException("CEP não encontrado:25900000"))
                .when(useCase).resolve(any());

        mockMvc.perform(post("/geolocation/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "postalCode": "25900000",
                                  "number": "123"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("CEP não encontrado:25900000"));
    }
}
