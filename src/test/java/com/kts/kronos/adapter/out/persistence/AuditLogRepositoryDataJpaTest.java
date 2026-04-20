package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.AuditLogEntity;
import com.kts.kronos.support.jpa.AbstractPostgresDataJpaTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditLogRepositoryDataJpaTest extends AbstractPostgresDataJpaTest {

    @Autowired
    private AuditLogRepository repository;

    @Test
    @DisplayName("JpaRepository: deve persistir e buscar log de auditoria")
    void shouldSaveAndFindAuditLog() {
        AuditLogEntity saved = repository.save(AuditLogEntity.builder()
                .userId(UUID.randomUUID())
                .action("DELETE_DOCUMENT")
                .ipAddress("127.0.0.1")
                .userAgent("JUnit")
                .details("remoção solicitada")
                .timestamp(LocalDateTime.of(2026, 4, 20, 10, 0))
                .build());

        assertEquals(1, repository.count());
        assertTrue(repository.findById(saved.getId()).isPresent());
        assertEquals("DELETE_DOCUMENT", repository.findById(saved.getId()).orElseThrow().getAction());
    }
}
