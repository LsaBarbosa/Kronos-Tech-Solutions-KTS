package com.kts.kronos.application.service;

import com.kts.kronos.adapter.out.persistence.AnonymizationConsolidatedResultRepository;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.LegalConsentProvider;
import com.kts.kronos.application.port.out.provider.LgpdRequestHistoryProvider;
import com.kts.kronos.application.port.out.provider.LgpdRequestProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.LgpdRequest;
import com.kts.kronos.domain.model.enuns.ConsentType;
import com.kts.kronos.domain.model.enuns.LgpdRequestStatus;
import com.kts.kronos.domain.model.enuns.LgpdRequestType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LgpdServiceConsentRevocationTest {
    @Mock
    private LgpdRequestProvider lgpdRequestProvider;

    @Mock
    private LgpdRequestHistoryProvider lgpdRequestHistoryProvider;

    @Mock
    private DomainAuthorizationService domainAuthorizationService;

    @Mock
    private JwtAuthenticatedUser jwtAuthenticatedUser;

    @Mock
    private LegalConsentProvider legalConsentProvider;

    @Mock
    private AuditService auditService;

    @Mock
    private AuditRequestContextService auditRequestContextService;

    @Mock
    private EmployeeProvider employeeProvider;

    @InjectMocks
    private LgpdService lgpdService;

    private UUID requestId;
    private UUID employeeId;
    private UUID companyId;
    private UUID userId;
    private LgpdRequest consentRevocationRequest;

    @BeforeEach
    void setUp() {
        requestId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        userId = UUID.randomUUID();

        Instant now = Instant.now();

        consentRevocationRequest = new LgpdRequest(
                requestId,
                employeeId,
                userId,
                companyId,
                LgpdRequestType.CONSENT_REVOCATION,
                LgpdRequestStatus.WAITING_LEGAL_REVIEW,
                "Revogar consentimento biométrico",
                null,
                now,
                now,
                null,
                null,
                null,
                now.plusSeconds(86400 * 30),
                "MEDIUM",
                null,
                null,
                null,
                ConsentType.BIOMETRIC_AUTHENTICATION,
                null,
                false
        );
    }

    @Test
    void assignRequest_preservesConsentRevocationFields() {
        UUID newAssignedToUserId = UUID.randomUUID();
        LgpdRequest assignedRequest = consentRevocationRequest.withAssignment(newAssignedToUserId, Instant.now());

        // Target consent type should be preserved
        assertEquals(ConsentType.BIOMETRIC_AUTHENTICATION, assignedRequest.targetConsentType());
        assertEquals(newAssignedToUserId, assignedRequest.assignedToUserId());
        assertEquals(LgpdRequestStatus.WAITING_LEGAL_REVIEW, assignedRequest.status());
    }

    @Test
    void addNote_preservesConsentRevocationFields() {
        String newInternalNotes = "Esperando aprovação da equipe jurídica";
        LgpdRequest updatedRequest = consentRevocationRequest.withInternalNotes(newInternalNotes, Instant.now());

        // Target consent type should be preserved
        assertEquals(ConsentType.BIOMETRIC_AUTHENTICATION, updatedRequest.targetConsentType());
        assertEquals(newInternalNotes, updatedRequest.internalNotes());
        assertEquals(LgpdRequestStatus.WAITING_LEGAL_REVIEW, updatedRequest.status());
    }

    @Test
    void transitionStatus_preservesConsentRevocationFields() {
        LgpdRequest transitionedRequest = consentRevocationRequest.withStatus(
                LgpdRequestStatus.APPROVED_FOR_EXPORT,
                null,
                "Aprovado para execução",
                "Pendente execução técnica",
                Instant.now()
        );

        // Target consent type should be preserved through status transition
        assertEquals(ConsentType.BIOMETRIC_AUTHENTICATION, transitionedRequest.targetConsentType());
        assertEquals(LgpdRequestStatus.APPROVED_FOR_EXPORT, transitionedRequest.status());
    }

    @Test
    void consentRevocationExecution_marksExecutionData() {
        Instant executionTime = Instant.now();

        LgpdRequest executedRequest = consentRevocationRequest
                .markConsentRevocationExecuted(executionTime)
                .markConsentRevocationNoActiveConsent(true);

        // Consent revocation fields should be set
        assertEquals(executionTime, executedRequest.consentRevocationExecutedAt());
        assertTrue(executedRequest.consentRevocationNoActiveConsent());
        // Target consent type should be preserved
        assertEquals(ConsentType.BIOMETRIC_AUTHENTICATION, executedRequest.targetConsentType());
    }

    @Test
    void requestWithoutRevocationFields_doesNotBreakOnStatusUpdate() {
        UUID normalRequestId = UUID.randomUUID();
        Instant now = Instant.now();

        LgpdRequest normalRequest = new LgpdRequest(
                normalRequestId,
                employeeId,
                userId,
                companyId,
                LgpdRequestType.ANONYMIZATION,
                LgpdRequestStatus.WAITING_LEGAL_REVIEW,
                "Anonimizar dados",
                null,
                now,
                now,
                null,
                null,
                null,
                now.plusSeconds(86400 * 30),
                "MEDIUM",
                null,
                null,
                null,
                null,
                null,
                false
        );

        LgpdRequest updatedRequest = normalRequest.withStatus(
                LgpdRequestStatus.APPROVED_FOR_EXPORT,
                null,
                "Aprovado",
                null,
                now
        );

        // Should handle null fields gracefully
        assertNull(updatedRequest.targetConsentType());
        assertNull(updatedRequest.consentRevocationExecutedAt());
        assertFalse(updatedRequest.consentRevocationNoActiveConsent());
        assertEquals(LgpdRequestStatus.APPROVED_FOR_EXPORT, updatedRequest.status());
    }
}
