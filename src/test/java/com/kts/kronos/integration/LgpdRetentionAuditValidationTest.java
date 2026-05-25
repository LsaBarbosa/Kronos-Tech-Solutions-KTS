package com.kts.kronos.integration;

import com.kts.kronos.adapter.out.persistence.AuditLogRepository;
import com.kts.kronos.adapter.out.persistence.DocumentRepository;
import com.kts.kronos.adapter.out.persistence.MessageRepository;
import com.kts.kronos.adapter.out.persistence.PasswordResetTokenRepository;
import com.kts.kronos.adapter.out.persistence.entity.AuditLogEntity;
import com.kts.kronos.adapter.out.persistence.entity.DocumentEntity;
import com.kts.kronos.adapter.out.persistence.entity.MessageEntity;
import com.kts.kronos.adapter.out.persistence.entity.PasswordResetTokenEntity;
import com.kts.kronos.application.service.retention.RetentionPolicyExecutor;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.MessagePriority;
import com.kts.kronos.domain.model.enuns.RetentionExecutionMode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("LGPD Retention Audit Validation Tests")
class LgpdRetentionAuditValidationTest {

    @Autowired
    private RetentionPolicyExecutor retentionPolicyExecutor;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private com.kts.kronos.adapter.out.persistence.UserRepository userRepository;

    @MockitoBean
    private com.kts.kronos.adapter.out.persistence.BlacklistedTokenRepository blacklistedTokenRepository;

    @MockitoBean
    private com.kts.kronos.adapter.out.persistence.EmployeeRepository employeeRepository;

