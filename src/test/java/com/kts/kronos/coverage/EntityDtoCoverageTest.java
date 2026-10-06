package com.kts.kronos.coverage;

import com.kts.kronos.adapter.in.web.dto.lgpd.*;
import com.kts.kronos.adapter.in.web.dto.security.LoginResponse;
import com.kts.kronos.adapter.in.web.dto.auth.SwitchCompanyRequest;
import com.kts.kronos.adapter.out.persistence.entity.*;
import com.kts.kronos.domain.model.*;
import com.kts.kronos.domain.model.enuns.*;
import com.kts.kronos.infrastructure.redis.RedisScopeKeyResolver;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage tests for entity classes, DTO records, and simple utility classes.
 */
class EntityDtoCoverageTest {

    // ── BlacklistedTokenEntity ────────────────────────────────────────────────

    @Test
    void blacklistedTokenEntity_toDomainAndFromDomain() {
        var domain = new BlacklistedToken("hash123", LocalDateTime.now());
        var entity = BlacklistedTokenEntity.fromDomain(domain);

        assertNotNull(entity);
        assertEquals("hash123", entity.getTokenHash());
        assertNotNull(entity.getExpiresAt());

        var back = entity.toDomain();
        assertEquals(domain.tokenHash(), back.tokenHash());
        assertEquals(domain.expiresAt(), back.expiresAt());
    }

    @Test
    void blacklistedTokenEntity_builderAndEquality() {
        var e1 = BlacklistedTokenEntity.builder().tokenHash("h1").expiresAt(LocalDateTime.now()).build();
        var e2 = BlacklistedTokenEntity.builder().tokenHash("h1").expiresAt(e1.getExpiresAt()).build();
        var e3 = BlacklistedTokenEntity.builder().tokenHash("h2").expiresAt(LocalDateTime.now()).build();

        assertEquals(e1, e2);
        assertNotEquals(e1, e3);
        assertEquals(e1.hashCode(), e2.hashCode());
        assertNotNull(e1.toString());
    }

    // ── FaqCategoryEntity ─────────────────────────────────────────────────────

    @Test
    void faqCategoryEntity_toDomain() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        FaqCategoryEntity entity = FaqCategoryEntity.builder()
                .id(id).name("Geral").description("Categoria geral")
                .createdAt(now).updatedAt(now).build();

        FaqCategory domain = entity.toDomain();

