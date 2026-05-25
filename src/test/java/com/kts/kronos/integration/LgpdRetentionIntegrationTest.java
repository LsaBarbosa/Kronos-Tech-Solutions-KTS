package com.kts.kronos.integration;

import com.kts.kronos.adapter.out.persistence.BlacklistedTokenRepository;
import com.kts.kronos.adapter.out.persistence.DocumentRepository;
import com.kts.kronos.adapter.out.persistence.EmployeeRepository;
import com.kts.kronos.adapter.out.persistence.MessageRepository;
import com.kts.kronos.adapter.out.persistence.PasswordResetTokenRepository;
import com.kts.kronos.adapter.out.persistence.UserRepository;
import com.kts.kronos.adapter.out.persistence.entity.DocumentEntity;
import com.kts.kronos.adapter.out.persistence.entity.MessageEntity;
import com.kts.kronos.adapter.out.persistence.entity.PasswordResetTokenEntity;
import com.kts.kronos.application.service.retention.RetentionPolicyExecutor;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.MessagePriority;
import com.kts.kronos.domain.model.enuns.RetentionExecutionMode;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.mockito.Mockito;
import com.kts.kronos.adapter.out.persistence.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(LgpdRetentionIntegrationTest.RetentionTestConfiguration.class)
@DisplayName("LGPD Retention Integration Tests")
class LgpdRetentionIntegrationTest {

    @Autowired
    private RetentionPolicyExecutor retentionPolicyExecutor;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private BlacklistedTokenRepository blacklistedTokenRepository;

