package com.kts.kronos.adapter.out.persistence.mapper;

import com.kts.kronos.adapter.out.persistence.entity.ServiceContractEntity;
import com.kts.kronos.domain.model.ServiceContract;
import com.kts.kronos.domain.model.enuns.ServiceContractStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ServiceContractMapperTest {

    @Test
    void shouldMapEntityToDomain() {
        var entity = entity();
        var domain = ServiceContractMapper.toDomain(entity);

        assertEquals(entity.getContractId(), domain.contractId());
        assertEquals(entity.getCompanyId(), domain.companyId());
        assertEquals(entity.getSourceDocumentId(), domain.sourceDocumentId());
        assertEquals(entity.getSourceDocumentOwnerEmployeeId(), domain.sourceDocumentOwnerEmployeeId());
        assertEquals(entity.getCreatedByUserId(), domain.createdByUserId());
        assertEquals(entity.getCreatedByEmployeeId(), domain.createdByEmployeeId());
        assertEquals(entity.getTitle(), domain.title());
        assertEquals(entity.getDescription(), domain.description());
        assertEquals(entity.getOriginalFileName(), domain.originalFileName());
        assertEquals(entity.getDocumentHashSha256(), domain.documentHashSha256());
        assertEquals(entity.getStatus(), domain.status());
        assertEquals(entity.getCreatedAt(), domain.createdAt());
        assertEquals(entity.getUpdatedAt(), domain.updatedAt());
        assertEquals(entity.getVoidedAt(), domain.voidedAt());
        assertEquals(entity.getVoidedByUserId(), domain.voidedByUserId());
        assertEquals(entity.getVoidReason(), domain.voidReason());
    }

    @Test
    void shouldMapDomainToEntity() {
        var domain = domain();
        var entity = ServiceContractMapper.toEntity(domain);

        assertEquals(domain.contractId(), entity.getContractId());
        assertEquals(domain.title(), entity.getTitle());
        assertEquals(domain.status(), entity.getStatus());
        assertEquals(domain.voidReason(), entity.getVoidReason());
    }

    @Test
    void shouldRoundTrip() {
        var original = domain();
        assertEquals(original, ServiceContractMapper.toDomain(ServiceContractMapper.toEntity(original)));
    }

    @Test
    void shouldReturnNullForNullEntity() {
        assertNull(ServiceContractMapper.toDomain(null));
    }

    @Test
    void shouldReturnNullForNullDomain() {
        assertNull(ServiceContractMapper.toEntity(null));
    }

    private ServiceContract domain() {
        Instant now = Instant.parse("2026-04-01T10:00:00Z");
        return new ServiceContract(
                UUID.fromString("aaaaaaaa-0001-0001-0001-000000000001"),
                UUID.fromString("bbbbbbbb-0002-0002-0002-000000000002"),
                UUID.fromString("cccccccc-0003-0003-0003-000000000003"),
                UUID.fromString("dddddddd-0004-0004-0004-000000000004"),
                UUID.fromString("eeeeeeee-0005-0005-0005-000000000005"),
                UUID.fromString("ffffffff-0006-0006-0006-000000000006"),
                "Contrato de Prestação de Serviços",
                "Descrição do contrato",
                "contrato.pdf",
                "abc123def456",
                ServiceContractStatus.ACTIVE,
                now,
                now.plusSeconds(60),
                null,
                null,
                null
        );
    }

    private ServiceContractEntity entity() {
        Instant now = Instant.parse("2026-04-01T10:00:00Z");
        return ServiceContractEntity.builder()
                .contractId(UUID.fromString("aaaaaaaa-0001-0001-0001-000000000001"))
                .companyId(UUID.fromString("bbbbbbbb-0002-0002-0002-000000000002"))
                .sourceDocumentId(UUID.fromString("cccccccc-0003-0003-0003-000000000003"))
                .sourceDocumentOwnerEmployeeId(UUID.fromString("dddddddd-0004-0004-0004-000000000004"))
                .createdByUserId(UUID.fromString("eeeeeeee-0005-0005-0005-000000000005"))
                .createdByEmployeeId(UUID.fromString("ffffffff-0006-0006-0006-000000000006"))
                .title("Contrato de Prestação de Serviços")
                .description("Descrição do contrato")
                .originalFileName("contrato.pdf")
                .documentHashSha256("abc123def456")
                .status(ServiceContractStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now.plusSeconds(60))
                .build();
    }
}