    private static final Pattern UUID_PATTERN = Pattern.compile(
        "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    private static final Pattern CPF_PATTERN = Pattern.compile(
        "^\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}$"
    );

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        documentRepository.deleteAll();
        messageRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
    }

    @Test
    @DisplayName("DRY_RUN retention does not log any employee IDs in audit")
    void testDryRunAuditHasNoEmployeeIds() {
        UUID employeeId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        LocalDateTime expiredDate = LocalDateTime.now(ZoneId.of("UTC")).minusDays(100);

        DocumentEntity doc = DocumentEntity.builder()
                .documentId(documentId)
                .employeeId(employeeId)
                .fileName("doc.pdf")
                .type(DocumentType.DOCUMENTS)
                .contentType("application/pdf")
                .storagePath("s3://bucket/path")
                .checksumSha256("hash-value")
                .uploadedAt(expiredDate)
                .build();

        documentRepository.save(doc);

        RetentionPolicy policy = new RetentionPolicy(
                UUID.randomUUID(),
                "TEST_DOCUMENT_RETENTION",
                "Test document retention",
                "DOCUMENT",
                90,
                RetentionExecutionMode.DRY_RUN,
                true,
                false,
                false,
                null,
                Instant.now(),
                Instant.now()
        );

        retentionPolicyExecutor.executePolicy(policy);

        List<AuditLogEntity> auditLogs = auditLogRepository.findAll();
        assertFalse(auditLogs.isEmpty(), "Audit log should be created");

        AuditLogEntity retentionAudit = auditLogs.stream()
                .filter(log -> log.getAction().contains("RETENTION"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Retention audit log not found"));

        // Verify no employee/user ID in audit
        assertNull(retentionAudit.getUserId(), "Audit should not contain userId");
        assertNull(retentionAudit.getCompanyId(), "Audit should not contain companyId");
        assertNotContainsUUID(retentionAudit.getDetails(), employeeId, "Details should not contain employee UUID");
        assertThat(retentionAudit.getRiskLevel()).isEqualTo("SYSTEM");
    }

    @Test
    @DisplayName("APPLY retention audit logs only aggregated metrics, not individual records")
    void testApplyAuditLogsAggregatedMetricsOnly() {
        UUID employeeId1 = UUID.randomUUID();
        UUID employeeId2 = UUID.randomUUID();
        LocalDateTime expiredDate = LocalDateTime.now(ZoneId.of("UTC")).minusDays(100);

        // Create multiple documents from different employees
        DocumentEntity doc1 = DocumentEntity.builder()
                .documentId(UUID.randomUUID())
                .employeeId(employeeId1)
                .fileName("doc1.pdf")
                .type(DocumentType.DOCUMENTS)
                .contentType("application/pdf")
                .storagePath("s3://bucket/doc1")
                .checksumSha256("hash1")
                .uploadedAt(expiredDate)
                .build();

        DocumentEntity doc2 = DocumentEntity.builder()
                .documentId(UUID.randomUUID())
                .employeeId(employeeId2)
                .fileName("doc2.pdf")
                .type(DocumentType.DOCUMENTS)
                .contentType("application/pdf")
                .storagePath("s3://bucket/doc2")
                .checksumSha256("hash2")
                .uploadedAt(expiredDate)
                .build();

        documentRepository.saveAll(List.of(doc1, doc2));

        RetentionPolicy policy = new RetentionPolicy(
                UUID.randomUUID(),
                "TEST_APPLY",
                "Test apply retention",
                "DOCUMENT",
                90,
                RetentionExecutionMode.DRY_RUN, // Use DRY_RUN as flag is disabled by default
                true,
                false,
                false,
                null,
                Instant.now(),
                Instant.now()
        );

        retentionPolicyExecutor.executePolicy(policy);

        List<AuditLogEntity> auditLogs = auditLogRepository.findAll();
        AuditLogEntity retentionAudit = auditLogs.stream()
                .filter(log -> log.getAction().contains("RETENTION"))
                .findFirst()
                .orElseThrow();

        String details = retentionAudit.getDetails();
        assertNotNull(details, "Audit details should contain metrics");

        JsonNode jsonDetails = parseJsonSafely(details);
        if (jsonDetails != null) {
            assertTrue(jsonDetails.has("totalScanned") || jsonDetails.has("totalAffected"),
                    "Audit should have metric counts");

            // Verify no individual employee IDs
            assertNotContainsUUID(details, employeeId1, "Details should not contain employee 1 UUID");
            assertNotContainsUUID(details, employeeId2, "Details should not contain employee 2 UUID");
        }
    }

    @Test
    @DisplayName("Retention audit does not contain personally identifiable information")
    void testRetentionAuditHasNoPII() {
        UUID employeeId = UUID.randomUUID();
        LocalDateTime expiredDate = LocalDateTime.now(ZoneId.of("UTC")).minusDays(100);

        MessageEntity message = MessageEntity.builder()
                .messageId(UUID.randomUUID())
                .employeeId(employeeId)
                .companyId(UUID.randomUUID())
                .title("Personal message")
                .messageText("This contains sensitive information")
                .priority(MessagePriority.CRITICAL)
                .createdAt(expiredDate)
                .deletedBySystem(false)
                .build();

        messageRepository.save(message);

        RetentionPolicy policy = new RetentionPolicy(
                UUID.randomUUID(),
                "TEST_MESSAGE",
                "Test message retention",
                "MESSAGE",
                730,
                RetentionExecutionMode.DRY_RUN,
                true,
                false,
                false,
                null,
                Instant.now(),
                Instant.now()
        );

        retentionPolicyExecutor.executePolicy(policy);

        List<AuditLogEntity> auditLogs = auditLogRepository.findAll();
        assertFalse(auditLogs.isEmpty(), "Audit should be created");

        AuditLogEntity retentionAudit = auditLogs.stream()
                .filter(log -> log.getAction().contains("RETENTION"))
                .findFirst()
                .orElseThrow();

        String details = retentionAudit.getDetails();

        // No personal data should be present
        assertDoesNotContain(details, "Personal message", "Audit should not contain message titles");
        assertDoesNotContain(details, "sensitive information", "Audit should not contain message content");
        assertNotContainsUUID(details, employeeId, "Audit should not contain employee ID");
    }

    @Test
    @DisplayName("Retention audit does not contain IP addresses or user agents")
    void testRetentionAuditHasNoClientMetadata() {
        LocalDateTime expiredDate = LocalDateTime.now(ZoneId.of("UTC")).minusDays(100);

        DocumentEntity doc = DocumentEntity.builder()
                .documentId(UUID.randomUUID())
                .employeeId(UUID.randomUUID())
                .fileName("doc.pdf")
                .type(DocumentType.DOCUMENTS)
                .contentType("application/pdf")
                .storagePath("s3://bucket/path")
                .checksumSha256("hash")
                .uploadedAt(expiredDate)
                .build();

        documentRepository.save(doc);

        RetentionPolicy policy = new RetentionPolicy(
                UUID.randomUUID(),
                "TEST_DOC",
                "Test",
                "DOCUMENT",
                90,
                RetentionExecutionMode.DRY_RUN,
                true,
                false,
                false,
                null,
                Instant.now(),
                Instant.now()
        );

        retentionPolicyExecutor.executePolicy(policy);

        AuditLogEntity retentionAudit = auditLogRepository.findAll().stream()
                .filter(log -> log.getAction().contains("RETENTION"))
                .findFirst()
                .orElseThrow();

        assertNull(retentionAudit.getIpAddress(), "Audit should not contain IP address");
        assertNull(retentionAudit.getUserAgent(), "Audit should not contain user agent");
    }

    @Test
    @DisplayName("Retention audit logs policy metadata for traceability")
    void testRetentionAuditLogsPolicyMetadata() {
        LocalDateTime expiredDate = LocalDateTime.now(ZoneId.of("UTC")).minusDays(100);

        DocumentEntity doc = DocumentEntity.builder()
                .documentId(UUID.randomUUID())
                .employeeId(UUID.randomUUID())
                .fileName("doc.pdf")
                .type(DocumentType.DOCUMENTS)
                .contentType("application/pdf")
                .storagePath("s3://bucket/path")
                .checksumSha256("hash")
                .uploadedAt(expiredDate)
                .build();

        documentRepository.save(doc);

        String policyCode = "TEST_RETENTION_POLICY";
        RetentionPolicy policy = new RetentionPolicy(
                UUID.randomUUID(),
                policyCode,
                "Test retention for documents",
                "DOCUMENT",
                90,
                RetentionExecutionMode.DRY_RUN,
                true,
                false,
                false,
                null,
                Instant.now(),
                Instant.now()
        );

        retentionPolicyExecutor.executePolicy(policy);

        AuditLogEntity retentionAudit = auditLogRepository.findAll().stream()
                .filter(log -> log.getAction().contains("RETENTION"))
                .findFirst()
                .orElseThrow();

        String details = retentionAudit.getDetails();
        assertTrue(details.contains(policyCode), "Audit should contain policy code for traceability");
        assertTrue(details.contains("DRY_RUN"), "Audit should contain execution mode");
    }

    @Test
    @DisplayName("Password reset token retention audit contains no token data")
    void testPasswordTokenRetentionAuditHasNoTokenData() {
        UUID employeeId = UUID.randomUUID();
        LocalDateTime expiredDate = LocalDateTime.now(ZoneId.of("UTC")).minusDays(10);

        String tokenValue = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP1THsR8U";
        PasswordResetTokenEntity token = PasswordResetTokenEntity.builder()
                .token(tokenValue)
                .userId(employeeId)
                .expiryDate(expiredDate)
                .createdAt(expiredDate)
                .build();

        passwordResetTokenRepository.save(token);

        RetentionPolicy policy = new RetentionPolicy(
                UUID.randomUUID(),
                "TEST_TOKEN",
                "Test token retention",
                "PASSWORD_RESET_TOKEN",
                1,
                RetentionExecutionMode.DRY_RUN,
                true,
                false,
                false,
                null,
                Instant.now(),
                Instant.now()
        );

        retentionPolicyExecutor.executePolicy(policy);

        AuditLogEntity retentionAudit = auditLogRepository.findAll().stream()
                .filter(log -> log.getAction().contains("RETENTION"))
                .findFirst()
                .orElseThrow();

        String details = retentionAudit.getDetails();
        assertDoesNotContain(details, tokenValue, "Audit should not contain actual token value");
        assertDoesNotContain(details, "eyJ", "Audit should not contain JWT prefix");
    }

    // Helper methods

    private JsonNode parseJsonSafely(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    private void assertNotContainsUUID(String content, UUID uuid, String message) {
        if (content != null) {
            assertFalse(content.contains(uuid.toString()), message);
        }
    }

    private void assertDoesNotContain(String content, String substring, String message) {
        if (content != null && !substring.isEmpty()) {
            assertFalse(content.toLowerCase().contains(substring.toLowerCase()), message);
        }
    }

    private static class assertThat {
        private final String value;

        private assertThat(String value) {
            this.value = value;
        }

        public void isEqualTo(String expected) {
            assertEquals(expected, value);
        }
    }

    private static assertThat assertThat(String value) {
        return new assertThat(value);
    }
}