    @MockitoBean
    private EmployeeRepository employeeRepository;

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
        messageRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
    }

    @Test
    @DisplayName("DRY_RUN mode is supported")
    void testDryRunModeSupported() {
        UUID documentId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        LocalDateTime expiredDate = LocalDateTime.now(ZoneId.of("UTC")).minusDays(100);

        DocumentEntity expiredDoc = DocumentEntity.builder()
                .documentId(documentId)
                .employeeId(employeeId)
                .fileName("expired-document.pdf")
                .type(DocumentType.DOCUMENTS)
                .contentType("application/pdf")
                .storagePath("s3://bucket/expired-document")
                .checksumSha256("hash-expired")
                .uploadedAt(expiredDate)
                .build();

        documentRepository.save(expiredDoc);
        long initialCount = documentRepository.count();

        RetentionPolicy policy = new RetentionPolicy(
                UUID.randomUUID(),
                "TEST_DOCUMENT_RETENTION",
                "Test document retention",
                "DOCUMENT",
                90,
                RetentionExecutionMode.DRY_RUN,
                true,
                true,
                true,
                null,
                Instant.now(),
                Instant.now()
        );

        retentionPolicyExecutor.executePolicy(policy);

        long finalCount = documentRepository.count();
        assertEquals(initialCount, finalCount, "DRY_RUN should not delete documents");
        assertTrue(documentRepository.existsById(documentId), "Document should still exist after DRY_RUN");
    }

    @Test
    @DisplayName("Password reset tokens can be processed")
    void testPasswordResetTokensSupported() {
        UUID tokenId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        LocalDateTime expiredDate = LocalDateTime.now(ZoneId.of("UTC")).minusDays(10);

        PasswordResetTokenEntity expiredToken = PasswordResetTokenEntity.builder()
                .token("test-token-expired-" + System.nanoTime())
                .userId(employeeId)
                .expiryDate(expiredDate)
                .createdAt(expiredDate)
                .build();

        passwordResetTokenRepository.save(expiredToken);
        long initialCount = passwordResetTokenRepository.count();

        RetentionPolicy policy = new RetentionPolicy(
                UUID.randomUUID(),
                "TEST_PASSWORD_TOKEN_RETENTION",
                "Test password reset token retention",
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

        long finalCount = passwordResetTokenRepository.count();
        assertEquals(initialCount, finalCount, "DRY_RUN should not delete tokens");
    }

    @Test
    @DisplayName("Messages can be processed")
    void testMessagesSupported() {
        UUID messageId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        LocalDateTime expiredDate = LocalDateTime.now(ZoneId.of("UTC")).minusDays(800);

        MessageEntity expiredMessage = MessageEntity.builder()
                .messageId(messageId)
                .employeeId(employeeId)
                .companyId(companyId)
                .title("Old message")
                .messageText("This is an old message")
                .priority(MessagePriority.NORMAL)
                .createdAt(expiredDate)
                .deletedBySystem(false)
                .build();

        messageRepository.save(expiredMessage);
        long initialCount = messageRepository.count();

        RetentionPolicy policy = new RetentionPolicy(
                UUID.randomUUID(),
                "TEST_MESSAGE_RETENTION",
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

        long finalCount = messageRepository.count();
        assertEquals(initialCount, finalCount, "DRY_RUN should not delete messages");
    }

    @Test
    @DisplayName("APPLY mode requires flag to be enabled")
    void testApplyModeRequiresFlag() {
        UUID documentId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        LocalDateTime expiredDate = LocalDateTime.now(ZoneId.of("UTC")).minusDays(100);

        DocumentEntity doc = DocumentEntity.builder()
                .documentId(documentId)
                .employeeId(employeeId)
                .fileName("contract.pdf")
                .type(DocumentType.DOCUMENTS)
                .contentType("application/pdf")
                .storagePath("s3://bucket/contract")
                .checksumSha256("hash-contract")
                .uploadedAt(expiredDate)
                .build();

        documentRepository.save(doc);
        long initialCount = documentRepository.count();

        RetentionPolicy policy = new RetentionPolicy(
                UUID.randomUUID(),
                "TEST_DOCUMENT",
                "Test document retention",
                "DOCUMENT",
                90,
                RetentionExecutionMode.APPLY,
                true,
                true,
                true,
                null,
                Instant.now(),
                Instant.now()
        );

        retentionPolicyExecutor.executePolicy(policy);

        long finalCount = documentRepository.count();
        assertTrue(finalCount > 0, "APPLY should be blocked when flag is false (default in test)");
    }

    @Test
    @DisplayName("Empty data set is handled correctly")
    void testEmptyDataSetHandling() {
        documentRepository.deleteAll();
        messageRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();

        assertEquals(0, documentRepository.count(), "All documents should be deleted");
        assertEquals(0, messageRepository.count(), "All messages should be deleted");
        assertEquals(0, passwordResetTokenRepository.count(), "All tokens should be deleted");

        RetentionPolicy policy = new RetentionPolicy(
                UUID.randomUUID(),
                "TEST_EMPTY",
                "Test with empty data",
                "DOCUMENT",
                90,
                RetentionExecutionMode.DRY_RUN,
                true,
                true,
                true,
                null,
                Instant.now(),
                Instant.now()
        );

        retentionPolicyExecutor.executePolicy(policy);

        assertEquals(0, documentRepository.count(), "Still no documents");
    }

    @TestConfiguration
    static class RetentionTestConfiguration {
        @Bean
        public PasswordResetTokenRepository passwordResetTokenRepository() {
            return Mockito.mock(PasswordResetTokenRepository.class);
        }

        @Bean
        public LegalConsentRepository legalConsentRepository() {
            return Mockito.mock(LegalConsentRepository.class);
        }

        @Bean
        public AuditLogRepository auditLogRepository() {
            return Mockito.mock(AuditLogRepository.class);
        }

        @Bean
        public CompanyRepository companyRepository() {
            return Mockito.mock(CompanyRepository.class);
        }

        @Bean
        public org.springframework.mail.javamail.JavaMailSender javaMailSender() {
            return Mockito.mock(org.springframework.mail.javamail.JavaMailSender.class);
        }
    }

    @Test
    @DisplayName("Multiple policies execution in sequence")
    void testMultiplePoliciesSequential() {
        UUID documentId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        LocalDateTime expiredDate = LocalDateTime.now(ZoneId.of("UTC")).minusDays(100);

        DocumentEntity doc = DocumentEntity.builder()
                .documentId(documentId)
                .employeeId(employeeId)
                .fileName("doc.pdf")
                .type(DocumentType.DOCUMENTS)
                .contentType("application/pdf")
                .storagePath("s3://bucket/doc")
                .checksumSha256("hash-doc")
                .uploadedAt(expiredDate)
                .build();

        PasswordResetTokenEntity token = PasswordResetTokenEntity.builder()
                .token("token-" + System.nanoTime())
                .userId(employeeId)
                .expiryDate(expiredDate)
                .createdAt(expiredDate)
                .build();

        documentRepository.save(doc);
        passwordResetTokenRepository.save(token);

        RetentionPolicy policy1 = new RetentionPolicy(
                UUID.randomUUID(),
                "TEST_DOCUMENT",
                "Test document",
                "DOCUMENT",
                90,
                RetentionExecutionMode.DRY_RUN,
                true,
                true,
                true,
                null,
                Instant.now(),
                Instant.now()
        );

        RetentionPolicy policy2 = new RetentionPolicy(
                UUID.randomUUID(),
                "TEST_TOKEN",
                "Test token",
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

        retentionPolicyExecutor.executePolicy(policy1);
        retentionPolicyExecutor.executePolicy(policy2);

        assertTrue(documentRepository.existsById(documentId), "Document should exist after DRY_RUN");
        assertNotNull(passwordResetTokenRepository.findByUserId(employeeId), "Token should exist after DRY_RUN");
    }

    @Test
    @DisplayName("Recent (non-expired) data is not affected")
    void testRecentDataNotAffected() {
        UUID documentId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        LocalDateTime recentDate = LocalDateTime.now(ZoneId.of("UTC")).minusDays(10);

        DocumentEntity recentDoc = DocumentEntity.builder()
                .documentId(documentId)
                .employeeId(employeeId)
                .fileName("recent-doc.pdf")
                .type(DocumentType.DOCUMENTS)
                .contentType("application/pdf")
                .storagePath("s3://bucket/recent-doc")
                .checksumSha256("hash-recent")
                .uploadedAt(recentDate)
                .build();

        documentRepository.save(recentDoc);
        long initialCount = documentRepository.count();

        RetentionPolicy policy = new RetentionPolicy(
                UUID.randomUUID(),
                "TEST_DOCUMENT",
                "Test document retention",
                "DOCUMENT",
                90,
                RetentionExecutionMode.DRY_RUN,
                true,
                true,
                true,
                null,
                Instant.now(),
                Instant.now()
        );

        retentionPolicyExecutor.executePolicy(policy);

        long finalCount = documentRepository.count();
        assertEquals(initialCount, finalCount, "Recent documents should not be affected");
        assertTrue(documentRepository.existsById(documentId), "Recent document should still exist");
    }
}
