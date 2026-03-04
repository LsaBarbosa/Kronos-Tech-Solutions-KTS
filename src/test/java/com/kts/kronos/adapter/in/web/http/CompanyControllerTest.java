package com.kts.kronos.adapter.in.web.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kts.kronos.adapter.in.web.dto.address.AddressRequest;
import com.kts.kronos.adapter.in.web.dto.address.UpdateAddressRequest;
import com.kts.kronos.adapter.in.web.dto.company.CreateCompanyRequest;
import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.adapter.in.web.dto.company.UpdateCompanyRequest;
import com.kts.kronos.adapter.in.web.dto.employee.CreateEmployeeRequest;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Company;
import org.junit.jupiter.api.BeforeEach;
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

import static com.kts.kronos.constants.Messages.COMPANY_ALREADY_EXIST;
import static com.kts.kronos.constants.Messages.COMPANY_NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@WebMvcTest(CompanyController.class)
@AutoConfigureMockMvc(addFilters = false) // Desabilita segurança para focar no teste do Controller
class CompanyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Mockamos apenas o UseCase, pois o Controller delega tudo para ele
    @MockitoBean
    private CompanyUseCase companyUseCase;

    // Constantes para facilitar
    private static final String BASE_URL = "/companies";
    private static final String VALID_CNPJ = "12345678000199";
    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final String INVALID_CNPJ = "00000000000000";
    private Company companyMock;

    @BeforeEach
    void setup() {
        // Prepara um objeto de domínio Company válido para retornos
        var address = new Address("Rua Teste", "100", "25900000", "Mage", "RJ");
        var location = new com.kts.kronos.adapter.in.web.dto.company.Location(-22.0, -43.0);

        companyMock = new Company(
                COMPANY_ID,
                "Kronos Tech",
                VALID_CNPJ,
                "contato@kronos.com",
                true,
                address,
                location,
                10, // Active employees
                2   // Inactive employees
        );
    }

    @Test
    @DisplayName("Deve registrar empresa com sucesso (200 OK)")
    void shouldRegisterCompanySuccessfully() throws Exception {
        // 1. Arrange: Criar um objeto Employee válido (sem Mock) para passar na validação @Valid
        var validEmployeeRequest = new CreateEmployeeRequest(
                "Administrador",
                "15902863759", // CPF Válido para passar na anotação @CPF
                "12345678901",
                "Gerente",
                "admin@novaempresa.com",
                5000.0,
                "21999999999",
                new AddressRequest("25930790", "100"),
                null, // companyId pode ser nulo aqui, pois será gerado/vinculado
                false,
                null, // faceImageBase64
                null, null, null, null, // Horários opcionais
                null, null, null, null, null // Configurações de escala opcionais
        );

        CreateCompanyRequest request = new CreateCompanyRequest(
                "Nova Empresa LTDA",
                "12345678000199", // CNPJ
                "email@empresa.com",
                new AddressRequest("25900000", "123"),
                validEmployeeRequest, // Passamos o objeto real
                new Location(-22.5, -43.1)
        );

        doNothing().when(companyUseCase).createCompany(any(CreateCompanyRequest.class));

        // 2. Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(companyUseCase, times(1)).createCompany(any(CreateCompanyRequest.class));
    }

    @Test
    @DisplayName("Deve buscar empresa por CNPJ e retornar dados (200 OK)")
    void shouldGetCompanyByCnpjSuccessfully() throws Exception {
        // Arrange
        when(companyUseCase.getCompany(VALID_CNPJ)).thenReturn(companyMock);

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/{cnpj}", VALID_CNPJ)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Kronos Tech"))
                .andExpect(jsonPath("$.cnpj").value(VALID_CNPJ))
                .andExpect(jsonPath("$.activeEmployees").value(10));
    }

    @Test
    @DisplayName("Deve listar empresas filtradas ou não (200 OK)")
    void shouldListCompaniesSuccessfully() throws Exception {
        // Arrange
        when(companyUseCase.listCompanies(true)).thenReturn(List.of(companyMock));

        // Act & Assert
        mockMvc.perform(get(BASE_URL)
                        .param("active", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companies[0].name").value("Kronos Tech"));
    }

    @Test
    @DisplayName("Deve atualizar empresa com sucesso (200 OK)")
    void shouldUpdateCompanySuccessfully() throws Exception {
        // Arrange
        UpdateCompanyRequest request = new UpdateCompanyRequest(
                "Nome Atualizado",
                "novo@email.com",
                true,
                new UpdateAddressRequest("25900000", "50"),
                new Location(-22.0, -43.0)
        );

        doNothing().when(companyUseCase).updateCompany(eq(VALID_CNPJ), any(UpdateCompanyRequest.class));

        // Act & Assert
        mockMvc.perform(patch(BASE_URL + "/{cnpj}", VALID_CNPJ)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("Deve alternar status (ativar/desativar) da empresa (200 OK)")
    void shouldToggleCompanyActivation() throws Exception {
        // Arrange
        doNothing().when(companyUseCase).toggleActivate(VALID_CNPJ);

        // Act & Assert
        mockMvc.perform(patch(BASE_URL + "/{cnpj}/toggle-activate", VALID_CNPJ))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(companyUseCase).toggleActivate(VALID_CNPJ);
    }

    @Test
    @DisplayName("Deve deletar empresa por CNPJ (200 OK)")
    void shouldDeleteCompanySuccessfully() throws Exception {
        // Arrange
        doNothing().when(companyUseCase).deleteByCnpj(VALID_CNPJ);

        // Act & Assert
        mockMvc.perform(delete(BASE_URL + "/{cnpj}", VALID_CNPJ))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("Deve verificar disponibilidade de CNPJ retornando 200 OK se existir")
    void shouldReturn200IfCnpjExists() throws Exception {
        // Arrange
        when(companyUseCase.cnpjExists(VALID_CNPJ)).thenReturn(true);

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/check-cnpj")
                        .param("cnpj", VALID_CNPJ))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando payload de criação for inválido (@Valid)")
    void shouldReturn400WhenCreatePayloadIsInvalid() throws Exception {
        // Request vazio ou com campos inválidos para disparar o Bean Validation
        CreateCompanyRequest invalidRequest = new CreateCompanyRequest(
                "", // Nome vazio (Erro)
                "123", // CNPJ inválido (Erro)
                "email-invalido", // Email sem formato (Erro)
                null,
                null,
                null
        );

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest()) // Valida 400
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.errors").isArray()); // Valida se há lista de erros de campo
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando CNPJ já existe (Regra de Negócio)")
    void shouldReturn400WhenCompanyAlreadyExists() throws Exception {
        // Objeto válido para passar na validação @Valid, mas falhar no UseCase

        var validEmployeeRequest = new CreateEmployeeRequest(
                "Administrador",
                "15902863759", // CPF Válido para passar na anotação @CPF
                "12345678901",
                "Gerente",
                "admin@novaempresa.com",
                5000.0,
                "21999999999",
                new AddressRequest("25930790", "100"),
                null, // companyId pode ser nulo aqui, pois será gerado/vinculado
                false,
                null, // faceImageBase64
                null, null, null, null, // Horários opcionais
                null, null, null, null, null // Configurações de escala opcionais
        );

        CreateCompanyRequest request = new CreateCompanyRequest(
                "Empresa Duplicada",
                "12345678000199",
                "email@teste.com",
                new AddressRequest("25900000", "10"),
                validEmployeeRequest, // Mock para passar @Valid
                new Location(-22.0, -43.0)
        );

        // Simula exceção lançada pelo Serviço
        doThrow(new BadRequestException(COMPANY_ALREADY_EXIST))
                .when(companyUseCase).createCompany(any(CreateCompanyRequest.class));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(COMPANY_ALREADY_EXIST));
    }

    // --- CENÁRIOS DE BUSCA (GET) ---

    @Test
    @DisplayName("Deve retornar 404 Not Found ao buscar empresa inexistente")
    void shouldReturn404WhenCompanyNotFound() throws Exception {
        when(companyUseCase.getCompany(INVALID_CNPJ))
                .thenThrow(new ResourceNotFoundException(COMPANY_NOT_FOUND + INVALID_CNPJ));

        mockMvc.perform(get(BASE_URL + "/{cnpj}", INVALID_CNPJ)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(COMPANY_NOT_FOUND + INVALID_CNPJ));
    }

    // --- CENÁRIOS DE ATUALIZAÇÃO (PATCH) ---

    @Test
    @DisplayName("Deve retornar 404 Not Found ao tentar atualizar empresa inexistente")
    void shouldReturn404WhenUpdatingNonExistentCompany() throws Exception {
        UpdateCompanyRequest request = new UpdateCompanyRequest(
                "Novo Nome", null, null, null, null
        );

        doThrow(new ResourceNotFoundException(COMPANY_NOT_FOUND))
                .when(companyUseCase).updateCompany(eq(INVALID_CNPJ), any(UpdateCompanyRequest.class));

        mockMvc.perform(patch(BASE_URL + "/{cnpj}", INVALID_CNPJ)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request se falhar regra de geolocalização na atualização")
    void shouldReturn400WhenGeolocationIsMissingOnAddressUpdate() throws Exception {
        // Cenário: Atualizar endereço sem enviar Location (Regra do Service)
        String geolocationErrorMsg = "Location (latitude e longitude) é obrigatório se o endereço for alterado.";

        UpdateCompanyRequest request = new UpdateCompanyRequest(
                null, null, null,
                new UpdateAddressRequest("25930790", "100"), // Endereço novo
                null // Sem Location
        );

        doThrow(new BadRequestException(geolocationErrorMsg))
                .when(companyUseCase).updateCompany(any(), any());

        mockMvc.perform(patch(BASE_URL + "/{cnpj}", "12345678000199")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(geolocationErrorMsg));
    }

    // --- CENÁRIOS DE EXCLUSÃO (DELETE) ---

    @Test
    @DisplayName("Deve retornar 404 Not Found ao tentar deletar empresa inexistente")
    void shouldReturn404WhenDeletingNonExistentCompany() throws Exception {
        doThrow(new ResourceNotFoundException(COMPANY_NOT_FOUND))
                .when(companyUseCase).deleteByCnpj(INVALID_CNPJ);

        mockMvc.perform(delete(BASE_URL + "/{cnpj}", INVALID_CNPJ))
                .andExpect(status().isNotFound());
    }

    // --- CENÁRIOS DE CHECK DE DISPONIBILIDADE ---

    @Test
    @DisplayName("Deve retornar 404 Not Found quando o CNPJ NÃO existe (está disponível)")
    void shouldReturn404WhenCnpjIsAvailable() throws Exception {
        // Nota: Neste endpoint específico, 404 significa "Disponível/Não Encontrado", o que pode ser um sucesso dependendo da ótica do front-end.
        // Testamos aqui como "Failure" do ponto de vista de "Recurso não encontrado".

        String availableCnpj = "99999999000199";
        when(companyUseCase.cnpjExists(availableCnpj)).thenReturn(false);

        mockMvc.perform(get(BASE_URL + "/check-cnpj")
                        .param("cnpj", availableCnpj))
                .andExpect(status().isNotFound());
    }
}