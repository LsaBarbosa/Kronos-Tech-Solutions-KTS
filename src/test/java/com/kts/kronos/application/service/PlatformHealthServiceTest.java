package com.kts.kronos.application.service;

import com.kts.kronos.adapter.out.persistence.CompanyRepository;
import com.kts.kronos.adapter.out.persistence.LgpdRequestRepository;
import com.kts.kronos.adapter.out.persistence.entity.CompanyEntity;
import com.kts.kronos.domain.model.enuns.LgpdRequestStatus;
import com.kts.kronos.observability.application.ObservabilityStatusUseCase;
import com.kts.kronos.observability.domain.ObservabilityStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformHealthServiceTest {

    @Mock
    private ObservabilityStatusUseCase observabilityStatusUseCase;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private LgpdRequestRepository lgpdRequestRepository;

    @InjectMocks
    private PlatformHealthService service;

    @Test
    @DisplayName("retorna OPERATIONAL quando health está UP e não há pendências")
    void shouldReturnOperationalWhenEverythingIsHealthy() {
        when(observabilityStatusUseCase.getStatus()).thenReturn(
                new ObservabilityStatus("kronos", "UP", "test", OffsetDateTime.now())
        );
        when(companyRepository.findByActiveTrue()).thenReturn(List.of(company(true)));
        when(companyRepository.findByActiveFalse()).thenReturn(List.of());
        when(lgpdRequestRepository.countByStatusIn(anyCollection())).thenReturn(0L);

        var response = service.getPlatformHealth();

        assertEquals("OPERATIONAL", response.state());
        assertEquals(1L, response.metrics().activeCompanies());
        assertEquals(0L, response.metrics().pendingLgpdRequests());
        assertNull(response.metrics().pendingDocuments());
    }

    @Test
    @DisplayName("retorna ATTENTION_REQUIRED quando há pendências LGPD")
    void shouldReturnAttentionRequiredWhenThereArePendingLgpdRequests() {
        when(observabilityStatusUseCase.getStatus()).thenReturn(
                new ObservabilityStatus("kronos", "UP", "test", OffsetDateTime.now())
        );
        when(companyRepository.findByActiveTrue()).thenReturn(List.of(company(true)));
        when(companyRepository.findByActiveFalse()).thenReturn(List.of(company(false)));
        when(lgpdRequestRepository.countByStatusIn(anyCollection())).thenReturn(2L);

        var response = service.getPlatformHealth();

        assertEquals("ATTENTION_REQUIRED", response.state());
        assertEquals("ATTENTION_REQUIRED", response.signals().stream()
                .filter(signal -> "pending-lgpd".equals(signal.id()))
                .findFirst()
                .orElseThrow()
                .state());
    }

    @Test
    @DisplayName("retorna ERROR quando o health check falha")
    void shouldReturnErrorWhenObservabilityFails() {
        when(observabilityStatusUseCase.getStatus()).thenThrow(new IllegalStateException("db down"));
        when(companyRepository.findByActiveTrue()).thenReturn(List.of());
        when(companyRepository.findByActiveFalse()).thenReturn(List.of());
        when(lgpdRequestRepository.countByStatusIn(anyCollection())).thenReturn(0L);

        var response = service.getPlatformHealth();

        assertEquals("ERROR", response.state());
        assertEquals("ERROR", response.signals().getFirst().state());
    }

    private CompanyEntity company(boolean active) {
        var company = new CompanyEntity();
        company.setId(UUID.randomUUID());
        company.setName(active ? "Ativa" : "Inativa");
        company.setCnpj(UUID.randomUUID().toString().replace("-", "").substring(0, 14));
        company.setEmail((active ? "ativa" : "inativa") + "@kronos.test");
        company.setActive(active);
        return company;
    }
}
