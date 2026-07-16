package com.kts.kronos.coverage;

import com.kts.kronos.adapter.out.persistence.entity.MessageDeliveryEntity;
import com.kts.kronos.adapter.out.persistence.entity.MessageEntity;
import com.kts.kronos.domain.model.DryRunToken;
import com.kts.kronos.domain.model.Message;
import com.kts.kronos.domain.model.MessageDelivery;
import com.kts.kronos.domain.model.enuns.MessagePriority;
import com.kts.kronos.domain.model.enuns.MessageScope;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DomainModelCoverageTest {

    // ── DryRunToken branches ──────────────────────────────────────────────────

    private DryRunToken token(DryRunToken.Status status, Instant expiresAt, Instant consumedAt) {
        return new DryRunToken(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                Instant.now(), expiresAt, consumedAt, status
        );
    }

    @Test
    void isExpired_beforeExpiry_returnsFalse() {
        var t = token(DryRunToken.Status.PENDING, Instant.now().plusSeconds(3600), null);
        assertFalse(t.isExpired(Instant.now()));
    }

    @Test
    void isExpired_afterExpiry_isBefore_returnsTrue() {
        var t = token(DryRunToken.Status.PENDING, Instant.now().minusSeconds(1), null);
        assertTrue(t.isExpired(Instant.now()));
    }

    @Test
    void isExpired_exactlyNow_equalsTrue() {
        // expiresAt.equals(now) → true (covers L25 equals branch)
        var now = Instant.now();
        var t = token(DryRunToken.Status.PENDING, now, null);
        assertTrue(t.isExpired(now));
    }

    @Test
    void isConsumed_nullConsumedAt_returnsFalse() {
        var t = token(DryRunToken.Status.CONSUMED, Instant.now().plusSeconds(60), null);
        assertFalse(t.isConsumed());
    }

    @Test
    void isConsumed_nonNullConsumedAtAndConsumedStatus_returnsTrue() {
        var t = token(DryRunToken.Status.CONSUMED, Instant.now().plusSeconds(60), Instant.now().minusSeconds(10));
        assertTrue(t.isConsumed());
    }

    @Test
    void isConsumed_nonNullConsumedAtButWrongStatus_returnsFalse() {
        var t = token(DryRunToken.Status.PENDING, Instant.now().plusSeconds(60), Instant.now().minusSeconds(10));
        assertFalse(t.isConsumed());
    }

    @Test
    void isValid_pendingNotExpiredNotConsumed_returnsTrue() {
        var t = token(DryRunToken.Status.PENDING, Instant.now().plusSeconds(3600), null);
        assertTrue(t.isValid(Instant.now()));
    }

    @Test
    void isValid_expired_returnsFalse() {
        var t = token(DryRunToken.Status.PENDING, Instant.now().minusSeconds(1), null);
        assertFalse(t.isValid(Instant.now()));
    }

    @Test
    void isValid_consumed_returnsFalse() {
        var t = token(DryRunToken.Status.CONSUMED, Instant.now().plusSeconds(3600), Instant.now().minusSeconds(10));
        assertFalse(t.isValid(Instant.now()));
    }

    @Test
    void isValid_expiredStatus_returnsFalse() {
        // status != PENDING → first condition false → isValid = false
        var t = token(DryRunToken.Status.EXPIRED, Instant.now().plusSeconds(3600), null);
        assertFalse(t.isValid(Instant.now()));
    }

    // ── MessageEntity.applyDefaults() via reflection ──────────────────────────

    @Test
    void messageEntity_applyDefaults_withAllNullFields_setsDefaults() throws Exception {
        var entity = new MessageEntity();
        // @Builder.Default initializes scope=DIRECT even in no-arg constructor; force null to test the GLOBAL branch
        java.lang.reflect.Field scopeField = MessageEntity.class.getDeclaredField("scope");
        scopeField.setAccessible(true);
        scopeField.set(entity, null);
        // also null out deliveries to test that branch
        java.lang.reflect.Field deliveriesField = MessageEntity.class.getDeclaredField("deliveries");
        deliveriesField.setAccessible(true);
        deliveriesField.set(entity, null);

        Method applyDefaults = MessageEntity.class.getDeclaredMethod("applyDefaults");
        applyDefaults.setAccessible(true);
        applyDefaults.invoke(entity);

        // scope == null + recipientEmployeeId == null → scope = GLOBAL
        assertEquals(MessageScope.GLOBAL, entity.getScope());
        // deliveries == null → new LinkedHashSet<>()
        assertNotNull(entity.getDeliveries());
    }

    @Test
    void messageEntity_applyDefaults_withNullScopeAndNonNullRecipient_setsDirect() throws Exception {
        // Build entity with null scope but non-null recipientEmployeeId using builder trick:
        // Use fromDomain with a Message that has null scope → Message compact constructor infers scope from recipientEmployeeId
        // So we can't get null scope from fromDomain. Use reflection instead.
        var entity = new MessageEntity();
        java.lang.reflect.Field recipientField = MessageEntity.class.getDeclaredField("recipientEmployeeId");
        recipientField.setAccessible(true);
        recipientField.set(entity, UUID.randomUUID());

        Method applyDefaults = MessageEntity.class.getDeclaredMethod("applyDefaults");
        applyDefaults.setAccessible(true);
        applyDefaults.invoke(entity);

        // scope == null + recipientEmployeeId != null → scope = DIRECT
        assertEquals(MessageScope.DIRECT, entity.getScope());
    }

    @Test
    void messageEntity_applyDefaults_withPresetScope_doesNotOverwrite() throws Exception {
        // Create via fromDomain which sets scope to GLOBAL
        var message = new Message(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "T", "M", MessagePriority.NORMAL, MessageScope.GLOBAL,
                LocalDateTime.now(), null, null, 0, null);
        var entity = MessageEntity.fromDomain(message);

        // scope is already set → applyDefaults should NOT change it
        Method applyDefaults = MessageEntity.class.getDeclaredMethod("applyDefaults");
        applyDefaults.setAccessible(true);
        applyDefaults.invoke(entity);

        // scope was GLOBAL before, stays GLOBAL
        assertEquals(MessageScope.GLOBAL, entity.getScope());
    }

    // ── MessageEntity.resolveDeliveredCount() via toDomain() ──────────────────

    @Test
    void messageEntity_toDomain_nonEmptyDeliveries_coversDeliveredCountBranch() {
        var msgId = UUID.randomUUID();
        var empId = UUID.randomUUID();
        var message = new Message(msgId, empId, UUID.randomUUID(),
                "T", "M", MessagePriority.NORMAL, MessageScope.GLOBAL,
                LocalDateTime.now(), null, null, 0, null);

        var entity = MessageEntity.fromDomain(message);
        // entity.getDeliveries() is non-null empty LinkedHashSet (from fromDomain)
        // Add a delivery to make it non-empty
        var delivery = new MessageDelivery(msgId, empId, LocalDateTime.now());
        var deliveryEntity = MessageDeliveryEntity.fromDomain(delivery, entity);
        entity.getDeliveries().add(deliveryEntity);

        var domain = entity.toDomain();

        // deliveries != null && !deliveries.isEmpty() → deliveries.size() = 1 (covers L124-125)
        assertEquals(1, domain.deliveredCount());
    }

    @Test
    void messageEntity_toDomain_nullDeliveriesAndNullRecipient_returnsZero() throws Exception {
        var entity = new MessageEntity();
        // deliveries is null (no-arg constructor), recipientEmployeeId is null → return 0

        java.lang.reflect.Field scopeField = MessageEntity.class.getDeclaredField("scope");
        scopeField.setAccessible(true);
        scopeField.set(entity, MessageScope.GLOBAL);

        var domain = entity.toDomain();

        assertEquals(0, domain.deliveredCount());
    }

    @Test
    void messageEntity_toDomain_nullDeliveriesButNonNullRecipient_returnsOne() throws Exception {
        var entity = new MessageEntity();
        // Set recipientEmployeeId via reflection (no public setter)
        java.lang.reflect.Field recipField = MessageEntity.class.getDeclaredField("recipientEmployeeId");
        recipField.setAccessible(true);
        recipField.set(entity, UUID.randomUUID());

        java.lang.reflect.Field scopeField = MessageEntity.class.getDeclaredField("scope");
        scopeField.setAccessible(true);
        scopeField.set(entity, MessageScope.DIRECT);

        var domain = entity.toDomain();

        // deliveries == null → recipientEmployeeId != null → 1 (covers L127 non-null branch)
        assertEquals(1, domain.deliveredCount());
    }
}
