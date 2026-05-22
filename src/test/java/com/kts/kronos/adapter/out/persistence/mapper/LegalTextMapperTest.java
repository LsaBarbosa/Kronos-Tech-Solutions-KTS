package com.kts.kronos.adapter.out.persistence.mapper;

import com.kts.kronos.adapter.out.persistence.entity.LegalTextEntity;
import com.kts.kronos.domain.model.LegalText;
import com.kts.kronos.domain.model.enuns.DocumentType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegalTextMapperTest {

    private final LegalTextMapper mapper = new LegalTextMapper();

    @Test
    void shouldMapEntityToDomain() {
        UUID legalTextId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-05-21T10:00:00Z");
        Instant publishedAt = Instant.parse("2026-05-21T10:05:00Z");
        LegalTextEntity entity = LegalTextEntity.builder()
                .legalTextId(legalTextId)
                .documentType(DocumentType.BIOMETRIC_CONSENT_TERM)
                .version("2026.05.21")
                .title("Termo")
                .content("Conteúdo")
                .contentHashSha256("hash")
                .active(true)
                .createdAt(createdAt)
                .publishedAt(publishedAt)
                .build();

        LegalText domain = mapper.toDomain(entity);

        assertEquals(legalTextId, domain.legalTextId());
        assertEquals(DocumentType.BIOMETRIC_CONSENT_TERM, domain.documentType());
        assertEquals("2026.05.21", domain.version());
        assertEquals("Termo", domain.title());
        assertEquals("Conteúdo", domain.content());
        assertEquals("hash", domain.contentHashSha256());
        assertTrue(domain.active());
        assertEquals(createdAt, domain.createdAt());
        assertEquals(publishedAt, domain.publishedAt());
    }

    @Test
    void shouldMapDomainToEntity() {
        UUID legalTextId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-05-21T10:00:00Z");
        Instant publishedAt = Instant.parse("2026-05-21T10:05:00Z");
        LegalText domain = new LegalText(
                legalTextId,
                DocumentType.BIOMETRIC_CONSENT_TERM,
                "2026.05.21",
                "Termo",
                "Conteúdo",
                "hash",
                true,
                createdAt,
                publishedAt
        );

        LegalTextEntity entity = mapper.toEntity(domain);

        assertEquals(legalTextId, entity.getLegalTextId());
        assertEquals(DocumentType.BIOMETRIC_CONSENT_TERM, entity.getDocumentType());
        assertEquals("2026.05.21", entity.getVersion());
        assertEquals("Termo", entity.getTitle());
        assertEquals("Conteúdo", entity.getContent());
        assertEquals("hash", entity.getContentHashSha256());
        assertTrue(entity.isActive());
        assertEquals(createdAt, entity.getCreatedAt());
        assertEquals(publishedAt, entity.getPublishedAt());
    }
}