        assertEquals(id, domain.id());
        assertEquals("Geral", domain.name());
    }

    @Test
    void faqCategoryEntity_equalsHashCodeToString() {
        UUID id = UUID.randomUUID();
        FaqCategoryEntity e1 = FaqCategoryEntity.builder().id(id).name("A").build();
        FaqCategoryEntity e2 = FaqCategoryEntity.builder().id(id).name("A").build();
        FaqCategoryEntity e3 = FaqCategoryEntity.builder().id(UUID.randomUUID()).name("B").build();

        assertEquals(e1, e2);
        assertNotEquals(e1, e3);
        assertEquals(e1.hashCode(), e2.hashCode());
        assertNotNull(e1.toString());
    }

    // ── FaqArticleEntity ──────────────────────────────────────────────────────

    @Test
    void faqArticleEntity_toDomainWithNullCategory() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        FaqArticleEntity entity = FaqArticleEntity.builder()
                .id(id).title("Título").shortAnswer("Curta").fullAnswer("Longa")
                .status(FaqStatus.ACTIVE).priority(1)
                .category(null)
                .allowedRoles(new ArrayList<>(List.of(Role.MANAGER)))
                .screenKeys(new ArrayList<>(List.of("screen1")))
                .tags(new ArrayList<>(List.of("tag1")))
                .helpfulCount(5).notHelpfulCount(2)
                .createdAt(now).updatedAt(now).build();

        // toDomain() → calls toDomainWithScore(null)
        FaqArticle domain = entity.toDomain();
        assertEquals(id, domain.id());
        assertNull(domain.category()); // null category → branch null TRUE

        // toDomainWithScore with actual score
        FaqArticle withScore = entity.toDomainWithScore(0.95);
        assertEquals(0.95, withScore.relevanceScore());
    }

    @Test
    void faqArticleEntity_toDomainWithNonNullCategory() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        FaqCategoryEntity cat = FaqCategoryEntity.builder()
                .id(UUID.randomUUID()).name("Cat").build();
        FaqArticleEntity entity = FaqArticleEntity.builder()
                .id(id).title("T").shortAnswer("S").fullAnswer("F")
                .status(FaqStatus.ACTIVE).priority(1)
                .category(cat)  // non-null category → branch null FALSE
                .allowedRoles(null)   // null allowedRoles → branch null TRUE → List.of()
                .screenKeys(null)     // null screenKeys → List.of()
                .tags(null)           // null tags → List.of()
                .build();

        FaqArticle domain = entity.toDomain();
        assertNotNull(domain.category()); // category != null → branch FALSE covered
        assertTrue(domain.allowedRoles().isEmpty()); // null list → List.of()
    }

    @Test
    void faqArticleEntity_equalsHashCodeToString_andNoArgConstructor() {
        UUID id = UUID.randomUUID();
        FaqArticleEntity e1 = new FaqArticleEntity();
        e1.setId(id);
        e1.setTitle("T1");
        FaqArticleEntity e2 = new FaqArticleEntity();
        e2.setId(id);
        e2.setTitle("T1");
        FaqArticleEntity e3 = new FaqArticleEntity();
        e3.setId(UUID.randomUUID());

        assertEquals(e1, e2);
        assertNotEquals(e1, e3);
        assertEquals(e1.hashCode(), e2.hashCode());
        assertNotNull(e1.toString());
    }

    // ── AuditLogEntity ────────────────────────────────────────────────────────

    @Test
    void auditLogEntity_builderAndGettersSetters() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        AuditLogEntity entity = AuditLogEntity.builder()
                .id(id).actorUserId(userId).action("TEST_ACTION")
                .timestamp(now).companyId(UUID.randomUUID())
                .resourceType("EMPLOYEE").resourceId("emp-1")
                .details("Details").ipAddress("127.0.0.1").userAgent("JUnit")
                .riskLevel("LOW").correlationId("corr-1")
                .build();

        assertEquals(id, entity.getId());
        assertEquals(userId, entity.getActorUserId());
        assertEquals("TEST_ACTION", entity.getAction());
        assertEquals(now, entity.getTimestamp());
        assertEquals("127.0.0.1", entity.getIpAddress());

        // test setters
        entity.setRiskLevel("HIGH");
        assertEquals("HIGH", entity.getRiskLevel());
        entity.setMinimizedAt(now);
        assertEquals(now, entity.getMinimizedAt());
    }

    @Test
    void auditLogEntity_noArgConstructorAndAllArgConstructor() {
        var noArg = new AuditLogEntity();
        assertNull(noArg.getId());

        UUID id = UUID.randomUUID();
        var allArg = new AuditLogEntity(id, UUID.randomUUID(), UUID.randomUUID(), "ACTION",
                "1.2.3.4", "UA", "details", LocalDateTime.now(), UUID.randomUUID(),
                "EMPLOYEE", "res-1", "corr-1", "MEDIUM", null);
        assertEquals(id, allArg.getId());
    }

    // ── UserCompanyAccessEntity ───────────────────────────────────────────────

    @Test
    void userCompanyAccessEntity_toDomainFromDomain_equalsHashCodeToString() {
        UserCompanyAccess access = new UserCompanyAccess(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "MANAGER", true, false, LocalDateTime.now(), null
        );
        UserCompanyAccessEntity entity = UserCompanyAccessEntity.fromDomain(access);
        UserCompanyAccess back = entity.toDomain();

        assertEquals(access.accessId(), back.accessId());
        assertEquals(access.role(), back.role());
        assertTrue(back.active());
        assertFalse(back.defaultCompany());

        // Lombok @Data: equals/hashCode/toString
        UserCompanyAccessEntity e2 = UserCompanyAccessEntity.fromDomain(access);
        assertEquals(entity, e2);
        assertEquals(entity.hashCode(), e2.hashCode());
        assertNotNull(entity.toString());
    }

    @Test
    void userCompanyAccessEntity_noArgConstructorAndBuilder() {
        var noArg = new UserCompanyAccessEntity();
        assertNull(noArg.getAccessId());

        UUID id = UUID.randomUUID();
        var built = UserCompanyAccessEntity.builder()
                .accessId(id).userId(UUID.randomUUID()).companyId(UUID.randomUUID())
                .role("CTO").active(true).defaultCompany(true)
                .createdAt(LocalDateTime.now()).build();
        assertEquals(id, built.getAccessId());
        assertTrue(built.isActive());
        assertTrue(built.isDefaultCompany());
    }

    // ── MessageDeliveryEntity ─────────────────────────────────────────────────

    @Test
    void messageDeliveryEntity_fromDomainAndGetters() {
        UUID msgId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        var delivery = new MessageDelivery(UUID.randomUUID(), msgId, empId, now, null);
        var msgEntity = new MessageEntity();  // fromDomain just stores the reference

        MessageDeliveryEntity entity = MessageDeliveryEntity.fromDomain(delivery, msgEntity);

        assertNotNull(entity);
        assertEquals(empId, entity.getRecipientEmployeeId());
        assertNull(entity.getSeenAt()); // seenAt=null
    }

    // ── Simple DTO records ───────────────────────────────────────────────────

    @Test
    void loginResponse_instantiation() {
        @SuppressWarnings("deprecation")
        var r = new LoginResponse(true);
        assertTrue(r.authenticated());
    }

    @Test
    void switchCompanyRequest_instantiation() {
        UUID id = UUID.randomUUID();
        var r = new SwitchCompanyRequest(id);
        assertEquals(id, r.companyId());
        assertEquals(r, new SwitchCompanyRequest(id));
        assertNotNull(r.toString());
    }

    @Test
    void cancelRequestRequest_instantiation() {
        var r = new CancelRequestRequest("motivo");
        assertEquals("motivo", r.reason());
        assertNotNull(r.toString());
    }

    @Test
    void requestComplementRequest_instantiation() {
        var r = new RequestComplementRequest("mensagem complemento");
        assertEquals("mensagem complemento", r.message());
    }

    @Test
    void dataProcessingPurposeResponse_fromDomain() {
        var purpose = new DataProcessingPurpose(
                "CODE_01", DataCategory.IDENTIFICATION, LegalBasis.CONSENT, "Finalidade teste",
                "RETENTION_01", false, true
        );
        var response = DataProcessingPurposeResponse.fromDomain(purpose);

        assertEquals("CODE_01", response.code());
        assertEquals(DataCategory.IDENTIFICATION, response.dataCategory());
        assertEquals(LegalBasis.CONSENT, response.legalBasis());
        assertTrue(response.active());
        assertFalse(response.sensitive());
    }

    @Test
    void dataProcessingPurposeResponse_equalsAndHashCode() {
        var p = new DataProcessingPurpose("C", DataCategory.IDENTIFICATION, LegalBasis.CONSENT, "P", "R", false, true);
        var r1 = DataProcessingPurposeResponse.fromDomain(p);
        var r2 = DataProcessingPurposeResponse.fromDomain(p);
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
        assertNotNull(r1.toString());
    }

    @Test
    void anonymizationApplyRequest_instantiationAndAccessors() {
        UUID token = UUID.randomUUID();
        var req = new AnonymizationApplyRequest("justificativa", Boolean.TRUE, token);
        assertEquals("justificativa", req.justification());
        assertTrue(req.confirmed());
        assertEquals(token, req.dryRunToken());
        assertEquals(req, new AnonymizationApplyRequest("justificativa", Boolean.TRUE, token));
        assertNotNull(req.toString());
    }

    // ── RedisScopeKeyResolver (static utility class) ─────────────────────────

    @Test
    void redisScopeKeyResolver_staticMethods() {
        String authScope = RedisScopeKeyResolver.authenticatedScope("key1", "key2");
        assertNotNull(authScope);
        assertFalse(authScope.isBlank());

        String pubScope = RedisScopeKeyResolver.publicScope("key1");
        assertNotNull(pubScope);
        assertFalse(pubScope.isBlank());
    }
}
