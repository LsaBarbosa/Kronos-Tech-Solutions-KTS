package com.kts.kronos.adapter.in.web.http.webmvc;

import com.kts.kronos.adapter.in.web.dto.employee.EmployeeProfile;
import com.kts.kronos.adapter.in.web.http.EmployeeController;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
import com.kts.kronos.application.port.in.usecase.EmployeeUseCase;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.enuns.WorkScheduleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
@AutoConfigureMockMvc(addFilters = false)
class EmployeeControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmployeeUseCase useCase;

    @MockitoBean
    private CompanyUseCase companyUseCase;

    @Test
    @DisplayName("registerEmployee: deve criar colaborador e retornar Location")
    void shouldRegisterEmployee() throws Exception {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        var employee = employee(employeeId, companyId);

        when(useCase.createEmployee(any())).thenReturn(employee);
        when(companyUseCase.getCompanyNameById(companyId)).thenReturn("Kronos Tech");

        mockMvc.perform(post("/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Lucas Silva",
                                  "cpf": "52998224725",
                                  "pis": "12345678901",
                                  "jobPosition": "Software Engineer",
                                  "email": "lucas@kronos.com",
                                  "salary": 6500.00,
                                  "phone": "21999999999",
                                  "address": {
                                    "postalCode": "12345678",
                                    "number": "100"
                                  },
                                  "companyId": "%s",
                                  "homeOffice": false,
                                  "workStartTime": "08:00",
                                  "workEndTime": "17:00",
                                  "breakStartTime": "12:00",
                                  "breakEndTime": "13:00",
                                  "scheduleType": "TRADITIONAL_5X2",
                                  "scaleStartDate": "2026-01-01",
                                  "preferredDayOff": "SUNDAY",
                                  "weekendOffIndex": 1,
                                  "fixedWorkDays": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"]
                                }
                                """.formatted(companyId)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/employee/" + employeeId)))
                .andExpect(jsonPath("$.employeeId").value(employeeId.toString()))
                .andExpect(jsonPath("$.fullName").value("Lucas Silva"))
                .andExpect(jsonPath("$.maskedCpf").value("529.***.725"))
                .andExpect(jsonPath("$.companyName").value("Kronos Tech"))
                .andExpect(jsonPath("$.cpf").doesNotExist())
                .andExpect(jsonPath("$.faceS3ObjectKey").doesNotExist());

        verify(useCase).createEmployee(any());
    }

    @Test
    @DisplayName("registerEmployee: deve retornar 400 para payload inválido")
    void shouldReturnBadRequestForInvalidCreatePayload() throws Exception {
        mockMvc.perform(post("/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "",
                                  "cpf": "123",
                                  "jobPosition": "",
                                  "email": "email-invalido",
                                  "salary": -1,
                                  "address": {
                                    "postalCode": "123",
                                    "number": ""
                                  },
                                  "homeOffice": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].name", hasItem("fullName")))
                .andExpect(jsonPath("$.errors[*].name", hasItem("cpf")))
                .andExpect(jsonPath("$.errors[*].name", hasItem("email")))
                .andExpect(jsonPath("$.errors[*].name", hasItem("salary")));
    }

    @Test
    @DisplayName("allEmployees: deve listar colaboradores sem filtro")
    void shouldListEmployeesWithoutFilter() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        var employee = employee(employeeId, companyId);

        when(useCase.listEmployees(null)).thenReturn(List.of(employee));
        when(companyUseCase.getCompanyNameById(companyId)).thenReturn("Kronos Tech");

        mockMvc.perform(get("/employee"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employees[0].employeeId").value(employeeId.toString()))
                .andExpect(jsonPath("$.employees[0].fullName").value("Lucas Silva"))
                .andExpect(jsonPath("$.employees[0].maskedCpf").value("529.***.725"))
                .andExpect(jsonPath("$.employees[0].companyName").value("Kronos Tech"))
                .andExpect(jsonPath("$.employees[0].cpf").doesNotExist())
                .andExpect(jsonPath("$.employees[0].faceS3ObjectKey").doesNotExist());

        verify(useCase).listEmployees(null);
    }

    @Test
    @DisplayName("allEmployees: deve listar colaboradores com filtro active")
    void shouldListEmployeesWithActiveFilter() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        var employee = employee(employeeId, companyId);

        when(useCase.listEmployees(true)).thenReturn(List.of(employee));
        when(companyUseCase.getCompanyNameById(companyId)).thenReturn("Kronos Tech");

        mockMvc.perform(get("/employee").param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employees[0].employeeId").value(employeeId.toString()));

        verify(useCase).listEmployees(true);
    }

    @Test
    @DisplayName("getEmployee: deve retornar colaborador por employeeId")
    void shouldGetEmployeeById() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        var employee = employee(employeeId, companyId);

        when(useCase.getEmployee(employeeId)).thenReturn(employee);
        when(companyUseCase.getCompanyNameById(companyId)).thenReturn("Kronos Tech");

        mockMvc.perform(get("/employee/{employeeId}", employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(employeeId.toString()))
                .andExpect(jsonPath("$.fullName").value("Lucas Silva"))
                .andExpect(jsonPath("$.maskedCpf").value("529.***.725"))
                .andExpect(jsonPath("$.companyName").value("Kronos Tech"))
                .andExpect(jsonPath("$.cpf").doesNotExist())
                .andExpect(jsonPath("$.faceS3ObjectKey").doesNotExist());
    }

    @Test
    @DisplayName("getEmployee: deve traduzir exceção para 404")
    void shouldTranslateExceptionWhenGettingEmployee() throws Exception {
        UUID employeeId = UUID.randomUUID();

        when(useCase.getEmployee(employeeId))
                .thenThrow(new ResourceNotFoundException("Colaborador não encontrado"));

        mockMvc.perform(get("/employee/{employeeId}", employeeId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Colaborador não encontrado"));
    }

    @Test
    @DisplayName("updateEmployee: deve delegar atualização por gestor")
    void shouldUpdateEmployeeByManager() throws Exception {
        UUID employeeId = UUID.randomUUID();

        mockMvc.perform(patch("/employee/manager/update-employee/{employeeId}", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "novo@kronos.com",
                                  "salary": 7000.00,
                                  "homeOffice": true
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(useCase).updateEmployee(eq(employeeId), any());
    }

    @Test
    @DisplayName("updateEmployee: deve retornar 400 para payload inválido")
    void shouldReturnBadRequestForInvalidManagerUpdatePayload() throws Exception {
        UUID employeeId = UUID.randomUUID();

        mockMvc.perform(patch("/employee/manager/update-employee/{employeeId}", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "email-invalido",
                                  "salary": -10
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].name", hasItem("email")))
                .andExpect(jsonPath("$.errors[*].name", hasItem("salary")));
    }

    @Test
    @DisplayName("getOwnProfile: deve retornar o próprio perfil")
    void shouldGetOwnProfile() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        var employee = employee(employeeId, companyId);

        when(useCase.getOwnProfile()).thenReturn(new EmployeeProfile(employee, "MANAGER"));
        when(companyUseCase.getCompanyNameById(companyId)).thenReturn("Kronos Tech");

        mockMvc.perform(get("/employee/own-profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(employeeId.toString()))
                .andExpect(jsonPath("$.maskedCpf").value("529.***.725"))
                .andExpect(jsonPath("$.role").value("MANAGER"))
                .andExpect(jsonPath("$.companyName").value("Kronos Tech"))
                .andExpect(jsonPath("$.cpf").doesNotExist())
                .andExpect(jsonPath("$.faceS3ObjectKey").doesNotExist());
    }

    @Test
    @DisplayName("updateOwnProfile: deve delegar atualização do próprio perfil")
    void shouldUpdateOwnProfile() throws Exception {
        mockMvc.perform(patch("/employee/update-own-profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "novo@kronos.com",
                                  "phone": "21988888888",
                                  "address": {
                                    "postalCode": "12345678",
                                    "number": "200"
                                  }
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(useCase).updateOwnProfile(any());
    }

    @Test
    @DisplayName("updateOwnProfile: deve retornar 400 para payload inválido")
    void shouldReturnBadRequestForInvalidOwnProfilePayload() throws Exception {
        mockMvc.perform(patch("/employee/update-own-profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "email-invalido",
                                  "address": {
                                    "postalCode": "123",
                                    "number": ""
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].name", hasItem("email")));
    }

    @Test
    @DisplayName("deleteEmployee: deve delegar exclusão")
    void shouldDeleteEmployee() throws Exception {
        UUID employeeId = UUID.randomUUID();

        mockMvc.perform(delete("/employee/{employeeId}", employeeId))
                .andExpect(status().isNoContent());

        verify(useCase).deleteEmployee(employeeId);
    }

    @Test
    @DisplayName("deleteEmployee: deve traduzir colaborador inexistente")
    void shouldTranslateExceptionWhenDeletingEmployee() throws Exception {
        UUID employeeId = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Colaborador não encontrado"))
                .when(useCase).deleteEmployee(employeeId);

        mockMvc.perform(delete("/employee/{employeeId}", employeeId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Colaborador não encontrado"));
    }

    @Test
    @DisplayName("markMessagesAsSeen: deve delegar marcação de mensagens vistas")
    void shouldMarkMessagesAsSeen() throws Exception {
        mockMvc.perform(post("/employee/mark-messages-seen"))
                .andExpect(status().isNoContent());

        verify(useCase).markMessagesAsSeen();
    }

    @Test
    @DisplayName("markMessagesAsSeen: deve traduzir erro de regra")
    void shouldTranslateExceptionWhenMarkingMessagesAsSeen() throws Exception {
        doThrow(new BadRequestException("Usuário sem colaborador vinculado"))
                .when(useCase).markMessagesAsSeen();

        mockMvc.perform(post("/employee/mark-messages-seen"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Usuário sem colaborador vinculado"));
    }

    @Test
    @DisplayName("checkCpfAvailability: retorna 200 quando CPF existe")
    void shouldReturnOkWhenCpfExists() throws Exception {
        when(useCase.cpfExists("52998224725")).thenReturn(true);

        mockMvc.perform(get("/employee/check-cpf").param("cpf", "52998224725"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("checkCpfAvailability: retorna 404 quando CPF não existe")
    void shouldReturnNotFoundWhenCpfDoesNotExist() throws Exception {
        when(useCase.cpfExists("52998224725")).thenReturn(false);

        mockMvc.perform(get("/employee/check-cpf").param("cpf", "52998224725"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("enrollBiometricByManager: endpoint removido /me/biometric-enrollment retorna 404")
    void shouldReturn404ForRemovedSelfEnrollmentEndpoint() throws Exception {
        UUID employeeId = UUID.randomUUID();
        String validBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

        mockMvc.perform(post("/employee/me/biometric-enrollment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "faceImageBase64": "%s",
                                  "employeeId": "%s",
                                  "livenessPassed": true
                                }
                                """.formatted(validBase64, employeeId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("enrollBiometricByManager: manager pode cadastrar biometria de colaborador")
    @WithMockUser(roles = "MANAGER")
    void shouldEnrollBiometricByManager() throws Exception {
        UUID targetEmployeeId = UUID.randomUUID();
        String validBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

        mockMvc.perform(post("/employee/manager/{employeeId}/biometric-enrollment", targetEmployeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "faceImageBase64": "%s",
                                  "employeeId": "%s",
                                  "livenessPassed": true
                                }
                                """.formatted(validBase64, targetEmployeeId)))
                .andExpect(status().isNoContent());

        verify(useCase).enrollBiometricByManager(eq(targetEmployeeId), any());
    }


    @Test
    @DisplayName("allEmployees: não deve expor salary, email, phone, address em listagem (SPEC-002)")
    void shouldNotExposeSensitiveDataInListResponse() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        var employee = employee(employeeId, companyId);

        when(useCase.listEmployees(null)).thenReturn(List.of(employee));
        when(companyUseCase.getCompanyNameById(companyId)).thenReturn("Kronos Tech");

        mockMvc.perform(get("/employee"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employees[0].employeeId").value(employeeId.toString()))
                .andExpect(jsonPath("$.employees[0].fullName").value("Lucas Silva"))
                .andExpect(jsonPath("$.employees[0].jobPosition").value("Software Engineer"))
                .andExpect(jsonPath("$.employees[0].maskedCpf").value("529.***.725"))
                .andExpect(jsonPath("$.employees[0].companyName").value("Kronos Tech"))
                .andExpect(jsonPath("$.employees[0].active").value(true))
                .andExpect(jsonPath("$.employees[0].salary").doesNotExist())
                .andExpect(jsonPath("$.employees[0].email").doesNotExist())
                .andExpect(jsonPath("$.employees[0].phone").doesNotExist())
                .andExpect(jsonPath("$.employees[0].address").doesNotExist());
    }

    @Test
    @DisplayName("getEmployee: pode expor salary, email, phone, address apenas para gestor (SPEC-002)")
    void shouldExposeDetailedDataOnlyInDetailResponse() throws Exception {
        UUID companyId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        var employee = employee(employeeId, companyId);

        when(useCase.getEmployee(employeeId)).thenReturn(employee);
        when(companyUseCase.getCompanyNameById(companyId)).thenReturn("Kronos Tech");

        mockMvc.perform(get("/employee/{employeeId}", employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(employeeId.toString()))
                .andExpect(jsonPath("$.fullName").value("Lucas Silva"))
                .andExpect(jsonPath("$.maskedCpf").value("529.***.725"))
                .andExpect(jsonPath("$.jobPosition").value("Software Engineer"))
                .andExpect(jsonPath("$.email").value("lucas@kronos.com"))
                .andExpect(jsonPath("$.salary").value(6500.00))
                .andExpect(jsonPath("$.phone").value("21999999999"))
                .andExpect(jsonPath("$.address").exists());
    }

    private Employee employee(UUID employeeId, UUID companyId) {
        return new Employee(
                employeeId,
                "Lucas Silva",
                "52998224725",
                "12345678901",
                "Software Engineer",
                "lucas@kronos.com",
                6500.00,
                "21999999999",
                true,
                new Address("Rua A", "100", "12345678", "Rio de Janeiro", "RJ"),
                companyId,
                LocalDateTime.of(2026, 1, 10, 8, 0),
                false,
                null,
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0),
                WorkScheduleType.TRADITIONAL_5X2,
                LocalDate.of(2026, 1, 1),
                DayOfWeek.SUNDAY,
                1,
                Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        );
    }
}
