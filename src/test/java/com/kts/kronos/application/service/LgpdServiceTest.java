package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.adapter.in.web.dto.lgpd.CreateLgpdRequestRequest;
import com.kts.kronos.adapter.in.web.dto.lgpd.LgpdEmployeeExportResponse;
import com.kts.kronos.adapter.in.web.dto.lgpd.UpdateLgpdRequestStatusRequest;
import com.kts.kronos.adapter.out.persistence.AnonymizationConsolidatedResultRepository;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.LegalConsentProvider;
import com.kts.kronos.application.port.out.provider.LgpdRequestHistoryProvider;
import com.kts.kronos.application.port.out.provider.LgpdRequestProvider;
import com.kts.kronos.application.port.out.provider.MessageProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.application.service.anonymization.AnonymizationPlanExecutor;
import com.kts.kronos.application.service.LgpdRequestNotificationService;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.AuditLog;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Document;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.LegalConsent;
import com.kts.kronos.domain.model.LgpdRequest;
import com.kts.kronos.domain.model.LgpdRequestHistory;
import com.kts.kronos.domain.model.Message;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.AuditAction;
import com.kts.kronos.domain.model.enuns.ConsentType;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.LegalBasis;
import com.kts.kronos.domain.model.enuns.LgpdRequestStatus;
import com.kts.kronos.domain.model.enuns.LgpdRequestType;
import com.kts.kronos.domain.model.enuns.MessagePriority;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LgpdServiceTest {
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
    private UserProvider userProvider;
    @Mock
    private CompanyProvider companyProvider;
    @Mock
    private DocumentProvider documentProvider;
    @Mock
    private TimeRecordProvider timeRecordProvider;
    @Mock
    private MessageProvider messageProvider;
    @Mock
    private AuditService auditService;
    @Mock
    private LegalConsentProvider legalConsentProvider;
    @Mock
    private EmployeeAnonymizationService employeeAnonymizationService;
    @Mock
    private AnonymizationPlanExecutor anonymizationPlanExecutor;
    @Mock
    private AnonymizationConsolidatedResultRepository anonymizationConsolidatedResultRepository;
    @Mock
    private LgpdSlaPolicyService lgpdSlaPolicyService;
    @Mock
    private LgpdRequestNotificationService notificationService;

    @Test
    void shouldCreateLgpdRequestAndRegisterHistoryAndAudit() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = buildEmployee(employeeId, companyId);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(lgpdRequestProvider.save(any(LgpdRequest.class))).thenAnswer(invocation -> {
            LgpdRequest request = invocation.getArgument(0);
            return new LgpdRequest(
                    UUID.randomUUID(),
                    request.employeeId(),
                    request.requestedByUserId(),
                    request.companyId(),
                    request.requestType(),
                    request.status(),
                    request.description(),
                    request.resolutionNotes(),
                    request.createdAt(),
                    request.updatedAt(),
                    request.resolvedAt(),
                    request.resolvedByUserId(),
                    request.assignedToUserId(),
                    request.dueAt(),
                    request.priority(),
                    request.closedReason(),
                    request.publicResolutionNotes(),
                    request.internalNotes()
            );
        });
        when(lgpdRequestHistoryProvider.save(any(LgpdRequestHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LgpdRequest created = service.createRequest(
                new CreateLgpdRequestRequest(null, LgpdRequestType.ACCESS, "Exportar meus dados"),
                "127.0.0.1",
                "JUnit"
        );

        assertNotNull(created.requestId());
        assertEquals(LgpdRequestStatus.OPEN, created.status());
        assertEquals(LgpdRequestType.ACCESS, created.requestType());

        verify(lgpdRequestHistoryProvider).save(any(LgpdRequestHistory.class));
        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditService).registerLgpd(
                eq(AuditAction.LGPD_REQUEST_CREATED),
                eq(employeeId),
                eq(companyId),
                eq("LGPD_REQUEST"),
                eq(created.requestId().toString()),
                eq("MEDIUM"),
                detailsCaptor.capture(),
                eq("127.0.0.1"),
                eq("JUnit")
        );
        assertTrue(detailsCaptor.getValue().contains(created.requestId().toString()));
    }

    @Test
    void shouldListCompanyRequestsForManager() {
        UUID companyId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(domainAuthorizationService.authorizeCompanyAccess(null)).thenReturn(companyId);
        when(lgpdRequestProvider.findByCompanyId(companyId)).thenReturn(List.of(
                buildRequest(employeeId, companyId, LgpdRequestType.ACCESS, LgpdRequestStatus.OPEN),
                buildRequest(employeeId, companyId, LgpdRequestType.ANONYMIZATION, LgpdRequestStatus.COMPLETED)
        ));

        List<LgpdRequest> requests = service.listRequests(null, null, LgpdRequestStatus.OPEN);

        assertEquals(1, requests.size());
        assertEquals(LgpdRequestType.ACCESS, requests.getFirst().requestType());
    }

    @Test
    void shouldUpdateRequestStatusAndAppendHistory() {
        UUID requestId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Employee employee = buildEmployee(employeeId, companyId);
        LgpdRequest existing = buildRequest(employeeId, companyId, LgpdRequestType.ACCESS, LgpdRequestStatus.OPEN);

        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(new LgpdRequest(
                requestId,
                existing.employeeId(),
                existing.requestedByUserId(),
                existing.companyId(),
                existing.requestType(),
                existing.status(),
                existing.description(),
                existing.resolutionNotes(),
                existing.createdAt(),
                existing.updatedAt(),
                existing.resolvedAt(),
                existing.resolvedByUserId(),
                existing.assignedToUserId(),
                existing.dueAt(),
                existing.priority(),
                existing.closedReason(),
                existing.publicResolutionNotes(),
                existing.internalNotes()
        )));
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(lgpdRequestProvider.save(any(LgpdRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lgpdRequestHistoryProvider.save(any(LgpdRequestHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LgpdRequest updated = service.updateRequestStatus(
                requestId,
                new UpdateLgpdRequestStatusRequest(LgpdRequestStatus.COMPLETED, "Atendido")
        );

        assertEquals(LgpdRequestStatus.COMPLETED, updated.status());
        assertEquals("Atendido", updated.resolutionNotes());
        assertEquals(userId, updated.resolvedByUserId());
        assertNotNull(updated.resolvedAt());
        verify(lgpdRequestHistoryProvider).save(any(LgpdRequestHistory.class));
    }

    @Test
    void shouldExportSanitizedEmployeeDataAndAudit() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = buildEmployee(employeeId, companyId);
        User user = new User(userId, "lucas", "super-secret-hash", Role.MANAGER, true, employeeId);
        Company company = new Company(companyId, "KTS", "12345678000199", "contato@kts.com", true,
                new Address("Rua A", "100", "01001000", "São Paulo", "SP"),
                new Location(-23.0, -46.0), 5, 1);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.of(user));
        when(documentProvider.findAllByEmployeeId(employeeId)).thenReturn(List.of(new Document(
                UUID.randomUUID(),
                employeeId,
                DocumentType.BIOMETRIC_CONSENT_TERM,
                "termo.pdf",
                "application/pdf",
                "company/secret/path.pdf",
                LocalDateTime.now(),
                null,
                false,
                false,
                "checksum-123"
        )));
        when(timeRecordProvider.findByEmployeeId(employeeId)).thenReturn(List.of(new TimeRecord(
                1L,
                LocalDateTime.now().minusHours(8),
                LocalDateTime.now(),
                StatusRecord.CREATED,
                false,
                true,
                employeeId,
                -22.9035,
                -43.2096,
                -22.9040,
                -43.2101,
                10L,
                11L,
                null,
                null
        )));
        when(messageProvider.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, employeeId)).thenReturn(List.of(
                new Message(UUID.randomUUID(), employeeId, companyId, "Aviso", "Texto", MessagePriority.ALERT, LocalDateTime.now(), employeeId)
        ));
        when(auditService.findByUserId(userId)).thenReturn(List.of(
                AuditLog.create(
                        userId,
                        "ACTION",
                        "127.0.0.1",
                        "JUnit",
                        "cpf=12345678901 token=eyJhbGciOiJIUzI1NiJ9.payload.signature storage=storage/documents/secret.pdf image="
                                + "A".repeat(220)
                )
        ));
        when(legalConsentProvider.findAllByEmployeeId(employeeId)).thenReturn(List.of(
                new LegalConsent(
                        UUID.randomUUID(),
                        employeeId,
                        userId,
                        ConsentType.BIOMETRIC_AUTHENTICATION,
                        LegalBasis.CONSENT,
                        "Autenticação biométrica",
                        "2026.05.21",
                        Instant.now(),
                        null,
                        "127.0.0.1",
                        "JUnit",
                        UUID.randomUUID(),
                        "hash",
                        Instant.now(),
                        null
                )
        ));

        LgpdEmployeeExportResponse export = service.exportEmployeeData(employeeId, false, "127.0.0.1", "JUnit", null);

        assertEquals("lucas", export.user().username());
        assertEquals("checksum-123", export.documents().getFirst().checksumSha256());
        assertTrue(export.biometricStatus().faceImageRegistered());
        assertTrue(export.biometricStatus().activeBiometricConsent());
        assertTrue(export.timeRecords().getFirst().geolocationPresent());
        assertTrue(export.timeRecords().getFirst().endGeolocationPresent());
        assertEquals(null, export.timeRecords().getFirst().latitude());
        assertEquals(null, export.timeRecords().getFirst().longitude());
        assertFalse(export.auditLogs().getFirst().details().contains("12345678901"));
        assertFalse(export.auditLogs().getFirst().details().contains("payload.signature"));
        assertFalse(export.auditLogs().getFirst().details().contains("storage/documents/secret.pdf"));
        assertTrue(export.auditLogs().getFirst().details().contains("123.***.901"));
        assertTrue(export.auditLogs().getFirst().details().contains("[MASKED_PATH]"));
        assertTrue(export.auditLogs().getFirst().details().contains("[BASE64_REDACTED]"));
        assertFalse(recordComponentNames(LgpdEmployeeExportResponse.ExportedUser.class).contains("password"));
        assertFalse(recordComponentNames(LgpdEmployeeExportResponse.ExportedDocumentMetadata.class).contains("storagePath"));
        verify(auditService).registerLgpd(
                eq(AuditAction.LGPD_DATA_EXPORTED),
                eq(employeeId),
                eq(companyId),
                eq("EMPLOYEE"),
                any(),
                eq("MEDIUM"),
                any(),
                eq("127.0.0.1"),
                eq("JUnit")
        );
    }

    @Test
    void shouldExportPreciseGeolocationForDataSubject() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = buildEmployee(employeeId, companyId);

        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(new Company(
                companyId, "KTS", "12345678000199", "contato@kts.com", true,
                new Address("Rua A", "100", "01001000", "São Paulo", "SP"),
                new Location(-23.0, -46.0), 5, 1
        )));
        when(documentProvider.findAllByEmployeeId(employeeId)).thenReturn(List.of());
        when(timeRecordProvider.findByEmployeeId(employeeId)).thenReturn(List.of(new TimeRecord(
                1L, LocalDateTime.now().minusHours(8), LocalDateTime.now(), StatusRecord.CREATED, false, true,
                employeeId, -22.9035, -43.2096, -22.9040, -43.2101, 10L, 11L, null, null
        )));
        when(messageProvider.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, employeeId)).thenReturn(List.of());
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.empty());
        when(legalConsentProvider.findAllByEmployeeId(employeeId)).thenReturn(List.of());

        LgpdEmployeeExportResponse export = service.exportEmployeeData(employeeId, true, "127.0.0.1", "JUnit", null);

        assertEquals(-22.9035, export.timeRecords().getFirst().latitude());
        assertEquals(-43.2096, export.timeRecords().getFirst().longitude());
        verify(auditService).registerLgpd(
                eq(AuditAction.LGPD_DATA_EXPORTED),
                eq(employeeId),
                eq(companyId),
                eq("EMPLOYEE"),
                any(),
                eq("HIGH"),
                any(),
                eq("127.0.0.1"),
                eq("JUnit")
        );
    }

    @Test
    void shouldKeepManagerExportMinimizedEvenWhenPreciseGeolocationIsRequested() {
        UUID employeeId = UUID.randomUUID();
        UUID managerEmployeeId = UUID.randomUUID();
        UUID managerUserId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = buildEmployee(employeeId, companyId);

        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(managerUserId);
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(new Company(
                companyId, "KTS", "12345678000199", "contato@kts.com", true,
                new Address("Rua A", "100", "01001000", "São Paulo", "SP"),
                new Location(-23.0, -46.0), 5, 1
        )));
        when(documentProvider.findAllByEmployeeId(employeeId)).thenReturn(List.of());
        when(timeRecordProvider.findByEmployeeId(employeeId)).thenReturn(List.of(new TimeRecord(
                1L, LocalDateTime.now().minusHours(8), LocalDateTime.now(), StatusRecord.CREATED, false, true,
                employeeId, -22.9035, -43.2096, -22.9040, -43.2101, 10L, 11L, null, null
        )));
        when(messageProvider.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, employeeId)).thenReturn(List.of());
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.empty());
        when(legalConsentProvider.findAllByEmployeeId(employeeId)).thenReturn(List.of());

        LgpdEmployeeExportResponse export = service.exportEmployeeData(employeeId, true, "127.0.0.1", "JUnit", "Personnel file");

        assertEquals(null, export.timeRecords().getFirst().latitude());
        assertEquals(null, export.timeRecords().getFirst().longitude());
        verify(auditService).registerLgpd(
                eq(AuditAction.LGPD_DATA_EXPORTED),
                eq(employeeId),
                eq(companyId),
                eq("EMPLOYEE"),
                any(),
                eq("MEDIUM"),
                any(),
                eq("127.0.0.1"),
                eq("JUnit")
        );
    }

    @Test
    void shouldDelegateEmployeeAnonymization() {
        UUID employeeId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getuserId()).thenReturn(actorUserId);

        service.anonymizeEmployee(employeeId, "127.0.0.1", "JUnit");

        verify(employeeAnonymizationService).anonymize(employeeId, "127.0.0.1", "JUnit", actorUserId);
    }

    private List<String> recordComponentNames(Class<?> type) {
        return Arrays.stream(type.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();
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
                LocalDateTime.now(),
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

    @Test
    void shouldListAdminRequestsForCto() {
        UUID companyId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        Employee employee = buildEmployee(employeeId, companyId);
        Company company = new Company(companyId, "KTS", "12345678000199", "contato@kts.com", true,
                new Address("Rua A", "100", "01001000", "São Paulo", "SP"),
                new Location(-23.0, -46.0), 5, 1);

        LgpdRequest request = buildRequest(employeeId, companyId, LgpdRequestType.ACCESS, LgpdRequestStatus.OPEN);

        when(lgpdRequestProvider.findAll(any())).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(request)));
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));

        var result = service.listAdminRequests(null, null, null, org.springframework.data.domain.Pageable.unpaged());

        assertEquals(1, result.getContent().size());
        assertEquals("Lucas", result.getContent().get(0).employeeFullName());
        assertEquals("KTS", result.getContent().get(0).companyName());
    }

    @Test
    void shouldGetRequestDetailsWithEnrichedData() {
        UUID companyId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID adminUserId = UUID.randomUUID();
        Employee employee = buildEmployee(employeeId, companyId);
        Company company = new Company(companyId, "KTS", "12345678000199", "contato@kts.com", true,
                new Address("Rua A", "100", "01001000", "São Paulo", "SP"),
                new Location(-23.0, -46.0), 5, 1);
        User assignedAdmin = new User(adminUserId, "admin", "hash", Role.CTO, true, null);
        LgpdRequest request = new LgpdRequest(
                requestId,
                employeeId,
                UUID.randomUUID(),
                companyId,
                LgpdRequestType.ACCESS,
                LgpdRequestStatus.OPEN,
                "Exportar dados",
                null,
                Instant.now(),
                Instant.now(),
                null,
                null,
                adminUserId,
                Instant.now().plusSeconds(86400 * 15),
                "NORMAL",
                null,
                null,
                null
        );

        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(userProvider.findById(adminUserId)).thenReturn(Optional.of(assignedAdmin));
        when(lgpdRequestHistoryProvider.findByRequestId(requestId)).thenReturn(List.of());

        var result = service.getRequestDetails(requestId);

        assertNotNull(result);
        assertEquals("Lucas", result.employee().fullName());
        assertEquals("KTS", result.company().tradeName());
        assertEquals("admin", result.assignedTo().username());
    }

    @Test
    void managerCannotListRequestsFromOtherCompany() {
        UUID managerCompanyId = UUID.randomUUID();
        UUID otherCompanyId = UUID.randomUUID();
        com.kts.kronos.application.exceptions.ForbiddenException exception =
            new com.kts.kronos.application.exceptions.ForbiddenException("Manager não pode acessar empresa diferente");

        when(domainAuthorizationService.authorizeCompanyAccess(otherCompanyId))
                .thenThrow(exception);

        try {
            service.listAdminRequests(null, null, otherCompanyId, null);
        } catch (com.kts.kronos.application.exceptions.ForbiddenException e) {
            assertEquals("Manager não pode acessar empresa diferente", e.getMessage());
        }
    }

    @Test
    void managerCannotAccessRequestDetailsFromOtherCompany() {
        UUID managerCompanyId = UUID.randomUUID();
        UUID otherCompanyId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        Employee employee = buildEmployee(employeeId, otherCompanyId);
        LgpdRequest request = buildRequest(employeeId, otherCompanyId, LgpdRequestType.ACCESS, LgpdRequestStatus.OPEN);

        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(domainAuthorizationService.authorizeCompanyAccess(otherCompanyId))
                .thenThrow(new com.kts.kronos.application.exceptions.ForbiddenException("Manager não pode acessar empresa diferente"));

        try {
            service.getRequestDetails(requestId);
        } catch (com.kts.kronos.application.exceptions.ForbiddenException e) {
            assertEquals("Manager não pode acessar empresa diferente", e.getMessage());
        }
    }

    @Test
    void managerCannotAssignRequestsFromOtherCompany() {
        UUID otherCompanyId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID assignToUserId = UUID.randomUUID();
        Employee employee = buildEmployee(employeeId, otherCompanyId);
        LgpdRequest request = buildRequest(employeeId, otherCompanyId, LgpdRequestType.ACCESS, LgpdRequestStatus.OPEN);

        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(domainAuthorizationService.authorizeCompanyAccess(otherCompanyId))
                .thenThrow(new com.kts.kronos.application.exceptions.ForbiddenException("Manager não pode acessar empresa diferente"));

        try {
            service.assignRequest(requestId, assignToUserId);
        } catch (com.kts.kronos.application.exceptions.ForbiddenException e) {
            assertEquals("Manager não pode acessar empresa diferente", e.getMessage());
        }
    }

    @Test
    void managerCannotAddNoteToRequestsFromOtherCompany() {
        UUID otherCompanyId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        Employee employee = buildEmployee(employeeId, otherCompanyId);
        LgpdRequest request = buildRequest(employeeId, otherCompanyId, LgpdRequestType.ACCESS, LgpdRequestStatus.OPEN);

        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(domainAuthorizationService.authorizeCompanyAccess(otherCompanyId))
                .thenThrow(new com.kts.kronos.application.exceptions.ForbiddenException("Manager não pode acessar empresa diferente"));

        try {
            service.addNote(requestId, "public note", "internal note");
        } catch (com.kts.kronos.application.exceptions.ForbiddenException e) {
            assertEquals("Manager não pode acessar empresa diferente", e.getMessage());
        }
    }

    @Test
    void managerCannotCompleteRequestsFromOtherCompany() {
        UUID otherCompanyId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        Employee employee = buildEmployee(employeeId, otherCompanyId);
        LgpdRequest request = buildRequest(employeeId, otherCompanyId, LgpdRequestType.ACCESS, LgpdRequestStatus.OPEN);

        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(domainAuthorizationService.authorizeCompanyAccess(otherCompanyId))
                .thenThrow(new com.kts.kronos.application.exceptions.ForbiddenException("Manager não pode acessar empresa diferente"));

        try {
            service.completeRequest(requestId, "public notes", "internal notes");
        } catch (com.kts.kronos.application.exceptions.ForbiddenException e) {
            assertEquals("Manager não pode acessar empresa diferente", e.getMessage());
        }
    }

    @Test
    void managerCannotRejectRequestsFromOtherCompany() {
        UUID otherCompanyId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        Employee employee = buildEmployee(employeeId, otherCompanyId);
        LgpdRequest request = buildRequest(employeeId, otherCompanyId, LgpdRequestType.ACCESS, LgpdRequestStatus.OPEN);

        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(domainAuthorizationService.authorizeCompanyAccess(otherCompanyId))
                .thenThrow(new com.kts.kronos.application.exceptions.ForbiddenException("Manager não pode acessar empresa diferente"));

        try {
            service.rejectRequest(requestId, "Invalid request", "public note", "internal note");
        } catch (com.kts.kronos.application.exceptions.ForbiddenException e) {
            assertEquals("Manager não pode acessar empresa diferente", e.getMessage());
        }
    }

    @Test
    void exportEmployeeDataShouldFetchAuditLogsByUserIdNotEmployeeId() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = buildEmployee(employeeId, companyId);
        User user = new User(userId, "testuser", "hash", Role.MANAGER, true, employeeId);
        Company company = new Company(companyId, "KTS", "12345678000199", "contato@kts.com", true,
                new Address("Rua A", "100", "01001000", "São Paulo", "SP"),
                new Location(-23.0, -46.0), 5, 1);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.of(user));
        when(documentProvider.findAllByEmployeeId(employeeId)).thenReturn(List.of());
        when(timeRecordProvider.findByEmployeeId(employeeId)).thenReturn(List.of());
        when(messageProvider.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, employeeId)).thenReturn(List.of());
        when(auditService.findByUserId(userId)).thenReturn(List.of(
                AuditLog.create(userId, "TEST_ACTION", "127.0.0.1", "JUnit", "Test details")
        ));
        when(legalConsentProvider.findAllByEmployeeId(employeeId)).thenReturn(List.of());

        var response = service.exportEmployeeData(employeeId, false, "127.0.0.1", "JUnit", null);

        assertNotNull(response);
        assertNotNull(response.manifest());
        assertEquals(1, response.auditLogs().size());
        assertEquals(userId, response.auditLogs().getFirst().userId());
        verify(auditService).findByUserId(userId);
    }

    @Test
    void exportEmployeeDataShouldReturnEmptyAuditLogsIfNoUser() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = buildEmployee(employeeId, companyId);
        Company company = new Company(companyId, "KTS", "12345678000199", "contato@kts.com", true,
                new Address("Rua A", "100", "01001000", "São Paulo", "SP"),
                new Location(-23.0, -46.0), 5, 1);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.empty());
        when(documentProvider.findAllByEmployeeId(employeeId)).thenReturn(List.of());
        when(timeRecordProvider.findByEmployeeId(employeeId)).thenReturn(List.of());
        when(messageProvider.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, employeeId)).thenReturn(List.of());
        when(legalConsentProvider.findAllByEmployeeId(employeeId)).thenReturn(List.of());

        var response = service.exportEmployeeData(employeeId, false, "127.0.0.1", "JUnit", null);

        assertNotNull(response);
        assertNotNull(response.manifest());
        assertTrue(response.auditLogs().isEmpty());
    }

    @Test
    void exportEmployeeDataShouldIncludeManifest() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = buildEmployee(employeeId, companyId);
        Company company = new Company(companyId, "KTS", "12345678000199", "contato@kts.com", true,
                new Address("Rua A", "100", "01001000", "São Paulo", "SP"),
                new Location(-23.0, -46.0), 5, 1);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.empty());
        when(documentProvider.findAllByEmployeeId(employeeId)).thenReturn(List.of());
        when(timeRecordProvider.findByEmployeeId(employeeId)).thenReturn(List.of());
        when(messageProvider.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, employeeId)).thenReturn(List.of());
        when(legalConsentProvider.findAllByEmployeeId(employeeId)).thenReturn(List.of());

        var response = service.exportEmployeeData(employeeId, false, "127.0.0.1", "JUnit", null);

        assertNotNull(response.manifest());
        assertNotNull(response.manifest().exportId());
        assertNotNull(response.manifest().exportedAt());
        assertEquals(userId, response.manifest().requestedByUserId());
        assertEquals(employeeId, response.manifest().targetEmployeeId());
        assertFalse(response.manifest().includePreciseGeolocation());
        assertFalse(response.manifest().sections().isEmpty());
        assertFalse(response.manifest().warnings().isEmpty());
    }

    @Test
    void exportEmployeeDataShouldRequireReasonForThirdPartyExport() {
        UUID employeeId = UUID.randomUUID();
        UUID targetEmployeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = buildEmployee(targetEmployeeId, companyId);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(domainAuthorizationService.authorizeEmployeeAccess(targetEmployeeId)).thenReturn(employee);

        try {
            service.exportEmployeeData(targetEmployeeId, false, "127.0.0.1", "JUnit", null);
            fail("Should have thrown ForbiddenException");
        } catch (com.kts.kronos.application.exceptions.ForbiddenException e) {
            assertTrue(e.getMessage().contains("justificativa"));
        }
    }

    @Test
    void exportEmployeeDataShouldAllowThirdPartyExportWithReason() {
        UUID employeeId = UUID.randomUUID();
        UUID targetEmployeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = buildEmployee(targetEmployeeId, companyId);
        Company company = new Company(companyId, "KTS", "12345678000199", "contato@kts.com", true,
                new Address("Rua A", "100", "01001000", "São Paulo", "SP"),
                new Location(-23.0, -46.0), 5, 1);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(domainAuthorizationService.authorizeEmployeeAccess(targetEmployeeId)).thenReturn(employee);
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(userProvider.findByEmployeeId(targetEmployeeId)).thenReturn(Optional.empty());
        when(documentProvider.findAllByEmployeeId(targetEmployeeId)).thenReturn(List.of());
        when(timeRecordProvider.findByEmployeeId(targetEmployeeId)).thenReturn(List.of());
        when(messageProvider.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, targetEmployeeId)).thenReturn(List.of());
        when(legalConsentProvider.findAllByEmployeeId(targetEmployeeId)).thenReturn(List.of());

        var response = service.exportEmployeeData(targetEmployeeId, false, "127.0.0.1", "JUnit", "HR request for personnel file");

        assertNotNull(response);
        assertNotNull(response.manifest());
    }

    private LgpdRequest buildRequest(UUID employeeId, UUID companyId, LgpdRequestType type, LgpdRequestStatus status) {
        return new LgpdRequest(
                UUID.randomUUID(),
                employeeId,
                UUID.randomUUID(),
                companyId,
                type,
                status,
                "Descrição",
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
    }
}
