package com.kts.kronos.application.service.anonymization;

import com.kts.kronos.application.security.PrivacyLogReferenceService;
import com.kts.kronos.adapter.out.persistence.EmployeeRepository;
import com.kts.kronos.adapter.out.persistence.entity.AddressEmbeddable;
import com.kts.kronos.adapter.out.persistence.entity.EmployeeEntity;
import com.kts.kronos.domain.model.AnonymizationPlan;
import com.kts.kronos.domain.model.enuns.AnonymizationResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeAnonymizerTest {


    @Mock
    private PrivacyLogReferenceService privacyLogReferenceService;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeAnonymizer anonymizer;

    @Test
    void testSupports() {
        assertEquals(AnonymizationResourceType.EMPLOYEE, anonymizer.supports());
    }

    @Test
    void testExecuteDryRunWithNoEmployee() {
        when(employeeRepository.findById(any())).thenReturn(Optional.empty());

        var plan = createPlan();
        var result = anonymizer.execute(plan, "DRY_RUN");

        assertEquals("DRY_RUN", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(0, result.scannedCount());
        assertEquals(0, result.affectedCount());
    }

    @Test
    void testExecuteDryRunWithEmployee() {
        var employee = createEmployee();
        when(employeeRepository.findById(any())).thenReturn(Optional.of(employee));

        var plan = createPlan();
        var result = anonymizer.execute(plan, "DRY_RUN");

        assertEquals("DRY_RUN", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(1, result.scannedCount());
        assertEquals(0, result.affectedCount());
    }

    @Test
    void testExecuteApplyAnonymizesCpf() {
        var employee = createEmployee();
        when(employeeRepository.findById(any())).thenReturn(Optional.of(employee));

        var plan = createPlan();
        var result = anonymizer.execute(plan, "APPLY");

        assertEquals("APPLY", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(1, result.affectedCount());

        verify(employeeRepository, times(1)).save(any());
    }

    @Test
    void testExecuteApplyAnonymizesEmail() {
        var employee = createEmployee();
        when(employeeRepository.findById(any())).thenReturn(Optional.of(employee));

        anonymizer.execute(createPlan(), "APPLY");

        var savedCaptor = org.mockito.ArgumentCaptor.forClass(EmployeeEntity.class);
        verify(employeeRepository).save(savedCaptor.capture());

        var saved = savedCaptor.getValue();
        assertTrue(saved.getEmail().startsWith("anon_"));
        assertTrue(saved.getEmail().endsWith("@employee.local"));
    }

    @Test
    void testExecuteApplyAnonymizesPhone() {
        var employee = createEmployee();
        employee.setPhone("123456789");
        when(employeeRepository.findById(any())).thenReturn(Optional.of(employee));

        anonymizer.execute(createPlan(), "APPLY");

        var savedCaptor = org.mockito.ArgumentCaptor.forClass(EmployeeEntity.class);
        verify(employeeRepository).save(savedCaptor.capture());

        var saved = savedCaptor.getValue();
        assertNull(saved.getPhone());
    }

    @Test
    void testExecuteApplyAnonymizesFullName() {
        var employee = createEmployee();
        when(employeeRepository.findById(any())).thenReturn(Optional.of(employee));

        anonymizer.execute(createPlan(), "APPLY");

        var savedCaptor = org.mockito.ArgumentCaptor.forClass(EmployeeEntity.class);
        verify(employeeRepository).save(savedCaptor.capture());

        var saved = savedCaptor.getValue();
        assertEquals("ANON", saved.getFullName());
    }

    @Test
    void testExecuteApplyAnonymizesAddress() {
        var address = new AddressEmbeddable("Rua Test", "123", "12345678", "São Paulo", "SP");
        var employee = createEmployee();
        employee.setAddress(address);
        when(employeeRepository.findById(any())).thenReturn(Optional.of(employee));

        anonymizer.execute(createPlan(), "APPLY");

        var savedCaptor = org.mockito.ArgumentCaptor.forClass(EmployeeEntity.class);
        verify(employeeRepository).save(savedCaptor.capture());

        var saved = savedCaptor.getValue();
        assertEquals("ANON", saved.getAddress().getStreet());
        assertNull(saved.getAddress().getNumber());
        assertEquals("ANON", saved.getAddress().getCity());
    }

    @Test
    void testExecuteApplyHandlesException() {
        when(employeeRepository.findById(any())).thenThrow(new RuntimeException("DB error"));

        var plan = createPlan();
        var result = anonymizer.execute(plan, "APPLY");

        assertEquals("APPLY", result.executionMode());
        assertEquals("ERROR", result.status());
        assertEquals(1, result.errorCount());
        assertTrue(result.notes().contains("DB error"));
    }

    @Test
    void testExecuteDryRunHandlesException() {
        when(employeeRepository.findById(any())).thenThrow(new RuntimeException("Query failed"));

        var plan = createPlan();
        var result = anonymizer.execute(plan, "DRY_RUN");

        assertEquals("DRY_RUN", result.executionMode());
        assertEquals("ERROR", result.status());
        assertEquals(1, result.errorCount());
    }

    private AnonymizationPlan createPlan() {
        return new AnonymizationPlan(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Test anonymization",
                false,
                false,
                false,
                false,
                false,
                false
        );
    }

    private EmployeeEntity createEmployee() {
        return EmployeeEntity.builder()
                .employeeId(UUID.randomUUID())
                .fullName("John Doe")
                .cpf("12345678901")
                .pis("12345678901")
                .email("john@example.com")
                .phone("123456789")
                .jobPosition("Developer")
                .salary(5000.0)
                .active(true)
                .companyId(UUID.randomUUID())
                .build();
    }
}
