package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.employee.EmployeeProfile;
import com.kts.kronos.adapter.in.web.exceptions.RestExceptionHandler;
import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
import com.kts.kronos.application.port.in.usecase.EmployeeUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployeeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RestExceptionHandler.class)
class EmployeeControllerWebMvcTest {

    @Resource
    private MockMvc mockMvc;

    @MockitoBean
    private EmployeeUseCase useCase;

    @MockitoBean
    private CompanyUseCase companyUseCase;

    @Test
    void shouldRegisterEmployeeSuccessfully() throws Exception {
        // implementar
    }

    @Test
    void shouldListEmployeesWithoutFilter() throws Exception {
        // implementar
    }

    @Test
    void shouldListEmployeesWithActiveFilter() throws Exception {
        // implementar
    }

    @Test
    void shouldGetEmployeeById() throws Exception {
        // implementar
    }

    @Test
    void shouldUpdateEmployee() throws Exception {
        // implementar
    }

    @Test
    void shouldGetOwnProfile() throws Exception {
        // implementar
    }

    @Test
    void shouldUpdateOwnProfile() throws Exception {
        // implementar
    }

    @Test
    void shouldDeleteEmployee() throws Exception {
        // implementar
    }

    @Test
    void shouldMarkMessagesAsSeen() throws Exception {
        // implementar
    }

    @Test
    void shouldReturnOkWhenCpfExists() throws Exception {
        // implementar
    }

    @Test
    void shouldReturnNotFoundWhenCpfDoesNotExist() throws Exception {
        // implementar
    }

    @Test
    void shouldReturnBadRequestWhenCreatePayloadIsInvalid() throws Exception {
        // implementar
    }

    @Test
    void shouldReturnBadRequestWhenUpdateOwnProfilePayloadIsInvalid() throws Exception {
        // implementar
    }
}