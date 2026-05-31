package com.kts.kronos.application.service;

import com.kts.kronos.application.security.PrivacyLogReferenceService;
import com.kts.kronos.adapter.out.persistence.AnonymizationConsolidatedResultRepository;
import com.kts.kronos.adapter.out.persistence.entity.AnonymizationConsolidatedResultEntity;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.LgpdRequestHistoryProvider;
import com.kts.kronos.application.port.out.provider.LgpdRequestProvider;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.application.service.anonymization.AnonymizationPlanExecutor;
import com.kts.kronos.application.service.LgpdRequestNotificationService;
import com.kts.kronos.application.service.DryRunTokenService;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.AnonymizationConsolidatedResult;
import com.kts.kronos.domain.model.AnonymizationExecutionResult;
import com.kts.kronos.domain.model.DryRunToken;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.LgpdRequest;
import com.kts.kronos.domain.model.enuns.AnonymizationConsolidatedStatus;
import com.kts.kronos.domain.model.enuns.LgpdRequestStatus;
import com.kts.kronos.domain.model.enuns.LgpdRequestType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LgpdServiceAnonymizationTest {


    @Mock
    private PrivacyLogReferenceService privacyLogReferenceService;

    @InjectMocks
    private LgpdService service;

    @Mock
    private LgpdRequestProvider lgpdRequestProvider;

    @Mock
    private LgpdRequestHistoryProvider lgpdRequestHistoryProvider;

    @Mock
    private DomainAuthorizationService domainAuthorizationService;

    @Mock
    private JwtAuthenticatedUser jwtAuthenticatedUser;

    @Mock
    private EmployeeProvider employeeProvider;

    @Mock
    private AnonymizationPlanExecutor anonymizationPlanExecutor;

    @Mock
    private AnonymizationConsolidatedResultRepository anonymizationConsolidatedResultRepository;

    @Mock
    private LgpdRequestNotificationService notificationService;

    @Mock
    private AuditRequestContextService auditRequestContextService;

    @Mock
    private AuditService auditService;

    @Mock
    private DryRunTokenService dryRunTokenService;

    @BeforeEach
    void setUp() {
        var mockContext = new AuditRequestContextService.AuditRequestContext("127.0.0.1", "Test-Agent/1.0", "LOCAL", false);
        lenient().when(auditRequestContextService.extractContext()).thenReturn(mockContext);
    }

    @Test
    void shouldExecuteAnonymizationForAnonymizationRequest() {
        UUID requestId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();

        LgpdRequest request = new LgpdRequest(
                requestId,
                employeeId,
                UUID.randomUUID(),
                companyId,
                LgpdRequestType.ANONYMIZATION,
                LgpdRequestStatus.IN_ANALYSIS,
                "Anonimizar dados",
                null,
                Instant.now(),
                Instant.now(),
                null,
                null,
                null,
                Instant.now().plusSeconds(86400 * 15),
                "NORMAL",
                null,
                null,
                null
        );

        Employee employee = buildEmployee(employeeId, companyId);
        AnonymizationConsolidatedResult consolidatedResult = new AnonymizationConsolidatedResult(
                UUID.randomUUID(),
                employeeId,
                companyId,
                actorUserId,
                AnonymizationConsolidatedStatus.SUCCESS,
                "APPLY",
                Instant.now(),
                Instant.now(),
                100L,
                50L,
                10L,
                0L,
                null,
                List.of(),
                List.of()
        );

        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(domainAuthorizationService.authorizeCompanyAccess(companyId)).thenReturn(companyId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(actorUserId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(anonymizationPlanExecutor.executePlanWithConsolidatedResult(any(), any())).thenReturn(consolidatedResult);
        when(anonymizationConsolidatedResultRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AnonymizationConsolidatedResult result = service.executeAnonymizationForRequest(requestId);

        assertNotNull(result);
        assertEquals(AnonymizationConsolidatedStatus.SUCCESS, result.consolidatedStatus());
        assertEquals(employeeId, result.employeeId());
        verify(anonymizationPlanExecutor).executePlanWithConsolidatedResult(any(), any());
        verify(anonymizationConsolidatedResultRepository).save(any());
    }

    @Test
    void shouldExecuteAnonymizationForDeletionRequest() {
        UUID requestId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();

        LgpdRequest request = new LgpdRequest(
                requestId,
                employeeId,
                UUID.randomUUID(),
                companyId,
                LgpdRequestType.DELETION,
                LgpdRequestStatus.IN_ANALYSIS,
                "Deletar dados",
                null,
                Instant.now(),
                Instant.now(),
                null,
                null,
                null,
                Instant.now().plusSeconds(86400 * 15),
                "NORMAL",
                null,
                null,
                null
        );

        Employee employee = buildEmployee(employeeId, companyId);
        AnonymizationConsolidatedResult consolidatedResult = new AnonymizationConsolidatedResult(
                UUID.randomUUID(),
                employeeId,
                companyId,
                actorUserId,
                AnonymizationConsolidatedStatus.PARTIAL_SUCCESS,
                "APPLY",
                Instant.now(),
                Instant.now(),
                100L,
                80L,
                10L,
                5L,
                null,
                List.of("MESSAGE"),
                List.of("Message anonymization failed")
        );

        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(domainAuthorizationService.authorizeCompanyAccess(companyId)).thenReturn(companyId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(actorUserId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(anonymizationPlanExecutor.executePlanWithConsolidatedResult(any(), any())).thenReturn(consolidatedResult);
        when(anonymizationConsolidatedResultRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AnonymizationConsolidatedResult result = service.executeAnonymizationForRequest(requestId);

        assertNotNull(result);
        assertEquals(AnonymizationConsolidatedStatus.PARTIAL_SUCCESS, result.consolidatedStatus());
        assertTrue(result.failedDomains().contains("MESSAGE"));
        verify(anonymizationConsolidatedResultRepository).save(any(AnonymizationConsolidatedResultEntity.class));
    }

    @Test
    void shouldThrowErrorWhenExecutingAnonymizationForNonAnonymizationRequest() {
        UUID requestId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        LgpdRequest request = new LgpdRequest(
                requestId,
                employeeId,
                UUID.randomUUID(),
                companyId,
                LgpdRequestType.ACCESS,
                LgpdRequestStatus.IN_ANALYSIS,
                "Exportar dados",
                null,
                Instant.now(),
                Instant.now(),
                null,
                null,
                null,
                Instant.now().plusSeconds(86400 * 15),
                "NORMAL",
                null,
                null,
                null
        );

        Employee employee = buildEmployee(employeeId, companyId);

        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(domainAuthorizationService.authorizeCompanyAccess(companyId)).thenReturn(companyId);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.executeAnonymizationForRequest(requestId)
        );

        assertTrue(exception.getMessage().contains("ANONYMIZATION ou DELETION"));
    }

    @Test
    void shouldNotExecuteAnonymizationForAdditionalArt18RequestTypes() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = buildEmployee(employeeId, companyId);

        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(domainAuthorizationService.authorizeCompanyAccess(companyId)).thenReturn(companyId);

        for (LgpdRequestType type : List.of(
                LgpdRequestType.CONSENT_INFORMATION,
                LgpdRequestType.OPPOSITION,
                LgpdRequestType.AUTOMATED_DECISION_REVIEW
        )) {
            UUID requestId = UUID.randomUUID();
            LgpdRequest request = new LgpdRequest(
                    requestId,
                    employeeId,
                    UUID.randomUUID(),
                    companyId,
                    type,
                    LgpdRequestStatus.APPROVED_FOR_EXPORT,
                    "Direito do titular",
                    null,
                    Instant.now(),
                    Instant.now(),
                    null,
                    null,
                    null,
                    Instant.now().plusSeconds(86400 * 15),
                    "NORMAL",
                    null,
                    null,
                    null
            );

            when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));

            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.executeAnonymizationForRequest(requestId)
            );

            assertTrue(exception.getMessage().contains("ANONYMIZATION ou DELETION"));
            assertTrue(exception.getMessage().contains(type.name()));
        }
    }

    @Test
    void shouldRetrieveAnonymizationResult() {
        UUID requestId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID consolidatedExecutionId = UUID.randomUUID();

        LgpdRequest request = new LgpdRequest(
                requestId,
                employeeId,
                UUID.randomUUID(),
                companyId,
                LgpdRequestType.ANONYMIZATION,
                LgpdRequestStatus.IN_ANALYSIS,
                "Anonimizar",
                null,
                Instant.now(),
                Instant.now(),
                null,
                null,
                null,
                Instant.now().plusSeconds(86400 * 15),
                "NORMAL",
                null,
                null,
                null
        );

        AnonymizationConsolidatedResultEntity entity = new AnonymizationConsolidatedResultEntity(
                consolidatedExecutionId,
                requestId,
                employeeId,
                companyId,
                actorUserId,
                AnonymizationConsolidatedStatus.SUCCESS,
                "APPLY",
                100L,
                50L,
                10L,
                0L,
                null,
                null,
                Instant.now(),
                Instant.now(),
                Instant.now()
        );

        Employee employee = buildEmployee(employeeId, companyId);

        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(domainAuthorizationService.authorizeCompanyAccess(companyId)).thenReturn(companyId);
        when(anonymizationConsolidatedResultRepository.findByRequestId(requestId)).thenReturn(Optional.of(entity));

        AnonymizationConsolidatedResult result = service.getAnonymizationResult(requestId);

        assertNotNull(result);
        assertEquals(consolidatedExecutionId, result.consolidatedExecutionId());
        assertEquals(AnonymizationConsolidatedStatus.SUCCESS, result.consolidatedStatus());
    }

    @Test
    void shouldReturnNullWhenAnonymizationResultNotFound() {
        UUID requestId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        LgpdRequest request = new LgpdRequest(
                requestId,
                employeeId,
                UUID.randomUUID(),
                companyId,
                LgpdRequestType.ANONYMIZATION,
                LgpdRequestStatus.IN_ANALYSIS,
                "Anonimizar",
                null,
                Instant.now(),
                Instant.now(),
                null,
                null,
                null,
                Instant.now().plusSeconds(86400 * 15),
                "NORMAL",
                null,
                null,
                null
        );

        Employee employee = buildEmployee(employeeId, companyId);

        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(domainAuthorizationService.authorizeCompanyAccess(companyId)).thenReturn(companyId);
        when(anonymizationConsolidatedResultRepository.findByRequestId(requestId)).thenReturn(Optional.empty());

        AnonymizationConsolidatedResult result = service.getAnonymizationResult(requestId);

        assertNull(result);
    }

    @Test
    void shouldBlockConclusionWhenNoAnonymizationResult() {
        UUID requestId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        LgpdRequest request = new LgpdRequest(
                requestId,
                employeeId,
                UUID.randomUUID(),
                companyId,
                LgpdRequestType.ANONYMIZATION,
                LgpdRequestStatus.WAITING_LEGAL_REVIEW,
                "Anonimizar",
                null,
                Instant.now(),
                Instant.now(),
                null,
                null,
                null,
                Instant.now().plusSeconds(86400 * 15),
                "NORMAL",
                null,
                null,
                null
        );

        Employee employee = buildEmployee(employeeId, companyId);

        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(domainAuthorizationService.authorizeCompanyAccess(companyId)).thenReturn(companyId);
        when(anonymizationConsolidatedResultRepository.findByRequestId(requestId)).thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.transitionStatus(requestId, LgpdRequestStatus.COMPLETED, "Concluído", null, null)
        );

        assertTrue(exception.getMessage().contains("Não é possível concluir requisição sem execução"));
    }

    @Test
    void shouldBlockConclusionWhenAnonymizationFailed() {
        UUID requestId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID consolidatedExecutionId = UUID.randomUUID();

        LgpdRequest request = new LgpdRequest(
                requestId,
                employeeId,
                UUID.randomUUID(),
                companyId,
                LgpdRequestType.ANONYMIZATION,
                LgpdRequestStatus.WAITING_LEGAL_REVIEW,
                "Anonimizar",
                null,
                Instant.now(),
                Instant.now(),
                null,
                null,
                null,
                Instant.now().plusSeconds(86400 * 15),
                "NORMAL",
                null,
                null,
                null
        );

        AnonymizationConsolidatedResultEntity entity = new AnonymizationConsolidatedResultEntity(
                consolidatedExecutionId,
                requestId,
                employeeId,
                companyId,
                UUID.randomUUID(),
                AnonymizationConsolidatedStatus.FAILED,
                "APPLY",
                100L,
                0L,
                0L,
                100L,
                "EMPLOYEE,USER",
                "Employee anonymization failed\nUser anonymization failed",
                Instant.now(),
                Instant.now(),
                Instant.now()
        );

        Employee employee = buildEmployee(employeeId, companyId);

        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(domainAuthorizationService.authorizeCompanyAccess(companyId)).thenReturn(companyId);
        when(anonymizationConsolidatedResultRepository.findByRequestId(requestId)).thenReturn(Optional.of(entity));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.transitionStatus(requestId, LgpdRequestStatus.COMPLETED, "Concluído", null, null)
        );

        assertTrue(exception.getMessage().contains("status FAILED"));
    }

    @Test
    void shouldBlockCompletedWhenPartialSuccessButAllowPartiallyCompleted() {
        UUID requestId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID consolidatedExecutionId = UUID.randomUUID();

        LgpdRequest request = new LgpdRequest(
                requestId,
                employeeId,
                UUID.randomUUID(),
                companyId,
                LgpdRequestType.ANONYMIZATION,
                LgpdRequestStatus.WAITING_LEGAL_REVIEW,
                "Anonimizar",
                null,
                Instant.now(),
                Instant.now(),
                null,
                null,
                null,
                Instant.now().plusSeconds(86400 * 15),
                "NORMAL",
                null,
                null,
                null
        );

        AnonymizationConsolidatedResultEntity entity = new AnonymizationConsolidatedResultEntity(
                consolidatedExecutionId,
                requestId,
                employeeId,
                companyId,
                UUID.randomUUID(),
                AnonymizationConsolidatedStatus.PARTIAL_SUCCESS,
                "APPLY",
                100L,
                75L,
                10L,
                5L,
                "MESSAGE",
                "Message anonymization failed",
                Instant.now(),
                Instant.now(),
                Instant.now()
        );

        Employee employee = buildEmployee(employeeId, companyId);

        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(domainAuthorizationService.authorizeCompanyAccess(companyId)).thenReturn(companyId);
        when(anonymizationConsolidatedResultRepository.findByRequestId(requestId)).thenReturn(Optional.of(entity));

        // Should throw for COMPLETED
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.transitionStatus(requestId, LgpdRequestStatus.COMPLETED, "Parcialmente concluído", null, null)
        );

        assertTrue(exception.getMessage().contains("PARTIAL_SUCCESS permite apenas conclusão parcial"));
    }

    @Test
    void shouldAllowPartiallyCompletedWhenPartialSuccess() {
        UUID requestId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID consolidatedExecutionId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();

        LgpdRequest request = new LgpdRequest(
                requestId,
                employeeId,
                UUID.randomUUID(),
                companyId,
                LgpdRequestType.ANONYMIZATION,
                LgpdRequestStatus.WAITING_LEGAL_REVIEW,
                "Anonimizar",
                null,
                Instant.now(),
                Instant.now(),
                null,
                null,
                null,
                Instant.now().plusSeconds(86400 * 15),
                "NORMAL",
                null,
                null,
                null
        );

        AnonymizationConsolidatedResultEntity entity = new AnonymizationConsolidatedResultEntity(
                consolidatedExecutionId,
                requestId,
                employeeId,
                companyId,
                actorUserId,
                AnonymizationConsolidatedStatus.PARTIAL_SUCCESS,
                "APPLY",
                100L,
                75L,
                10L,
                5L,
                "MESSAGE",
                "Message anonymization failed",
                Instant.now(),
                Instant.now(),
                Instant.now()
        );

        Employee employee = buildEmployee(employeeId, companyId);

        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(domainAuthorizationService.authorizeCompanyAccess(companyId)).thenReturn(companyId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(actorUserId);
        when(anonymizationConsolidatedResultRepository.findByRequestId(requestId)).thenReturn(Optional.of(entity));
        when(lgpdRequestProvider.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(lgpdRequestHistoryProvider.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Should not throw for PARTIALLY_COMPLETED
        LgpdRequest result = service.transitionStatus(
                requestId,
                LgpdRequestStatus.PARTIALLY_COMPLETED,
                "Parcialmente concluído",
                null,
                null
        );

        assertNotNull(result);
        assertEquals(LgpdRequestStatus.PARTIALLY_COMPLETED, result.status());
    }

    @Test
    void shouldAllowCompletedWhenSuccess() {
        UUID requestId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID consolidatedExecutionId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();

        LgpdRequest request = new LgpdRequest(
                requestId,
                employeeId,
                UUID.randomUUID(),
                companyId,
                LgpdRequestType.ANONYMIZATION,
                LgpdRequestStatus.WAITING_LEGAL_REVIEW,
                "Anonimizar",
                null,
                Instant.now(),
                Instant.now(),
                null,
                null,
                null,
                Instant.now().plusSeconds(86400 * 15),
                "NORMAL",
                null,
                null,
                null
        );

        AnonymizationConsolidatedResultEntity entity = new AnonymizationConsolidatedResultEntity(
                consolidatedExecutionId,
                requestId,
                employeeId,
                companyId,
                actorUserId,
                AnonymizationConsolidatedStatus.SUCCESS,
                "APPLY",
                100L,
                100L,
                0L,
                0L,
                null,
                null,
                Instant.now(),
                Instant.now(),
                Instant.now()
        );

        Employee employee = buildEmployee(employeeId, companyId);

        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(domainAuthorizationService.authorizeCompanyAccess(companyId)).thenReturn(companyId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(actorUserId);
        when(anonymizationConsolidatedResultRepository.findByRequestId(requestId)).thenReturn(Optional.of(entity));
        when(lgpdRequestProvider.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(lgpdRequestHistoryProvider.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LgpdRequest result = service.transitionStatus(
                requestId,
                LgpdRequestStatus.COMPLETED,
                "Concluído",
                null,
                null
        );

        assertNotNull(result);
        assertEquals(LgpdRequestStatus.COMPLETED, result.status());
    }

    @Test
    void shouldAllowDryRunForWaitingControllerStatus() {
        UUID requestId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();

        LgpdRequest request = new LgpdRequest(
                requestId,
                employeeId,
                UUID.randomUUID(),
                companyId,
                LgpdRequestType.ANONYMIZATION,
                LgpdRequestStatus.WAITING_CONTROLLER,
                "Anonimizar",
                null,
                Instant.now(),
                Instant.now(),
                null,
                null,
                null,
                Instant.now().plusSeconds(86400 * 15),
                "NORMAL",
                null,
                null,
                null
        );

        Employee employee = buildEmployee(employeeId, companyId);
        Instant now = Instant.now();

        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(domainAuthorizationService.authorizeCompanyAccess(companyId)).thenReturn(companyId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(actorUserId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(anonymizationPlanExecutor.executePlanWithResults(any(), any())).thenReturn(List.of(
                AnonymizationExecutionResult.success(
                        UUID.randomUUID(),
                        employeeId,
                        companyId,
                        actorUserId,
                        com.kts.kronos.domain.model.enuns.AnonymizationResourceType.TIME_RECORD,
                        "DRY_RUN",
                        100L,
                        50L,
                        10L
                )
        ));
        when(dryRunTokenService.generateToken(requestId, employeeId, companyId, actorUserId))
                .thenReturn(new DryRunToken(UUID.randomUUID(), requestId, UUID.randomUUID(), employeeId, companyId, actorUserId, now, now.plusSeconds(900), null, DryRunToken.Status.PENDING));

        com.kts.kronos.adapter.in.web.dto.lgpd.AnonymizationDryRunWithTokenResponse result =
                service.executeDryRunAnonymizationForRequest(requestId);

        assertNotNull(result);
        assertNotNull(result.dryRunToken());
    }

    @Test
    void shouldAllowDryRunForWaitingLegalReviewStatus() {
        UUID requestId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();

        LgpdRequest request = new LgpdRequest(
                requestId,
                employeeId,
                UUID.randomUUID(),
                companyId,
                LgpdRequestType.ANONYMIZATION,
                LgpdRequestStatus.WAITING_LEGAL_REVIEW,
                "Anonimizar",
                null,
                Instant.now(),
                Instant.now(),
                null,
                null,
                null,
                Instant.now().plusSeconds(86400 * 15),
                "NORMAL",
                null,
                null,
                null
        );

        Employee employee = buildEmployee(employeeId, companyId);
        Instant now = Instant.now();

        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(domainAuthorizationService.authorizeCompanyAccess(companyId)).thenReturn(companyId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(actorUserId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(anonymizationPlanExecutor.executePlanWithResults(any(), any())).thenReturn(List.of(
                AnonymizationExecutionResult.success(
                        UUID.randomUUID(),
                        employeeId,
                        companyId,
                        actorUserId,
                        com.kts.kronos.domain.model.enuns.AnonymizationResourceType.TIME_RECORD,
                        "DRY_RUN",
                        100L,
                        50L,
                        10L
                )
        ));
        when(dryRunTokenService.generateToken(requestId, employeeId, companyId, actorUserId))
                .thenReturn(new DryRunToken(UUID.randomUUID(), requestId, UUID.randomUUID(), employeeId, companyId, actorUserId, now, now.plusSeconds(900), null, DryRunToken.Status.PENDING));

        com.kts.kronos.adapter.in.web.dto.lgpd.AnonymizationDryRunWithTokenResponse result =
                service.executeDryRunAnonymizationForRequest(requestId);

        assertNotNull(result);
        assertNotNull(result.dryRunToken());
    }

    @Test
    void shouldBlockApplyForWaitingControllerStatus() {
        UUID requestId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();

        LgpdRequest request = new LgpdRequest(
                requestId,
                employeeId,
                UUID.randomUUID(),
                companyId,
                LgpdRequestType.ANONYMIZATION,
                LgpdRequestStatus.WAITING_CONTROLLER,
                "Anonimizar",
                null,
                Instant.now(),
                Instant.now(),
                null,
                null,
                null,
                Instant.now().plusSeconds(86400 * 15),
                "NORMAL",
                null,
                null,
                null
        );

        Employee employee = buildEmployee(employeeId, companyId);

        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(domainAuthorizationService.authorizeCompanyAccess(companyId)).thenReturn(companyId);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.applyAnonymizationForRequest(
                        requestId,
                        "Justificativa",
                        true,
                        UUID.randomUUID(),
                        "127.0.0.1",
                        "Test-Agent"
                )
        );

        assertTrue(exception.getMessage().contains("Aprovação formal é obrigatória"));
    }

    @Test
    void shouldBlockApplyForWaitingLegalReviewStatus() {
        UUID requestId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();

        LgpdRequest request = new LgpdRequest(
                requestId,
                employeeId,
                UUID.randomUUID(),
                companyId,
                LgpdRequestType.ANONYMIZATION,
                LgpdRequestStatus.WAITING_LEGAL_REVIEW,
                "Anonimizar",
                null,
                Instant.now(),
                Instant.now(),
                null,
                null,
                null,
                Instant.now().plusSeconds(86400 * 15),
                "NORMAL",
                null,
                null,
                null
        );

        Employee employee = buildEmployee(employeeId, companyId);

        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(domainAuthorizationService.authorizeCompanyAccess(companyId)).thenReturn(companyId);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.applyAnonymizationForRequest(
                        requestId,
                        "Justificativa",
                        true,
                        UUID.randomUUID(),
                        "127.0.0.1",
                        "Test-Agent"
                )
        );

        assertTrue(exception.getMessage().contains("Aprovação formal é obrigatória"));
    }

    @Test
    void shouldAllowApplyForApprovedForExportStatus() {
        UUID requestId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID dryRunTokenId = UUID.randomUUID();
        Instant now = Instant.now();

        LgpdRequest request = new LgpdRequest(
                requestId,
                employeeId,
                UUID.randomUUID(),
                companyId,
                LgpdRequestType.ANONYMIZATION,
                LgpdRequestStatus.APPROVED_FOR_EXPORT,
                "Anonimizar",
                null,
                now,
                now,
                null,
                null,
                null,
                now.plusSeconds(86400 * 15),
                "NORMAL",
                null,
                null,
                null
        );

        Employee employee = buildEmployee(employeeId, companyId);
        AnonymizationConsolidatedResult consolidatedResult = new AnonymizationConsolidatedResult(
                UUID.randomUUID(),
                employeeId,
                companyId,
                actorUserId,
                AnonymizationConsolidatedStatus.SUCCESS,
                "APPLY",
                now,
                now.plusSeconds(10),
                100L,
                50L,
                10L,
                0L,
                null,
                List.of(),
                List.of()
        );

        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(domainAuthorizationService.authorizeCompanyAccess(companyId)).thenReturn(companyId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(actorUserId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(anonymizationConsolidatedResultRepository.findByRequestId(requestId)).thenReturn(Optional.empty());
        when(dryRunTokenService.validateAndGetToken(dryRunTokenId))
                .thenReturn(new DryRunToken(dryRunTokenId, requestId, UUID.randomUUID(), employeeId, companyId, actorUserId, now, now.plusSeconds(900), null, DryRunToken.Status.PENDING));
        when(anonymizationPlanExecutor.executePlanWithConsolidatedResult(any(), any())).thenReturn(consolidatedResult);
        when(anonymizationConsolidatedResultRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AnonymizationConsolidatedResult result = service.applyAnonymizationForRequest(
                requestId,
                "Justificativa",
                true,
                dryRunTokenId,
                "127.0.0.1",
                "Test-Agent"
        );

        assertNotNull(result);
        assertEquals(AnonymizationConsolidatedStatus.SUCCESS, result.consolidatedStatus());
    }

    private Employee buildEmployee(UUID employeeId, UUID companyId) {
        return new Employee(
                employeeId,
                "Lucas",
                "12345678901",
                "98765432100",
                "Dev",
                "lucas@kts.com",
                5000.0,
                "11999999999",
                true,
                new Address("Rua A", "100", "01001000", "São Paulo", "SP"),
                companyId,
                java.time.LocalDateTime.now(),
                true,
                "face/object/key",
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0),
                null,
                null,
                null,
                null,
                Set.of()
        );
    }
}
