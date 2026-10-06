package com.kts.kronos.misc;

import com.kts.kronos.adapter.out.persistence.BlacklistedTokenRepository;
import com.kts.kronos.application.exceptions.IncidentCommunicationDeadlineException;
import com.kts.kronos.application.scheduler.BlacklistedTokenCleanupScheduler;
import com.kts.kronos.application.security.PrivacyLogReferenceService;
import com.kts.kronos.application.service.retention.EmployeeContractRetentionProcessor;
import com.kts.kronos.domain.model.RetentionExecutionResult;
import com.kts.kronos.domain.model.RetentionPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.kts.kronos.adapter.in.web.dto.lgpd.AnonymizationDryRunResponse;
import com.kts.kronos.adapter.in.web.dto.lgpd.LgpdRequestAdminListResponse;
import com.kts.kronos.adapter.out.persistence.entity.MessageDeliveryEntity;
import com.kts.kronos.adapter.out.persistence.entity.MessageEntity;
import com.kts.kronos.application.service.anonymization.util.AnonymizationUtil;
import com.kts.kronos.application.service.retention.RetentionDomainProcessor;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Document;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.LgpdRequest;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.LgpdRequestStatus;
import com.kts.kronos.domain.model.enuns.LgpdRequestType;
import com.kts.kronos.domain.model.enuns.RetentionExecutionMode;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import com.kts.kronos.application.service.PublicPrivacyService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import com.kts.kronos.application.exceptions.DryRunTokenExpiredException;
import com.kts.kronos.application.exceptions.DryRunTokenInvalidException;
import com.kts.kronos.domain.model.ClientIpResolution;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.Role;

