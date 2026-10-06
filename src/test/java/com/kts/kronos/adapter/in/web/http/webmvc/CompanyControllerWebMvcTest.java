package com.kts.kronos.adapter.in.web.http.webmvc;

import com.kts.kronos.adapter.in.web.http.CompanyController;
import com.kts.kronos.adapter.in.web.dto.company.CompanyHardDeleteResultDTO;
import com.kts.kronos.adapter.in.web.dto.company.CompanyResponse;
import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Company;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
                                  "cnpj": "11222333000181",
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
                .andExpect(status().isCreated());

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
    @DisplayName("registerCompany: deve validar CNPJ com @CNPJ validator")
    void shouldRejectInvalidCnpjWhenRegisteringCompany() throws Exception {
        doThrow(new BadRequestException("CNPJ já cadastrado"))
                .when(useCase).createCompany(any());

        mockMvc.perform(post("/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateCompanyJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("CNPJ já cadastrado"));
    }

    @Test
    @DisplayName("getCompany: deve retornar empresa por CNPJ")
    void shouldGetCompanyByCnpj() throws Exception {
        Company company = company(true);
        when(useCase.getCompanyResponse("11222333000181")).thenReturn(CompanyResponse.fromDomain(company));

        mockMvc.perform(get("/companies/{cnpj}", "11222333000181"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(company.companyId().toString()))
                .andExpect(jsonPath("$.name").value("Kronos Tech"))
                .andExpect(jsonPath("$.cnpj").value("11222333000181"));
    }

    @Test
    @DisplayName("getCompany: deve traduzir empresa inexistente")
    void shouldTranslateExceptionWhenGettingCompany() throws Exception {
        when(useCase.getCompanyResponse("11222333000181"))
                .thenThrow(new ResourceNotFoundException("Empresa não encontrada"));

        mockMvc.perform(get("/companies/{cnpj}", "11222333000181"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Empresa não encontrada"));
    }

    @Test
    @DisplayName("allCompanies: deve listar empresas sem filtro")
    void shouldListCompaniesWithoutFilter() throws Exception {
        when(useCase.listCompanies(null)).thenReturn(List.of(company(true), company(false)));

        mockMvc.perform(get("/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companies[0].name").value("Kronos Tech"))
                .andExpect(jsonPath("$.companies.length()").value(2));

        verify(useCase).listCompanies(null);
    }

    @Test
    @DisplayName("allCompanies: deve listar empresas filtradas por active")
    void shouldListCompaniesWithActiveFilter() throws Exception {
        when(useCase.listCompanies(true)).thenReturn(List.of(company(true)));

        mockMvc.perform(get("/companies").param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companies[0].active").value(true));

        verify(useCase).listCompanies(true);
    }

    @Test
    @DisplayName("updateCompany: deve delegar atualização")
    void shouldUpdateCompany() throws Exception {
        mockMvc.perform(patch("/companies/{cnpj}", "11222333000181")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Kronos Atualizada",
                                  "email": "novo@kronos.com",
                                  "active": true,
                                  "address": {
                                    "postalCode": "12345678",
                                    "number": "200"
                                  },
                                  "location": {
                                    "latitude": -22.90,
                                    "longitude": -43.20
                                  }
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(useCase).updateCompany(eq("11222333000181"), any());
    }

    @Test
    @DisplayName("updateCompany: deve retornar 400 para payload inválido")
    void shouldReturnBadRequestForInvalidUpdatePayload() throws Exception {
        mockMvc.perform(patch("/companies/{cnpj}", "11222333000181")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "email-invalido"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("updateCompany: deve traduzir empresa inexistente")
    void shouldTranslateExceptionWhenUpdatingCompany() throws Exception {
        doThrow(new ResourceNotFoundException("Empresa não encontrada"))
                .when(useCase).updateCompany(eq("11222333000181"), any());

        mockMvc.perform(patch("/companies/{cnpj}", "11222333000181")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Kronos Atualizada"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Empresa não encontrada"));
    }

    @Test
    @DisplayName("deactivateCompany: deve alternar ativação")
    void shouldToggleCompanyActivation() throws Exception {
        mockMvc.perform(patch("/companies/{cnpj}/toggle-activate", "11222333000181"))
                .andExpect(status().isNoContent());

        verify(useCase).toggleActivate("11222333000181");
    }

    @Test
    @DisplayName("deleteCompany: deve delegar exclusão")
    void shouldDeleteCompany() throws Exception {
        mockMvc.perform(delete("/companies/{cnpj}", "11222333000181"))
                .andExpect(status().isNoContent());

        verify(useCase).deleteByCnpj("11222333000181");
    }

    @Test
    @DisplayName("deleteCompany: deve traduzir empresa inexistente")
    void shouldTranslateExceptionWhenDeletingCompany() throws Exception {
        doThrow(new ResourceNotFoundException("Empresa não encontrada"))
                .when(useCase).deleteByCnpj("11222333000181");

        mockMvc.perform(delete("/companies/{cnpj}", "11222333000181"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Empresa não encontrada"));
    }

    @Test
    @DisplayName("checkCnpjAvailability: retorna 200 quando CNPJ existe")
    void shouldReturnOkWhenCnpjExists() throws Exception {
        when(useCase.cnpjExists("11222333000181")).thenReturn(true);

        mockMvc.perform(get("/companies/check-cnpj")
                        .param("cnpj", "11222333000181"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("checkCnpjAvailability: retorna 404 quando CNPJ não existe")
    void shouldReturnNotFoundWhenCnpjDoesNotExist() throws Exception {
        when(useCase.cnpjExists("11222333000181")).thenReturn(false);

        mockMvc.perform(get("/companies/check-cnpj")
                        .param("cnpj", "11222333000181"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("toggleTerminalFlag: alterna flag de terminal da empresa")
    void shouldToggleTerminalFlag() throws Exception {
        mockMvc.perform(patch("/companies/{cnpj}/toggle-terminal", "11222333000181"))
                .andExpect(status().isNoContent());
        verify(useCase).toggleTerminalFlag("11222333000181");
    }

    @Test
    @DisplayName("hardDeleteCompany: exclui empresa com hard delete e retorna resultado")
    void shouldHardDeleteCompany() throws Exception {
        var result = new CompanyHardDeleteResultDTO("11222333000181", "Kronos Tech", 2, 0, List.of());
        when(useCase.hardDeleteCompany("11222333000181")).thenReturn(result);

        mockMvc.perform(delete("/companies/{cnpj}/hard-delete", "11222333000181"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyCnpj").value("11222333000181"))
                .andExpect(jsonPath("$.employeesDeleted").value(2));

        verify(useCase).hardDeleteCompany("11222333000181");
    }


    private static String validCreateCompanyJson() {
        return """
                {
                  "name": "Kronos Tech",
                  "cnpj": "11222333000181",
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
                """;
    }

    private static Company company(boolean active) {
        return new Company(
                UUID.randomUUID(),
                "Kronos Tech",
                "11222333000181",
                "empresa@kronos.com",
                active,
                new Address("Rua A", "10", "12345678", "Rio", "RJ"),
                null,
                2,
                1
        );
    }
}
