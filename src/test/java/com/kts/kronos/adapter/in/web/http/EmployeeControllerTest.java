package com.kts.kronos.adapter.in.web.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kts.kronos.adapter.in.web.dto.address.AddressRequest;
import com.kts.kronos.adapter.in.web.dto.address.UpdateAddressRequest;
import com.kts.kronos.adapter.in.web.dto.employee.CreateEmployeeRequest;
import com.kts.kronos.adapter.in.web.dto.employee.EmployeeProfile;
import com.kts.kronos.adapter.in.web.dto.employee.UpdateEmployeeManagerRequest;
import com.kts.kronos.adapter.in.web.dto.employee.UpdateEmployeePartnerRequest;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
import com.kts.kronos.application.port.in.usecase.EmployeeUseCase;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.CPF_ALREADY_EXIST;
import static com.kts.kronos.constants.Messages.EMPLOYEE_NOT_FOUND;
import static com.kts.kronos.constants.Swagger.OWN_PROFILE_404;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployeeController.class)
@AutoConfigureMockMvc(addFilters = false) // Desativa filtros de segurança (JWT)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmployeeUseCase employeeUseCase;

    @MockitoBean
    private CompanyUseCase companyUseCase;

    private static final String BASE_URL = "/employee";
    private static final UUID EMP_ID = UUID.randomUUID();
    private static final UUID COMPANY_ID = UUID.randomUUID();
    private Employee employeeMock;

    @BeforeEach
    void setup() {
        // Objeto de domínio padrão para os testes
        var address = new Address("Rua A", "10", "25900000", "Mage", "RJ");
        employeeMock = new Employee(
                EMP_ID, "João Silva", "12345678901", "12345678901", "Dev", "joao@email.com",
                5000.0, "2199999999", true, address, COMPANY_ID, LocalDateTime.now(),
                false, null, null, null, null, null,
                null, null, null, null, null
        );
    }

    // ==================================================================================
    // CENÁRIOS DE SUCESSO (HAPPY PATH)
    // ==================================================================================

    @Test
    @DisplayName("Deve registrar funcionário com sucesso (201 Created)")
    void shouldRegisterEmployeeSuccessfully() throws Exception {
        // Arrange
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "João Silva", "11366653742", "12345678901", "Dev", "joao@email.com", 5000.0,
                "2199999999", new AddressRequest("25900000", "10"), COMPANY_ID, false,
                null, null, null, null, null, null, null, null, null, null
        );

        when(employeeUseCase.createEmployee(any(CreateEmployeeRequest.class))).thenReturn(employeeMock);
        when(companyUseCase.getCompanyNameById(COMPANY_ID)).thenReturn("Kronos Inc.");

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.fullName").value("João Silva"))
                .andExpect(jsonPath("$.companyName").value("Kronos Inc."));
    }

    @Test
    @DisplayName("Deve listar todos os funcionários (200 OK)")
    void shouldListEmployeesSuccessfully() throws Exception {
        when(employeeUseCase.listEmployees(any())).thenReturn(List.of(employeeMock));
        when(companyUseCase.getCompanyNameById(COMPANY_ID)).thenReturn("Kronos Inc.");

        mockMvc.perform(get(BASE_URL)
                        .param("active", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employees[0].fullName").value("João Silva"))
                .andExpect(jsonPath("$.employees[0].companyName").value("Kronos Inc."));

        verify(employeeUseCase).listEmployees(eq(true));
    }

    @Test
    @DisplayName("Deve buscar funcionário por ID (200 OK)")
    void shouldGetEmployeeByIdSuccessfully() throws Exception {
        when(employeeUseCase.getEmployee(EMP_ID)).thenReturn(employeeMock);
        when(companyUseCase.getCompanyNameById(COMPANY_ID)).thenReturn("Kronos Inc.");

        mockMvc.perform(get(BASE_URL + "/{id}", EMP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("João Silva"));
    }

    @Test
    @DisplayName("Deve atualizar funcionário (Manager) com sucesso (200 OK)")
    void shouldUpdateEmployeeSuccessfully() throws Exception {
        UpdateEmployeeManagerRequest request = new UpdateEmployeeManagerRequest(
                "João Atualizado", null, null, null, null, null, null, null,
                new UpdateAddressRequest("25900000", "20"), null, null, null, null, null, null, null, null, null, null
        );

        doNothing().when(employeeUseCase).updateEmployee(eq(EMP_ID), any());

        mockMvc.perform(patch(BASE_URL + "/manager/update-employee/{id}", EMP_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(employeeUseCase).updateEmployee(eq(EMP_ID), any());
    }

    @Test
    @DisplayName("Deve retornar perfil do próprio usuário (200 OK)")
    void shouldGetOwnProfileSuccessfully() throws Exception {
        EmployeeProfile profile = new EmployeeProfile(employeeMock, "MANAGER");

        when(employeeUseCase.getOwnProfile()).thenReturn(profile);
        when(companyUseCase.getCompanyNameById(COMPANY_ID)).thenReturn("Kronos Inc.");

        mockMvc.perform(get(BASE_URL + "/own-profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("João Silva"))
                .andExpect(jsonPath("$.role").value("MANAGER"));
    }

    @Test
    @DisplayName("Deve atualizar o próprio perfil (Partner) com sucesso (200 OK)")
    void shouldUpdateOwnProfileSuccessfully() throws Exception {
        UpdateEmployeePartnerRequest request = new UpdateEmployeePartnerRequest(
                "novo@email.com", "21988888888", new UpdateAddressRequest("25900000", "99")
        );

        doNothing().when(employeeUseCase).updateOwnProfile(any());

        mockMvc.perform(patch(BASE_URL + "/update-own-profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Deve deletar funcionário com sucesso (200 OK)")
    void shouldDeleteEmployeeSuccessfully() throws Exception {
        doNothing().when(employeeUseCase).deleteEmployee(EMP_ID);

        mockMvc.perform(delete(BASE_URL + "/{id}", EMP_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Deve marcar mensagens como lidas (200 OK)")
    void shouldMarkMessagesAsSeenSuccessfully() throws Exception {
        doNothing().when(employeeUseCase).markMessagesAsSeen();

        mockMvc.perform(post(BASE_URL + "/mark-messages-seen"))
                .andExpect(status().isNoContent());

        verify(employeeUseCase).markMessagesAsSeen();
    }

    @Test
    @DisplayName("Deve verificar disponibilidade de CPF (200 OK se existir)")
    void shouldReturn200IfCpfExists() throws Exception {
        String cpf = "12345678901";
        when(employeeUseCase.cpfExists(cpf)).thenReturn(true);

        mockMvc.perform(get(BASE_URL + "/check-cpf")
                        .param("cpf", cpf))
                .andExpect(status().isOk());
    }

    // ==================================================================================
    // CENÁRIOS DE FALHA (FAILURE PATH)
    // ==================================================================================

    @Test
    @DisplayName("Deve retornar 400 Bad Request se o payload de criação for inválido")
    void shouldReturn400WhenCreatePayloadIsInvalid() throws Exception {
        CreateEmployeeRequest invalidRequest = new CreateEmployeeRequest(
                "", // Nome vazio
                "123", // CPF inválido
                null, "", "email-errado", -100.0, null, null, null, false, null, null, null, null, null, null, null, null, null, null
        );

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request se houver erro de negócio na criação (Ex: CPF duplicado)")
    void shouldReturn400WhenBusinessErrorOnCreate() throws Exception {
        // Request válido para passar no @Valid
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "João", "11366653742", null, "Dev", "j@k.com", 1000.0,
                null, new AddressRequest("20000000", "1"), COMPANY_ID, false, null, null, null, null, null, null, null, null, null, null
        );

        when(employeeUseCase.createEmployee(any())).thenThrow(new BadRequestException(CPF_ALREADY_EXIST));

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(CPF_ALREADY_EXIST));
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found ao buscar funcionário inexistente")
    void shouldReturn404WhenEmployeeNotFound() throws Exception {
        when(employeeUseCase.getEmployee(EMP_ID)).thenThrow(new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));

        mockMvc.perform(get(BASE_URL + "/{id}", EMP_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(EMPLOYEE_NOT_FOUND));
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found ao listar funcionários quando empresa do funcionário não for encontrada")
    void shouldReturn404WhenListingEmployeesAndCompanyIsMissing() throws Exception {
        when(employeeUseCase.listEmployees(any())).thenReturn(List.of(employeeMock));
        when(companyUseCase.getCompanyNameById(COMPANY_ID)).thenThrow(new ResourceNotFoundException("Empresa não encontrada"));

        mockMvc.perform(get(BASE_URL).param("active", "true"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Empresa não encontrada"));
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found ao tentar atualizar funcionário inexistente")
    void shouldReturn404WhenUpdatingNonExistentEmployee() throws Exception {
        UpdateEmployeeManagerRequest request = new UpdateEmployeeManagerRequest(
                "Novo Nome", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
        );

        doThrow(new ResourceNotFoundException(EMPLOYEE_NOT_FOUND))
                .when(employeeUseCase).updateEmployee(eq(EMP_ID), any());

        mockMvc.perform(patch(BASE_URL + "/manager/update-employee/{id}", EMP_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o payload de atualização do gestor for inválido")
    void shouldReturn400WhenManagerUpdatePayloadIsInvalid() throws Exception {
        UpdateEmployeeManagerRequest invalidRequest = new UpdateEmployeeManagerRequest(
                "", null, null, null, "email-invalido", -1.0, null, null,
                new UpdateAddressRequest("", ""), null, null, null, null, null, null, null, null, null, null
        );

        mockMvc.perform(patch(BASE_URL + "/manager/update-employee/{id}", EMP_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found ao tentar atualizar o próprio perfil quando funcionário não existe")
    void shouldReturn404WhenUpdatingOwnProfileForMissingEmployee() throws Exception {
        UpdateEmployeePartnerRequest request = new UpdateEmployeePartnerRequest(
                "novo@email.com", "21988888888", new UpdateAddressRequest("25900000", "99")
        );

        doThrow(new ResourceNotFoundException(EMPLOYEE_NOT_FOUND)).when(employeeUseCase).updateOwnProfile(any());

        mockMvc.perform(patch(BASE_URL + "/update-own-profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(EMPLOYEE_NOT_FOUND));
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found ao tentar excluir funcionário inexistente")
    void shouldReturn404WhenDeletingMissingEmployee() throws Exception {
        doThrow(new ResourceNotFoundException(EMPLOYEE_NOT_FOUND)).when(employeeUseCase).deleteEmployee(EMP_ID);

        mockMvc.perform(delete(BASE_URL + "/{id}", EMP_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(EMPLOYEE_NOT_FOUND));
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found ao tentar marcar mensagens como lidas para funcionário inexistente")
    void shouldReturn404WhenMarkMessagesAsSeenForMissingEmployee() throws Exception {
        doThrow(new ResourceNotFoundException(EMPLOYEE_NOT_FOUND)).when(employeeUseCase).markMessagesAsSeen();

        mockMvc.perform(post(BASE_URL + "/mark-messages-seen"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(EMPLOYEE_NOT_FOUND));
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found se o CPF NÃO estiver cadastrado (Disponível)")
    void shouldReturn404IfCpfDoesNotExist() throws Exception {
        String cpf = "00000000000";
        when(employeeUseCase.cpfExists(cpf)).thenReturn(false);

        // Lógica do controller: se não existe, retorna 404 (Not Found)
        mockMvc.perform(get(BASE_URL + "/check-cpf")
                        .param("cpf", cpf))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve retornar 404 ao tentar buscar perfil próprio sem estar autenticado ou sem funcionário vinculado")
    void shouldReturn404WhenGettingOwnProfileFails() throws Exception {
        // Caso onde o token não tem userId ou employeeId válido no contexto do serviço
        when(employeeUseCase.getOwnProfile()).thenThrow(new ResourceNotFoundException(OWN_PROFILE_404));

        mockMvc.perform(get(BASE_URL + "/own-profile"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(OWN_PROFILE_404));
    }
}
