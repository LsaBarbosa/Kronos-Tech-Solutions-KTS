package com.kts.kronos.adapter.in.web.http;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.kts.kronos.adapter.in.web.dto.address.AddressRequest;
import com.kts.kronos.adapter.in.web.dto.employee.CreateEmployeeRequest;
import com.kts.kronos.adapter.in.web.dto.employee.EmployeeListResponse;
import com.kts.kronos.adapter.in.web.dto.employee.EmployeeResponse;
import com.kts.kronos.adapter.in.web.dto.employee.UpdateEmployeeManagerRequest;
import com.kts.kronos.adapter.in.web.dto.employee.UpdateEmployeePartnerRequest;
import com.kts.kronos.application.port.in.usecase.EmployeeUseCase;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.kts.kronos.constants.ApiPaths.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
@DisplayName("EmployeeController Unit Tests")
class EmployeeControllerTest {


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmployeeUseCase useCase;

    private UUID employeeId;
    private Employee employee;
    private EmployeeResponse employeeResponse;
    private final String managerRole = "MANAGER";
    private final String partnerRole = "PARTNER";

    @BeforeEach
    void setUp() {
        employeeId = UUID.randomUUID();
        Address address = new Address("Test Street", "123", "12345678", "Test City", "Test State");
        employee = new Employee(employeeId, "Test Employee", "12345678901", "Developer", "test@test.com", 5000.0, "11999999999", true, address, UUID.randomUUID());
        employeeResponse = EmployeeResponse.fromDomain(employee);
    }

    // MANAGER ENDPOINTS
    // --------------------------------------------------

    @Nested
    @DisplayName("Manager Operations")
    class ManagerTests {

        @Test
        @DisplayName("POST /employee - Should register a new employee and return 201 Created")
        void registerEmployee_Success() throws Exception {
            CreateEmployeeRequest request = new CreateEmployeeRequest(
                    "New Employee",
                    "98765432101",
                    "Analyst",
                    "new@email.com",
                    4000.0,
                    "11888888888",
                    new AddressRequest("87654321", "456")
            );
            doNothing().when(useCase).createEmployee(any(CreateEmployeeRequest.class));

            mockMvc.perform(post(EMPLOYEE)
                            .with(user("managerUser").roles(managerRole))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("GET /employee - Should return a list of all employees and 200 OK")
        void allEmployees_Success() throws Exception {
            when(useCase.listEmployees(any())).thenReturn(Collections.singletonList(employee));
            EmployeeListResponse expectedResponse = new EmployeeListResponse(Collections.singletonList(employeeResponse));

            mockMvc.perform(get(EMPLOYEE)
                            .with(user("managerUser").roles(managerRole))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().json(objectMapper.writeValueAsString(expectedResponse)));
        }

        @Test
        @DisplayName("GET /employee/{employeeId} - Should return an employee by ID and 200 OK")
        void getEmployee_Success() throws Exception {
            when(useCase.getEmployee(employeeId)).thenReturn(employee);

            mockMvc.perform(get(EMPLOYEE + EMPLOYEE_ID, employeeId)
                            .with(user("managerUser").roles(managerRole))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().json(objectMapper.writeValueAsString(employeeResponse)));
        }

        @Test
        @DisplayName("PATCH /employee/manager/update-employee/{employeeId} - Should update an employee and return 200 OK")
        void updateEmployee_Success() throws Exception {
            UpdateEmployeeManagerRequest request = new UpdateEmployeeManagerRequest(
                    "Updated Name",
                    null,
                    "Senior Developer",
                    null,
                    6000.0,
                    null,
                    null
            );
            doNothing().when(useCase).updateEmployee(eq(employeeId), any(UpdateEmployeeManagerRequest.class));

            mockMvc.perform(patch(EMPLOYEE + UPDATE_EMPLOYEE, employeeId)
                            .with(user("managerUser").roles(managerRole))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("DELETE /employee/{employeeId} - Should delete an employee and return 204 No Content")
        void deleteEmployee_Success() throws Exception {
            doNothing().when(useCase).deleteEmployee(employeeId);

            mockMvc.perform(delete(EMPLOYEE + EMPLOYEE_ID, employeeId)
                            .with(user("managerUser").roles(managerRole))
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }
    }
    // PARTNER ENDPOINTS
    // --------------------------------------------------

    @Nested
    @DisplayName("Partner Operations")
    class PartnerTests {

        @Test
        @DisplayName("GET /employee/own-profile - Should return own profile and 200 OK")
        void getOwnProfile_Success() throws Exception {
            when(useCase.getOwnProfile()).thenReturn(employee);

            mockMvc.perform(get(EMPLOYEE + OWN_PROFILE)
                            .with(user("partnerUser").roles(partnerRole))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().json(objectMapper.writeValueAsString(employeeResponse)));
        }

        @Test
        @DisplayName("PATCH /employee/update-own-profile - Should update own profile and return 200 OK")
        void updateOwnProfile_Success() throws Exception {
            UpdateEmployeePartnerRequest request = new UpdateEmployeePartnerRequest(
                    "updated_partner@email.com",
                    "11988888888",
                    null
            );
            doNothing().when(useCase).updateOwnProfile(any(UpdateEmployeePartnerRequest.class));

            mockMvc.perform(patch(EMPLOYEE + UPDATE_OWN_PROFILE)
                            .with(user("partnerUser").roles(partnerRole))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
    }
}