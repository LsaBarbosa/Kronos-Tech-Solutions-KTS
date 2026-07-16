package com.kts.kronos.adapter.out.persistence.mapper;

import com.kts.kronos.adapter.out.persistence.entity.ServiceContractSignatureEntity;
import com.kts.kronos.domain.model.ServiceContractSignature;
import com.kts.kronos.domain.model.enuns.ContractSignatureMethod;
import com.kts.kronos.domain.model.enuns.ContractSignatureStatus;
import com.kts.kronos.domain.model.enuns.ContractSignatureType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ServiceContractSignatureMapperTest {

    @Test
    void shouldMapEntityToDomain() {
        var entity = entity();
        var domain = ServiceContractSignatureMapper.toDomain(entity);

        assertEquals(entity.getSignatureId(), domain.signatureId());
        assertEquals(entity.getAssignmentId(), domain.assignmentId());
        assertEquals(entity.getContractId(), domain.contractId());
        assertEquals(entity.getEmployeeId(), domain.employeeId());
        assertEquals(entity.getCompanyId(), domain.companyId());
        assertEquals(entity.getSignerUserId(), domain.signerUserId());
        assertEquals(entity.getSignedAt(), domain.signedAt());
        assertEquals(entity.getSignedAtZone(), domain.signedAtZone());
        assertEquals(entity.getSignatureType(), domain.signatureType());
        assertEquals(entity.getSignatureMethod(), domain.signatureMethod());
        assertEquals(entity.getStatus(), domain.status());
        assertEquals(entity.getDeclarationText(), domain.declarationText());
        assertEquals(entity.getIpAddress(), domain.ipAddress());
        assertEquals(entity.getCreatedAt(), domain.createdAt());
        assertEquals(entity.getPadesSignatureStatus(), domain.padesSignatureStatus());
    }

    @Test
    void shouldMapDomainToEntity() {
        var domain = domain();
        var entity = ServiceContractSignatureMapper.toEntity(domain);

        assertEquals(domain.signatureId(), entity.getSignatureId());
        assertEquals(domain.contractId(), entity.getContractId());
        assertEquals(domain.status(), entity.getStatus());
        assertEquals(domain.declarationText(), entity.getDeclarationText());
        assertEquals(domain.padesSignatureStatus(), entity.getPadesSignatureStatus());
    }

    @Test
    void shouldRoundTrip() {
        var original = domain();
        assertEquals(original, ServiceContractSignatureMapper.toDomain(ServiceContractSignatureMapper.toEntity(original)));
    }

    @Test
    void shouldReturnNullForNullEntity() {
        assertNull(ServiceContractSignatureMapper.toDomain(null));
    }

    @Test
    void shouldReturnNullForNullDomain() {
        assertNull(ServiceContractSignatureMapper.toEntity(null));
    }

    private ServiceContractSignature domain() {
        Instant now = Instant.parse("2026-05-10T14:00:00Z");
        return new ServiceContractSignature(
                UUID.fromString("a1b1c1d1-0000-0000-0000-000000000001"),
                UUID.fromString("a2b2c2d2-0000-0000-0000-000000000002"),
                UUID.fromString("a3b3c3d3-0000-0000-0000-000000000003"),
                UUID.fromString("a4b4c4d4-0000-0000-0000-000000000004"),
                UUID.fromString("a5b5c5d5-0000-0000-0000-000000000005"),
                UUID.fromString("a6b6c6d6-0000-0000-0000-000000000006"),
                now, "America/Sao_Paulo",
                ContractSignatureType.INTERNAL_ADVANCED,
                ContractSignatureMethod.PASSWORD_REAUTH,
                ContractSignatureStatus.ACTIVE,
                UUID.fromString("a7b7c7d7-0000-0000-0000-000000000007"),
                "hash-contract-sha256",
                "hash-pdf-sha256",
                "v1.0",
                "hash-decl-sha256",
                "Concordo com os termos do contrato",
                "10.0.0.1",
                "Chrome/120",
                "{\"ip\":\"10.0.0.1\"}",
                now, now.plusSeconds(30),
                null, null, null,
                "SERVICE_CONTRACT_TERMS", "2.0",
                "hash-canonical-sha256",
                UUID.fromString("a8b8c8d8-0000-0000-0000-000000000008"),
                "VALID"
        );
    }

    private ServiceContractSignatureEntity entity() {
        Instant now = Instant.parse("2026-05-10T14:00:00Z");
        return ServiceContractSignatureEntity.builder()
                .signatureId(UUID.fromString("a1b1c1d1-0000-0000-0000-000000000001"))
                .assignmentId(UUID.fromString("a2b2c2d2-0000-0000-0000-000000000002"))
                .contractId(UUID.fromString("a3b3c3d3-0000-0000-0000-000000000003"))
                .employeeId(UUID.fromString("a4b4c4d4-0000-0000-0000-000000000004"))
                .companyId(UUID.fromString("a5b5c5d5-0000-0000-0000-000000000005"))
                .signerUserId(UUID.fromString("a6b6c6d6-0000-0000-0000-000000000006"))
                .signedAt(now).signedAtZone("America/Sao_Paulo")
                .signatureType(ContractSignatureType.INTERNAL_ADVANCED)
                .signatureMethod(ContractSignatureMethod.PASSWORD_REAUTH)
                .status(ContractSignatureStatus.ACTIVE)
                .signedDocumentId(UUID.fromString("a7b7c7d7-0000-0000-0000-000000000007"))
                .contractDocumentHashSha256("hash-contract-sha256")
                .signedPdfHashSha256("hash-pdf-sha256")
                .declarationVersion("v1.0")
                .declarationHashSha256("hash-decl-sha256")
                .declarationText("Concordo com os termos do contrato")
                .ipAddress("10.0.0.1")
                .userAgent("Chrome/120")
                .evidenceJson("{\"ip\":\"10.0.0.1\"}")
                .createdAt(now).updatedAt(now.plusSeconds(30))
                .documentType("SERVICE_CONTRACT_TERMS").documentVersion("2.0")
                .canonicalEvidenceHashSha256("hash-canonical-sha256")
                .auditLogId(UUID.fromString("a8b8c8d8-0000-0000-0000-000000000008"))
                .padesSignatureStatus("VALID")
                .build();
    }
}
