package com.kts.kronos.adapter.out.persistence.mapper;

import com.kts.kronos.adapter.out.persistence.entity.DataProcessingInventoryEntity;
import com.kts.kronos.domain.model.DataProcessingInventory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DataProcessingInventoryMapperTest {

    private final DataProcessingInventoryMapper mapper = new DataProcessingInventoryMapper();

    @Test
    void shouldMapEntityToDomain() {
        var entity = entity();

        var domain = mapper.toDomain(entity);

        assertEquals(entity.getInventoryId(), domain.inventoryId());
        assertEquals(entity.getProcessCode(), domain.processCode());
        assertEquals(entity.getProcessName(), domain.processName());
        assertEquals(entity.getDescription(), domain.description());
        assertEquals(entity.getDataCategory(), domain.dataCategory());
        assertEquals(entity.getDataFields(), domain.dataFields());
        assertEquals(entity.getDataSubjectCategory(), domain.dataSubjectCategory());
        assertEquals(entity.getPurpose(), domain.purpose());
        assertEquals(entity.getLegalBasis(), domain.legalBasis());
        assertEquals(entity.getSensitiveData(), domain.sensitiveData());
        assertEquals(entity.getSourceSystem(), domain.sourceSystem());
        assertEquals(entity.getStorageLocation(), domain.storageLocation());
        assertEquals(entity.getRetentionPolicyCode(), domain.retentionPolicyCode());
        assertEquals(entity.getExternalSharing(), domain.externalSharing());
        assertEquals(entity.getInternationalTransfer(), domain.internationalTransfer());
        assertEquals(entity.getSecurityMeasures(), domain.securityMeasures());
        assertEquals(entity.getActive(), domain.active());
        assertEquals(entity.getRiskLevel(), domain.riskLevel());
        assertEquals(entity.getRipdRequired(), domain.ripdRequired());
        assertEquals(entity.getVersion(), domain.version());
        assertEquals(entity.getOperators(), domain.operators());
        assertEquals(entity.getCreatedAt(), domain.createdAt());
        assertEquals(entity.getUpdatedAt(), domain.updatedAt());
    }

    @Test
    void shouldMapDomainToEntity() {
        var domain = domain();

        var entity = mapper.toEntity(domain);

        assertEquals(domain.inventoryId(), entity.getInventoryId());
        assertEquals(domain.processCode(), entity.getProcessCode());
        assertEquals(domain.processName(), entity.getProcessName());
        assertEquals(domain.description(), entity.getDescription());
        assertEquals(domain.dataCategory(), entity.getDataCategory());
        assertEquals(domain.dataFields(), entity.getDataFields());
        assertEquals(domain.dataSubjectCategory(), entity.getDataSubjectCategory());
        assertEquals(domain.purpose(), entity.getPurpose());
        assertEquals(domain.legalBasis(), entity.getLegalBasis());
        assertEquals(domain.sensitiveData(), entity.getSensitiveData());
        assertEquals(domain.sourceSystem(), entity.getSourceSystem());
        assertEquals(domain.storageLocation(), entity.getStorageLocation());
        assertEquals(domain.retentionPolicyCode(), entity.getRetentionPolicyCode());
        assertEquals(domain.externalSharing(), entity.getExternalSharing());
        assertEquals(domain.internationalTransfer(), entity.getInternationalTransfer());
        assertEquals(domain.securityMeasures(), entity.getSecurityMeasures());
        assertEquals(domain.active(), entity.getActive());
        assertEquals(domain.riskLevel(), entity.getRiskLevel());
        assertEquals(domain.ripdRequired(), entity.getRipdRequired());
        assertEquals(domain.version(), entity.getVersion());
        assertEquals(domain.operators(), entity.getOperators());
        assertEquals(domain.createdAt(), entity.getCreatedAt());
        assertEquals(domain.updatedAt(), entity.getUpdatedAt());
    }

    @Test
    void shouldRoundTripDomainThroughEntity() {
        var original = domain();

        assertEquals(original, mapper.toDomain(mapper.toEntity(original)));
    }

    @Test
    void shouldPreserveNullableFields() {
        var entity = DataProcessingInventoryEntity.builder()
                .inventoryId(UUID.randomUUID())
                .processCode("PROC-001")
                .processName("Processo mínimo")
                .active(true)
                .build();

        var domain = mapper.toDomain(entity);

        assertNull(domain.description());
        assertNull(domain.sensitiveData());
        assertNull(domain.internationalTransfer());
        assertNull(domain.ripdRequired());
        assertNull(domain.createdAt());
        assertNull(domain.updatedAt());
    }

    private DataProcessingInventory domain() {
        Instant now = Instant.parse("2026-03-10T12:00:00Z");
        return new DataProcessingInventory(
                UUID.fromString("11110000-0000-0000-0000-000000000001"),
                "PROC-PONTO-001",
                "Registro de Ponto",
                "Controle de jornada dos colaboradores",
                "BIOMETRICO",
                "hora_entrada, hora_saida, foto_facial",
                "COLABORADOR",
                "Controle de ponto e folha de pagamento",
                "CONTRATO",
                true,
                "Sistema KTS",
                "PostgreSQL + S3",
                "RETENTION_5Y",
                "NAO",
                false,
                "Criptografia AES-256, TLS 1.3",
                true,
                "MEDIO",
                false,
                "v2.1",
                "Hostinger",
                now,
                now.plusSeconds(3600)
        );
    }

    private DataProcessingInventoryEntity entity() {
        Instant now = Instant.parse("2026-03-10T12:00:00Z");
        return DataProcessingInventoryEntity.builder()
                .inventoryId(UUID.fromString("11110000-0000-0000-0000-000000000001"))
                .processCode("PROC-PONTO-001")
                .processName("Registro de Ponto")
                .description("Controle de jornada dos colaboradores")
                .dataCategory("BIOMETRICO")
                .dataFields("hora_entrada, hora_saida, foto_facial")
                .dataSubjectCategory("COLABORADOR")
                .purpose("Controle de ponto e folha de pagamento")
                .legalBasis("CONTRATO")
                .sensitiveData(true)
                .sourceSystem("Sistema KTS")
                .storageLocation("PostgreSQL + S3")
                .retentionPolicyCode("RETENTION_5Y")
                .externalSharing("NAO")
                .internationalTransfer(false)
                .securityMeasures("Criptografia AES-256, TLS 1.3")
                .active(true)
                .riskLevel("MEDIO")
                .ripdRequired(false)
                .version("v2.1")
                .operators("Hostinger")
                .createdAt(now)
                .updatedAt(now.plusSeconds(3600))
                .build();
    }
}
