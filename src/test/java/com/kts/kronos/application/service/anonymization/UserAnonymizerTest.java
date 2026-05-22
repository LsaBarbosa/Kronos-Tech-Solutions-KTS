package com.kts.kronos.application.service.anonymization;

import com.kts.kronos.adapter.out.persistence.UserRepository;
import com.kts.kronos.adapter.out.persistence.entity.UserEntity;
import com.kts.kronos.domain.model.AnonymizationPlan;
import com.kts.kronos.domain.model.enuns.AnonymizationResourceType;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAnonymizerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserAnonymizer anonymizer;

    @Test
    void testSupports() {
        assertEquals(AnonymizationResourceType.USER, anonymizer.supports());
    }

    @Test
    void testExecuteDryRunWithNoUsers() {
        when(userRepository.findByEmployeeId(any())).thenReturn(Optional.empty());

        var plan = createPlan();
        var result = anonymizer.execute(plan, "DRY_RUN");

        assertEquals("DRY_RUN", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(0, result.scannedCount());
        assertEquals(0, result.affectedCount());
    }

    @Test
    void testExecuteDryRunWithUser() {
        var user = createUser();
        when(userRepository.findByEmployeeId(any())).thenReturn(Optional.of(user));

        var plan = createPlan();
        var result = anonymizer.execute(plan, "DRY_RUN");

        assertEquals("DRY_RUN", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(1, result.scannedCount());
        assertEquals(0, result.affectedCount());
    }

    @Test
    void testExecuteApplyAnonymizesUsername() {
        var user = createUser();
        when(userRepository.findByEmployeeId(any())).thenReturn(Optional.of(user));

        var plan = createPlan();
        var result = anonymizer.execute(plan, "APPLY");

        assertEquals("APPLY", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(1, result.affectedCount());

        verify(userRepository, times(1)).save(any());
    }

    @Test
    void testExecuteApplyUsernameFormat() {
        var user = createUser();
        when(userRepository.findByEmployeeId(any())).thenReturn(Optional.of(user));

        anonymizer.execute(createPlan(), "APPLY");

        var savedCaptor = org.mockito.ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(savedCaptor.capture());

        var saved = savedCaptor.getValue();
        assertTrue(saved.getUsername().startsWith("anon_"));
        assertEquals(13, saved.getUsername().length());
    }

    @Test
    void testExecuteApplyHandlesException() {
        when(userRepository.findByEmployeeId(any())).thenThrow(new RuntimeException("DB error"));

        var plan = createPlan();
        var result = anonymizer.execute(plan, "APPLY");

        assertEquals("APPLY", result.executionMode());
        assertEquals("ERROR", result.status());
        assertEquals(1, result.errorCount());
    }

    @Test
    void testExecuteDryRunHandlesException() {
        when(userRepository.findByEmployeeId(any())).thenThrow(new RuntimeException("Query failed"));

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

    private UserEntity createUser() {
        return UserEntity.builder()
                .userId(UUID.randomUUID())
                .username("testuser")
                .password("hashedpassword")
                .role(Role.CTO)
                .active(true)
                .employeeId(UUID.randomUUID())
                .build();
    }
}
