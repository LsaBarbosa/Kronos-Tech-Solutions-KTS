package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.enuns.DocumentType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegalTextTest {

    @Test
    void shouldExposeAllFields() {
        UUID legalTextId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-05-21T10:00:00Z");
        Instant publishedAt = Instant.parse("2026-05-21T10:05:00Z");

        LegalText legalText = new LegalText(
                legalTextId,
                DocumentType.BIOMETRIC_CONSENT_TERM,
                "2026.05.21",
                "Termo de Consentimento Biométrico",
                "Conteúdo",
                "hash",
                true,
                createdAt,
                publishedAt
        );

        assertEquals(legalTextId, legalText.legalTextId());
        assertEquals(DocumentType.BIOMETRIC_CONSENT_TERM, legalText.documentType());
        assertEquals("2026.05.21", legalText.version());
        assertEquals("Termo de Consentimento Biométrico", legalText.title());
        assertEquals("Conteúdo", legalText.content());
        assertEquals("hash", legalText.contentHashSha256());
        assertTrue(legalText.active());
        assertEquals(createdAt, legalText.createdAt());
        assertEquals(publishedAt, legalText.publishedAt());
    }
}
