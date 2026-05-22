package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.LegalTextRepository;
import com.kts.kronos.adapter.out.persistence.entity.LegalTextEntity;
import com.kts.kronos.adapter.out.persistence.mapper.LegalTextMapper;
import com.kts.kronos.domain.model.enuns.DocumentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalTextProviderImplTest {

    @Mock
    private LegalTextRepository repository;

    private final LegalTextMapper mapper = new LegalTextMapper();

    @Test
    void shouldFindActiveLegalTextByDocumentType() {
        UUID legalTextId = UUID.randomUUID();
        LegalTextEntity entity = LegalTextEntity.builder()
                .legalTextId(legalTextId)
                .documentType(DocumentType.BIOMETRIC_CONSENT_TERM)
                .version("2026.05.21")
                .title("Termo")
                .content("Conteúdo")
                .contentHashSha256("hash")
                .active(true)
                .createdAt(Instant.parse("2026-05-21T10:00:00Z"))
                .publishedAt(Instant.parse("2026-05-21T10:05:00Z"))
                .build();
        var provider = new LegalTextProviderImpl(repository, mapper);
        when(repository.findByDocumentTypeAndActiveTrue(DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(Optional.of(entity));

        var result = provider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM);

        assertTrue(result.isPresent());
        assertEquals(legalTextId, result.get().legalTextId());
        assertEquals("2026.05.21", result.get().version());
    }

    @Test
    void shouldFindLegalTextByDocumentTypeAndVersion() {
        LegalTextEntity entity = LegalTextEntity.builder()
                .legalTextId(UUID.randomUUID())
                .documentType(DocumentType.BIOMETRIC_CONSENT_TERM)
                .version("2026.05.21")
                .title("Termo")
                .content("Conteúdo")
                .contentHashSha256("hash")
                .active(true)
                .createdAt(Instant.parse("2026-05-21T10:00:00Z"))
                .publishedAt(Instant.parse("2026-05-21T10:05:00Z"))
                .build();
        var provider = new LegalTextProviderImpl(repository, mapper);
        when(repository.findByDocumentTypeAndVersion(DocumentType.BIOMETRIC_CONSENT_TERM, "2026.05.21"))
                .thenReturn(Optional.of(entity));

        var result = provider.findByDocumentTypeAndVersion(DocumentType.BIOMETRIC_CONSENT_TERM, "2026.05.21");

        assertTrue(result.isPresent());
        assertEquals("hash", result.get().contentHashSha256());
    }
}
