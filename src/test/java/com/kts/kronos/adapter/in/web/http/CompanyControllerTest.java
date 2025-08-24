package com.kts.kronos.adapter.in.web.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kts.kronos.adapter.in.web.dto.address.AddressResponse;
import com.kts.kronos.adapter.in.web.dto.address.UpdateAddressRequest;
import com.kts.kronos.adapter.in.web.dto.company.CompanyListResponse;
import com.kts.kronos.adapter.in.web.dto.company.CompanyResponse;
import com.kts.kronos.adapter.in.web.dto.company.CreateCompanyRequest;
import com.kts.kronos.adapter.in.web.dto.company.UpdateCompanyRequest;
import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Company;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.kts.kronos.constants.ApiPaths.BY_CNPJ;
import static com.kts.kronos.constants.ApiPaths.COMPANIES;
import static com.kts.kronos.constants.ApiPaths.TOGGLE_ACTIVATE_EMPLOYEE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(CompanyController.class)
@DisplayName("CompanyController Unit Tests")
class CompanyControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CompanyUseCase useCase;

    private String cnpj;
    private String ctoRole;
    private CreateCompanyRequest createCompanyRequest;
    private UpdateCompanyRequest updateCompanyRequest;
    private Company company;
    private CompanyResponse companyResponse;

    @BeforeEach
    void setUp() {
        cnpj = "12345678901234";
        ctoRole = "CTO";
        createCompanyRequest = new CreateCompanyRequest(
                "Test Company",
                cnpj,
                "contact@testcompany.com",
                new com.kts.kronos.adapter.in.web.dto.address.AddressRequest("12345678", "123")
        );
        updateCompanyRequest = new UpdateCompanyRequest(
                "Updated Company",
                "updated@testcompany.com",
                null,
                new UpdateAddressRequest("87654321", "456")
        );
        Address address = new Address("Street", "123", "12345678", "City", "State");
        company = new Company(UUID.randomUUID(), "Test Company", cnpj, "contact@testcompany.com", true, address);
        companyResponse = new CompanyResponse(
                company.companyId(),
                company.name(),
                company.cnpj(),
                company.email(),
                company.active(),
                new AddressResponse(address.street(), address.number(), address.postalCode(), address.city(), address.state())
        );
    }

    @Nested
    @DisplayName("POST /companies")
    class RegisterCompanyTests {
        @Test
        @DisplayName("Should register a new company and return 201 Created")
        void registerCompany_Success_Returns201() throws Exception {
            doNothing().when(useCase).createCompany(any(CreateCompanyRequest.class));

            mockMvc.perform(post(COMPANIES)
                            .with(user("ctoUser").roles(ctoRole))
                            .with(csrf()) // Adicionado o token CSRF
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createCompanyRequest)))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("GET /companies/{cnpj}")
    class GetCompanyTests {
        @Test
        @DisplayName("Should return a company by CNPJ and 200 OK")
        void getCompany_Success_ReturnsCompanyResponse() throws Exception {
            when(useCase.getCompany(cnpj)).thenReturn(company);

            mockMvc.perform(get(COMPANIES + BY_CNPJ, cnpj)
                            .with(user("ctoUser").roles(ctoRole))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().json(objectMapper.writeValueAsString(companyResponse)));
        }
    }

    @Nested
    @DisplayName("GET /companies")
    class AllCompaniesTests {
        @Test
        @DisplayName("Should return a list of companies and 200 OK")
        void allCompanies_Success_ReturnsListOfCompanies() throws Exception {
            List<Company> companiesList = Collections.singletonList(company);
            when(useCase.listCompanies(any())).thenReturn(companiesList);

            CompanyListResponse expectedResponse = new CompanyListResponse(
                    companiesList.stream().map(CompanyResponse::fromDomain).toList()
            );

            mockMvc.perform(get(COMPANIES)
                            .with(user("ctoUser").roles(ctoRole))
                            .param("active", "true")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().json(objectMapper.writeValueAsString(expectedResponse)));
        }
    }

    @Nested
    @DisplayName("PATCH /companies/{cnpj}")
    class UpdateCompanyTests {
        @Test
        @DisplayName("Should update a company and return 200 OK")
        void updateCompany_Success_Returns200() throws Exception {
            doNothing().when(useCase).updateCompany(eq(cnpj), any(UpdateCompanyRequest.class));

            mockMvc.perform(patch(COMPANIES + BY_CNPJ, cnpj)
                            .with(user("ctoUser").roles(ctoRole))
                            .with(csrf()) // Adicionado o token CSRF
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateCompanyRequest)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PATCH /companies/{cnpj}/toggle-activate")
    class DeactivateCompanyTests {
        @Test
        @DisplayName("Should toggle company activation and return 200 OK")
        void deactivateCompany_Success_Returns200() throws Exception {
            doNothing().when(useCase).toggleActivate(cnpj);

            mockMvc.perform(patch(COMPANIES + TOGGLE_ACTIVATE_EMPLOYEE, cnpj)
                            .with(user("ctoUser").roles(ctoRole))
                            .with(csrf())) // Adicionado o token CSRF
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /companies/{cnpj}")
    class DeleteCompanyTests {
        @Test
        @DisplayName("Should delete a company and return 204 No Content")
        void deleteCompany_Success_Returns204() throws Exception {
            doNothing().when(useCase).deleteByCnpj(cnpj);

            mockMvc.perform(delete(COMPANIES + BY_CNPJ, cnpj)
                            .with(user("ctoUser").roles(ctoRole))
                            .with(csrf())) // Adicionado o token CSRF
                    .andExpect(status().isNoContent());
        }
    }
}