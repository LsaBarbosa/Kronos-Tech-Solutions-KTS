package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.LegalConsentRepository;
import com.kts.kronos.adapter.out.persistence.entity.LegalConsentEntity;
import com.kts.kronos.adapter.out.persistence.mapper.LegalConsentMapper;
import com.kts.kronos.domain.model.LegalConsent;
import com.kts.kronos.domain.model.enuns.ConsentType;
import com.kts.kronos.domain.model.enuns.LegalBasis;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalConsentProviderImplTest {

    @Mock
    private LegalConsentRepository repository;

    @Spy
    private LegalConsentMapper mapper;

    @InjectMocks
    private LegalConsentProviderImpl provider;

    @Test
    void shouldSaveConsent() {
        var consent = domain(null);
        var entity = entity(consent);
        when(repository.save(any(LegalConsentEntity.class))).thenReturn(entity);

        var saved = provider.save(consent);

        assertEquals(consent.consentId(), saved.consentId());
        verify(repository).save(any(LegalConsentEntity.class));
    }

    @Test
    void shouldFindActiveConsent() {
        var consent = domain(null);
        when(repository.findByEmployeeIdAndConsentTypeAndRevokedAtIsNull(
                consent.employeeId(),
                ConsentType.BIOMETRIC_AUTHENTICATION
        )).thenReturn(Optional.of(entity(consent)));

        var found = provider.findActive(consent.employeeId(), ConsentType.BIOMETRIC_AUTHENTICATION);

        assertTrue(found.isPresent());
        assertEquals(consent.evidenceDocumentId(), found.orElseThrow().evidenceDocumentId());
    }

    @Test
    void shouldReturnExistsActiveFromRepository() {
        UUID employeeId = UUID.randomUUID();
        when(repository.existsByEmployeeIdAndConsentTypeAndRevokedAtIsNull(
                employeeId,
                ConsentType.BIOMETRIC_AUTHENTICATION
        )).thenReturn(true);

        assertTrue(provider.existsActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION));
    }

    @Test
    void shouldListHistoryByEmployee() {
        var consent = domain(null);
        when(repository.findByEmployeeIdOrderByGrantedAtDesc(consent.employeeId()))
                .thenReturn(List.of(entity(consent)));

        var history = provider.findAllByEmployeeId(consent.employeeId());

        assertEquals(1, history.size());
        assertEquals(consent.version(), history.getFirst().version());
    }

    private static LegalConsent domain(Instant revokedAt) {
        return new LegalConsent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                ConsentType.BIOMETRIC_AUTHENTICATION,
                LegalBasis.CONSENT,
                "Biometric authentication",
                "2026.05.21",
                Instant.parse("2026-05-21T09:00:00Z"),
                revokedAt,
                "127.0.0.1",
                "JUnit",
                UUID.randomUUID(),
                "hash",
                Instant.parse("2026-05-21T09:00:00Z"),
                null
        );
    }

    private static LegalConsentEntity entity(LegalConsent consent) {
        return LegalConsentEntity.builder()
                .consentId(consent.consentId())
                .employeeId(consent.employeeId())
                .userId(consent.userId())
                .consentType(consent.consentType())
                .legalBasis(consent.legalBasis())
                .purpose(consent.purpose())
                .version(consent.version())
                .grantedAt(consent.grantedAt())
                .revokedAt(consent.revokedAt())
                .ipAddress(consent.ipAddress())
                .userAgent(consent.userAgent())
                .evidenceDocumentId(consent.evidenceDocumentId())
                .evidenceHashSha256(consent.evidenceHashSha256())
                .createdAt(consent.createdAt())
                .updatedAt(consent.updatedAt())
                .build();
    }
}
