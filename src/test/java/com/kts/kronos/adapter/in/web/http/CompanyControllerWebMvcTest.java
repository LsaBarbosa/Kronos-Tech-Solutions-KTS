package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CompanyController.class)
@AutoConfigureMockMvc(addFilters = false)
class CompanyControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompanyUseCase useCase;

    @Test
    @DisplayName("registerCompany: deve delegar criação")
    void shouldRegisterCompany() throws Exception {
        mockMvc.perform(post("/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Kronos Tech",
                                  "cnpj": "12345678000199",
                                  "email": "empresa@kronos.com",
                                  "address": {
                                    "postalCode": "12345678",
                                    "number": "10"
                                  },
                                  "employeeRequest": {
                                    "fullName": "Gestor Inicial",
                                    "cpf": "12345678909",
                                    "pis": "12345678901",
                                    "jobPosition": "Manager",
                                    "email": "gestor@kronos.com",
                                    "salary": 5000,
                                    "phone": "21999999999",
                                    "address": {
                                      "postalCode": "12345678",
                                      "number": "10"
                                    },
                                    "companyId": null,
                                    "homeOffice": false
                                  },
                                  "location": {
                                    "latitude": -22.90,
                                    "longitude": -43.20
                                  }
                                }
                                """))
                .andExpect(status().isOk());

        verify(useCase).createCompany(any());
    }

    @Test
    @DisplayName("registerCompany: deve retornar 400 para payload inválido")
    void shouldReturnBadRequestForInvalidPayload() throws Exception {
        mockMvc.perform(post("/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "cnpj": "123",
                                  "email": "email-invalido"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("checkCnpjAvailability: retorna 200 quando CNPJ existe")
    void shouldReturnOkWhenCnpjExists() throws Exception {
        when(useCase.cnpjExists("12345678000199")).thenReturn(true);

        mockMvc.perform(get("/companies/check-cnpj")
                        .param("cnpj", "12345678000199"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("checkCnpjAvailability: retorna 404 quando CNPJ não existe")
    void shouldReturnNotFoundWhenCnpjDoesNotExist() throws Exception {
        when(useCase.cnpjExists("12345678000199")).thenReturn(false);

        mockMvc.perform(get("/companies/check-cnpj")
                        .param("cnpj", "12345678000199"))
                .andExpect(status().isNotFound());
    }
}