import com.kts.kronos.adapter.in.web.dto.retention.RetentionExecutionResponse;
import com.kts.kronos.adapter.in.web.dto.retention.RetentionPolicyResponse;
import com.kts.kronos.adapter.in.web.dto.security.FaceLoginRequest;
import com.kts.kronos.adapter.in.web.dto.terminal.TerminalCheckinRequest;
import com.kts.kronos.application.exceptions.IncidentClosureValidationException;
import com.kts.kronos.domain.model.enuns.LivenessOperation;
import com.kts.kronos.domain.model.enuns.SecurityImpactLevel;
import com.kts.kronos.domain.model.enuns.RetentionPolicyType;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.kts.kronos.adapter.in.web.dto.timerecord.GeolocationRequest;
import com.kts.kronos.adapter.in.web.http.DashboardController;
import com.kts.kronos.application.service.DashboardService;
import com.kts.kronos.domain.model.TimesheetSignature;
import com.kts.kronos.domain.model.enuns.TimesheetSignatureMethod;
import com.kts.kronos.domain.model.enuns.TimesheetSignatureStatus;
import com.kts.kronos.domain.model.enuns.TimesheetSignatureType;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MiscCoverageTest {

    @Mock private BlacklistedTokenRepository blacklistRepo;

    // ── IncidentCommunicationDeadlineException constructors ───────────────────
    @Test
    void incidentDeadlineException_withDetails_hasCode() {
        UUID id = UUID.randomUUID();
        var ex = new IncidentCommunicationDeadlineException(id, "deadline missing");
        assertEquals("INCIDENT_COMMUNICATION_DEADLINE_REQUIRED", ex.getCode());
        assertEquals(id, ex.getIncidentId());
        assertNotNull(ex.getMessage());
    }

    @Test
    void incidentDeadlineException_noDetails_hasCode() {
        UUID id = UUID.randomUUID();
        var ex = new IncidentCommunicationDeadlineException(id);
        assertEquals("INCIDENT_COMMUNICATION_DEADLINE_REQUIRED", ex.getCode());
        assertEquals(id, ex.getIncidentId());
    }

    // ── BlacklistedTokenCleanupScheduler.cleanupExpiredTokens() ─────────────
    @Test
    void cleanupExpiredTokens_delegatesToRepository() {
        var scheduler = new BlacklistedTokenCleanupScheduler(blacklistRepo);
        scheduler.cleanupExpiredTokens();
        verify(blacklistRepo).deleteExpiredTokens(any(LocalDateTime.class));
    }

    // ── PrivacyLogReferenceService: uncalled methods + null/empty branches ───
    @Test
    void privacyLogRef_storageRef_objectRef_faceRef_covered() {
        var svc = new PrivacyLogReferenceService("test-hmac-secret-for-unit-tests-32b!");
        assertNotNull(svc.storageRef("s3://bucket/key.pdf"));
        assertNotNull(svc.objectRef("face/image.jpg"));
        assertNotNull(svc.faceRef("face-abc-123"));
    }

    @Test
    void privacyLogRef_nullValue_returnsNoneRef() {
        var svc = new PrivacyLogReferenceService("test-hmac-secret-for-unit-tests-32b!");
        String ref = svc.storageRef(null);
        assertTrue(ref.endsWith("_ref_none"), "Expected none ref, got: " + ref);
    }

    @Test
    void privacyLogRef_emptyStoragePath_returnsEmptyRef() {
        var svc = new PrivacyLogReferenceService("test-hmac-secret-for-unit-tests-32b!");
        String ref = svc.genericRef("storage", "");
        assertTrue(ref.endsWith("_ref_empty"), "Expected empty ref, got: " + ref);
    }

    @Test
    void privacyLogRef_externalImageRef_covered() {
        var svc = new PrivacyLogReferenceService("test-hmac-secret-for-unit-tests-32b!");
        String ref = svc.externalImageRef(UUID.randomUUID());
        assertNotNull(ref, "externalImageRef deve retornar referência não-nula");
    }

    // ── EmployeeContractRetentionProcessor: catch block ──────────────────────
    // First policyCode() call (inside executeDryRun) throws → enters catch block.
    // Subsequent calls return a value so the catch block can complete normally.
    @Test
    void employeeContractProcessor_throwingPolicy_coversExceptionCatch() {
        var processor = new EmployeeContractRetentionProcessor();
        RetentionPolicy policy = mock(RetentionPolicy.class);
        when(policy.policyCode())
                .thenThrow(new RuntimeException("forced-error"))
                .thenReturn("POLICY_CODE");
        RetentionExecutionResult result = processor.execute(policy, "DRY_RUN");
        assertEquals("ERROR", result.status());
    }
    // ── AnonymizationUtil: default constructor ──────────────────────────────
    @Test
    void anonymizationUtil_defaultConstructor_covered() {
        assertNotNull(new AnonymizationUtil());
    }

    // ── AnonymizationDryRunResponse.empty() ─────────────────────────────────
    @Test
    void anonymizationDryRunResponse_empty_covered() {
        var r = AnonymizationDryRunResponse.empty(UUID.randomUUID());
        assertNotNull(r.employeeId());
        assertNotNull(r.summary());
    }

    // ── Document 9-arg constructor ───────────────────────────────────────────
    @Test
    void document_nineArgConstructor_covered() {
        var doc = new Document(UUID.randomUUID(), DocumentType.PAYSLIP,
                "file.pdf", "application/pdf", "/storage/path",
                LocalDateTime.now(), null, false, false);
        assertNotNull(doc.documentId());
    }

    // ── LgpdRequestAdminListResponse.fromDomain(request,employee,company,user) ─
    @Test
    void lgpdRequestAdminListResponse_secondFromDomain_covered() {
        LgpdRequest request = mock(LgpdRequest.class);
        when(request.requestId()).thenReturn(UUID.randomUUID());
        when(request.requestType()).thenReturn(LgpdRequestType.ANONYMIZATION);
        when(request.status()).thenReturn(LgpdRequestStatus.OPEN);
        when(request.createdAt()).thenReturn(Instant.now());
        when(request.updatedAt()).thenReturn(Instant.now());
        Employee employee = mock(Employee.class);
        when(employee.fullName()).thenReturn("Test Employee");
        Company company = mock(Company.class);
        when(company.name()).thenReturn("Test Company");

        var response = LgpdRequestAdminListResponse.fromDomain(
                request, employee, company, java.util.Optional.empty());
        assertEquals("Test Employee", response.employeeFullName());
        assertEquals("Test Company", response.companyName());
    }

    // ── RetentionPolicy Integer retentionDays constructor ────────────────────
    @Test
    void retentionPolicy_integerRetentionDaysConstructor_covered() {
        var policy = new RetentionPolicy(
                UUID.randomUUID(), "TEST", "desc", "RESOURCE",
                Integer.valueOf(30), RetentionExecutionMode.DRY_RUN,
                true, false, false, null, Instant.now(), null);
        assertNotNull(policy.policyId());
        assertEquals(30, policy.retentionDays());
    }

    // ── RetentionDomainProcessor default interface methods ───────────────────
    @Test
    void retentionDomainProcessor_defaultMethods_covered() {
        RetentionDomainProcessor proc = new RetentionDomainProcessor() {
            @Override public RetentionResourceType supports() {
                return RetentionResourceType.BLACKLISTED_TOKEN;
            }
            @Override public com.kts.kronos.domain.model.RetentionExecutionResult execute(
                    RetentionPolicy policy, String mode) {
                return null;
            }
        };
        assertTrue(proc.supportsDryRun());
        assertFalse(proc.supportsApply());
        assertFalse(proc.isDestructive());
    }

    // ── MessageDeliveryEntity.toDomain() ─────────────────────────────────────
    @Test
    void messageDeliveryEntity_toDomain_covered() {
        MessageEntity msgEntity = mock(MessageEntity.class);
        when(msgEntity.getMessageId()).thenReturn(UUID.randomUUID());
        var entity = new MessageDeliveryEntity(
                UUID.randomUUID(), msgEntity, UUID.randomUUID(),
                LocalDateTime.now(), null);
        var domain = entity.toDomain();
        assertNotNull(domain);
        assertNotNull(domain.messageDeliveryId());
    }


    // ── DryRunTokenInvalidException 2-arg constructor ───────────────────────
    @Test
    void dryRunTokenInvalidException_causeConstructor_covered() {
        var cause = new RuntimeException("root");
        var ex = new DryRunTokenInvalidException("invalid token", cause);
        assertEquals("invalid token", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    // ── DryRunTokenExpiredException 2-arg constructor ────────────────────────
    @Test
    void dryRunTokenExpiredException_causeConstructor_covered() {
        var cause = new RuntimeException("root");
        var ex = new DryRunTokenExpiredException("expired token", cause);
        assertEquals("expired token", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    // ── ClientIpResolution 3-arg constructor ─────────────────────────────────
    @Test
    void clientIpResolution_threeArgConstructor_covered() {
        var resolution = new ClientIpResolution("1.2.3.4", "REMOTE_ADDR", true);
        assertEquals("1.2.3.4", resolution.ipAddress());
        assertEquals("REMOTE_ADDR", resolution.source());
        assertTrue(resolution.trusted());
        assertTrue(resolution.proxyChainValid());
    }

    // ── User 9-arg constructor ────────────────────────────────────────────────
    @Test
    void user_nineArgConstructor_covered() {
        var userId = UUID.randomUUID();
        var employeeId = UUID.randomUUID();
        var user = new User(userId, "user@kts.com", "hash", Role.MANAGER, true,
                employeeId, LocalDateTime.now(), null, "reason");
        assertEquals(userId, user.userId());
        assertEquals(0L, user.sessionVersion());
    }

    // ── User canonical constructor null-sessionVersion branch ─────────────────
    @Test
    void user_canonicalConstructor_nullSessionVersion_coercedToZero() {
        var userId = UUID.randomUUID();
        // Calling the 10-arg canonical constructor with null sessionVersion
        // triggers: sessionVersion = sessionVersion == null ? 0L : sessionVersion
        var user = new User(userId, "u@kts.com", "hash", Role.MANAGER, true,
                null, (Long) null, null, null, null);
        assertEquals(0L, user.sessionVersion());
        assertNull(user.employeeId());
    }



    // ── SecurityImpactLevel.getLabel ──────────────────────────────────────────
    @Test
    void securityImpactLevel_getLabel_coveredForEachValue() {
        assertEquals("Sem impacto", SecurityImpactLevel.NONE.getLabel());
        assertEquals("Crítico",     SecurityImpactLevel.CRITICAL.getLabel());
    }

    // ── LivenessOperation.getDescription ─────────────────────────────────────
    @Test
    void livenessOperation_getDescription_coveredForEachValue() {
        assertEquals("Matrícula biométrica", LivenessOperation.ENROLLMENT.getDescription());
        assertEquals("Login por rosto",      LivenessOperation.FACE_LOGIN.getDescription());
    }

    // ── IncidentClosureValidationException.getMessage ─────────────────────────
    @Test
    void incidentClosureValidationException_getMessage_returnsExpectedMessage() {
        var id = UUID.randomUUID();
        var ex = new IncidentClosureValidationException(id, List.of("evidenceFile", "description"));
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("evidenceFile"));
        assertEquals(id, ex.getIncidentId());
        assertEquals(List.of("evidenceFile", "description"), ex.getMissingFields());
        assertEquals("INCIDENT_CLOSURE_MISSING_EVIDENCE", ex.getCode());
    }

    // ── FaceLoginRequest.toString ─────────────────────────────────────────────
    @Test
    void faceLoginRequest_toString_masksFaceImage() {
        var req = new FaceLoginRequest("base64imageData", true);
        var str = req.toString();
        assertFalse(str.contains("base64imageData"));
        assertTrue(str.contains("***MASKED***"));
    }

    // ── TerminalCheckinRequest.toString ───────────────────────────────────────
    @Test
    void terminalCheckinRequest_toString_masksFaceImage() {
        var req = new TerminalCheckinRequest("base64imageData", true, -23.5, -46.6);
        var str = req.toString();
        assertFalse(str.contains("base64imageData"));
        assertTrue(str.contains("REDACTED"));
    }

    // ── RetentionExecutionResponse.fromResult null resourceType (BR L38) ─────
    @Test
    void retentionExecutionResponse_fromResult_nullResourceType_producesNullResourceType() {
        var result = new RetentionExecutionResult(
                UUID.randomUUID(), "POLICY", null, "DRY_RUN",
                Instant.now(), Instant.now(), "SUCCESS", 0L, 0L, 0L, 0L, null
        );
        var response = RetentionExecutionResponse.fromResult(result);
        assertNull(response.resourceType());
    }

    // ── RetentionPolicyResponse.fromDomain null retentionDays (BR L24) ───────
    @Test
    void retentionPolicyResponse_fromDomain_nullRetentionDays_producesNullLong() {
        var policy = new RetentionPolicy(
                UUID.randomUUID(), "CODE", "desc",
                RetentionPolicyType.TIME_BASED, "AUDIT_LOG",
                null,
                RetentionExecutionMode.DRY_RUN,
                true, false, false, null, Instant.now(), null
        );
        var response = RetentionPolicyResponse.fromDomain(policy);
        assertNull(response.retentionDays());
    }


    // ── PublicPrivacyService BR L265: cacheProvider==null → loader.get() ─────
    @Test
    void publicPrivacyService_nullCacheProvider_usesLoaderDirectly() {
        var service = new PublicPrivacyService(null);
        var result = service.getPublicProcessingCatalog();
        assertNotNull(result);
    }

    // ── GeolocationRequest.toString masks faceImageBase64 ────────────────────
    @Test
    void geolocationRequest_toString_masksFaceImage() {
        var req = new GeolocationRequest(
                -23.5614, -46.6560,
                "base64FaceImageData",
                true
        );
        String str = req.toString();
        assertFalse(str.contains("base64FaceImageData"), "toString should mask faceImageBase64");
        assertTrue(str.contains("***MASKED***"));
    }

    // ── TimesheetSignature.withId returns signature with new ID ──────────────
    @Test
    void timesheetSignature_withId_updatesSignatureId() {
        var original = new TimesheetSignature(
                UUID.randomUUID(),           // signatureId
                UUID.randomUUID(),           // employeeId
                UUID.randomUUID(),           // companyId
                UUID.randomUUID(),           // signerUserId
                2026, 7,                     // referenceYear, referenceMonth
                java.time.LocalDate.now(), java.time.LocalDate.now(),  // periodStart, periodEnd
                java.time.Instant.now(), "America/Sao_Paulo",          // signedAt, signedAtZone
                TimesheetSignatureType.INTERNAL_ADVANCED,
                TimesheetSignatureMethod.PASSWORD_REAUTH,
                TimesheetSignatureStatus.ACTIVE,
                null, null, null,            // pointMirrorDocumentId, pointMirrorHashSha256, recordsSnapshotHashSha256
                null, null, null,            // declarationVersion, declarationHashSha256, declarationText
                "127.0.0.1", "JUnit",        // ipAddress, userAgent
                null,                        // evidenceJson
                java.time.Instant.now(), java.time.Instant.now(),  // createdAt, updatedAt
                null, null, null,            // voidedAt, voidedByUserId, voidReason
                "DIGITAL", "v1.0",           // documentType, documentVersion
                null, null, null             // canonicalEvidenceHashSha256, auditLogId, padesSignatureStatus
        );
        UUID newId = UUID.randomUUID();
        var updated = original.withId(newId);
        assertEquals(newId, updated.signatureId());
        assertEquals(original.employeeId(), updated.employeeId());
        assertEquals(original.companyId(), updated.companyId());
    }

    // ── DashboardController.getDashboardSummary delegates to service ─────────
    @Test
    void dashboardController_getDashboardSummary_returnsOk() {
        var dashService = mock(DashboardService.class);
        var summaryResponse = mock(com.kts.kronos.adapter.in.web.dto.dashboard.DashboardSummaryResponse.class);
        when(dashService.getDashboardSummary()).thenReturn(summaryResponse);
        var controller = new DashboardController(dashService);
        var result = controller.getDashboardSummary();
        assertEquals(200, result.getStatusCode().value());
        assertEquals(summaryResponse, result.getBody());
    }

    // ── CreateEmployeeRequest.toString() masks sensitive fields (LINE 69) ─────
    @Test
    void createEmployeeRequest_toString_masksCpfAndFace() {
        var req = new com.kts.kronos.adapter.in.web.dto.employee.CreateEmployeeRequest(
                "Ana Lima", "12345678901", null, "Analista", "ana@kts.com",
                3000.0, null, null, UUID.randomUUID(), false,
                "base64FaceData", null, null, null, null, null, null, null, null, null, null, null, null, null
        );
        String str = req.toString();
        assertTrue(str.contains("Ana Lima"));
        assertFalse(str.contains("12345678901"), "CPF should be masked");
        assertFalse(str.contains("base64FaceData"), "faceImageBase64 should be masked");
        assertTrue(str.contains("***MASKED***"));
    }

    // ── UpdateEmployeeManagerRequest.toString() masks sensitive fields (LINE 57) ─
    @Test
    void updateEmployeeManagerRequest_toString_masksSensitiveFields() {
        var req = new com.kts.kronos.adapter.in.web.dto.employee.UpdateEmployeeManagerRequest(
                "Carlos", "12345678901", null, "Dev", "carlos@kts.com",
                4000.0, null, null, null, "faceBase64Data",
                null, null, null, null, null, null, null, null, null, null, null, null, null
        );
        String str = req.toString();
        assertTrue(str.contains("Carlos"));
        assertFalse(str.contains("12345678901"), "CPF should be masked");
        assertFalse(str.contains("faceBase64Data"), "faceImageBase64 should be masked");
        assertTrue(str.contains("***MASKED**"));
    }

    // ── EmployeeDetailResponse.fromDomain(4-arg) delegates to 5-arg (LINE 56) ──
    @Test
    void employeeDetailResponse_fromDomain4Args_sandbox() {
        var empId = UUID.randomUUID();
        var companyId = UUID.randomUUID();
        var address = new com.kts.kronos.domain.model.Address("Rua A", "100", "12345678", "SP", "SP");
        var employee = new com.kts.kronos.domain.model.Employee(
                empId, "Lucas", "12345678901", "98765432100", "Dev", "lucas@kts.com",
                5000.0, null, true, address, companyId,
                java.time.LocalDateTime.of(2026, 1, 1, 8, 0), false, null,
                java.time.LocalTime.of(8, 0), java.time.LocalTime.of(17, 0),
                java.time.LocalTime.of(12, 0), java.time.LocalTime.of(13, 0),
                com.kts.kronos.domain.model.enuns.WorkScheduleType.TRADITIONAL_5X2,
                java.time.LocalDate.of(2026, 1, 1),
                java.time.DayOfWeek.SUNDAY, 1,
                java.util.Set.of(java.time.DayOfWeek.MONDAY)
        );
        var response = com.kts.kronos.adapter.in.web.dto.employee.EmployeeDetailResponse.fromDomain(
                employee, "KTS", "MANAGER", true
        );
        assertNotNull(response);
        assertTrue(response.sandbox());
    }


}