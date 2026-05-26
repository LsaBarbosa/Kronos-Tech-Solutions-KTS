package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.AddressEmbeddable;
import com.kts.kronos.adapter.out.persistence.entity.EmployeeEntity;
import com.kts.kronos.adapter.out.persistence.entity.LegalConsentEntity;
import com.kts.kronos.application.port.out.projection.CompanyEmployeeCountsProjection;
import com.kts.kronos.domain.model.enuns.ConsentType;
import com.kts.kronos.domain.model.enuns.LegalBasis;
import com.kts.kronos.support.jpa.AbstractPostgresDataJpaTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;


class EmployeeRepositoryDataJpaTest extends AbstractPostgresDataJpaTest {

    @Autowired
    private EmployeeRepository repository;

    @Autowired
    private LegalConsentRepository legalConsentRepository;

    @Test
    @DisplayName("countByCompanyIds: deve retornar ativos e inativos por empresa")
    void shouldCountEmployeesByCompanyIds() {
        UUID companyA = UUID.randomUUID();
        UUID companyB = UUID.randomUUID();

        repository.save(employee(companyA, "12345678909", true));
        repository.save(employee(companyA, "98765432100", false));
        repository.save(employee(companyB, "11144477735", true));

        List<CompanyEmployeeCountsProjection> result = repository.countByCompanyIds(List.of(companyA, companyB));

        assertEquals(2, result.size());

        CompanyEmployeeCountsProjection projectionA = result.stream()
                .filter(p -> p.getCompanyId().equals(companyA))
                .findFirst()
                .orElseThrow();

        CompanyEmployeeCountsProjection projectionB = result.stream()
                .filter(p -> p.getCompanyId().equals(companyB))
                .findFirst()
                .orElseThrow();

        assertEquals(1L, projectionA.getActiveCount());
        assertEquals(1L, projectionA.getInactiveCount());

        assertEquals(1L, projectionB.getActiveCount());
        assertEquals(0L, projectionB.getInactiveCount());
    }

    @Test
    @DisplayName("findByCompanyIdAndActive: deve filtrar ativos por empresa")
    void shouldFindByCompanyIdAndActive() {
        UUID companyId = UUID.randomUUID();

        repository.save(employee(companyId, "12345678909", true));
        repository.save(employee(companyId, "98765432100", false));

        assertEquals(1, repository.findByCompanyIdAndActive(companyId, true).size());
        assertEquals(1, repository.findByCompanyIdAndActive(companyId, false).size());
    }

    @Test
    @DisplayName("existsByCpf/findByCpf: deve localizar colaborador por CPF")
    void shouldFindAndCheckExistenceByCpf() {
        UUID companyId = UUID.randomUUID();
        repository.save(employee(companyId, "12345678909", true));

        assertTrue(repository.existsByCpf("12345678909"));
        assertTrue(repository.findByCpf("12345678909").isPresent());
        assertTrue(repository.findByCpf("00000000000").isEmpty());
    }

    @Test
    @DisplayName("findByCompanyId/countByCompanyIdAndActive: deve filtrar e contar por empresa")
    void shouldFindAndCountByCompanyId() {
        UUID companyId = UUID.randomUUID();
        UUID otherCompanyId = UUID.randomUUID();

        repository.save(employee(companyId, "12345678909", true));
        repository.save(employee(companyId, "98765432100", false));
        repository.save(employee(otherCompanyId, "11144477735", true));

        assertEquals(2, repository.findByCompanyId(companyId).size());
        assertEquals(1, repository.countByCompanyIdAndActive(companyId, true));
        assertEquals(1, repository.countByCompanyIdAndActive(companyId, false));
    }

    @Test
    @DisplayName("deleteById: deve remover colaborador por id")
    void shouldDeleteById() {
        UUID companyId = UUID.randomUUID();
        EmployeeEntity saved = repository.save(employee(companyId, "12345678909", true));
        repository.flush();

        repository.deleteById(saved.getEmployeeId());
        repository.flush();

        assertTrue(repository.findById(saved.getEmployeeId()).isEmpty());
    }

    @Test
    @DisplayName("findEligibleBiometricArtifactsByMissingConsent: deve ignorar consentimento ativo e incluir ausencia de consentimento")
    void shouldFindEligibleBiometricArtifactsByMissingConsent() {
        UUID companyId = UUID.randomUUID();
        EmployeeEntity withoutConsent = repository.save(employee(companyId, "12345678909", true, "face-a"));
        EmployeeEntity withActiveConsent = repository.save(employee(companyId, "98765432100", true, "face-b"));
        legalConsentRepository.save(activeBiometricConsent(withActiveConsent.getEmployeeId()));

        List<EmployeeEntity> result = repository.findEligibleBiometricArtifactsByMissingConsent();

        assertTrue(result.stream().anyMatch(employee -> employee.getEmployeeId().equals(withoutConsent.getEmployeeId())));
        assertTrue(result.stream().noneMatch(employee -> employee.getEmployeeId().equals(withActiveConsent.getEmployeeId())));
    }

    @Test
    @DisplayName("findEligibleBiometricArtifactsByRevokedConsent: deve incluir apenas consentimento revogado antes do cutoff")
    void shouldFindEligibleBiometricArtifactsByRevokedConsent() {
        UUID companyId = UUID.randomUUID();
        Instant cutoff = Instant.parse("2026-05-20T00:00:00Z");
        EmployeeEntity revokedOld = repository.save(employee(companyId, "12345678909", true, "face-a"));
        EmployeeEntity revokedRecent = repository.save(employee(companyId, "98765432100", true, "face-b"));
        EmployeeEntity activeConsent = repository.save(employee(companyId, "11144477735", true, "face-c"));

        legalConsentRepository.save(revokedBiometricConsent(revokedOld.getEmployeeId(), cutoff.minusSeconds(3600)));
        legalConsentRepository.save(revokedBiometricConsent(revokedRecent.getEmployeeId(), cutoff.plusSeconds(3600)));
        legalConsentRepository.save(activeBiometricConsent(activeConsent.getEmployeeId()));

        List<EmployeeEntity> result = repository.findEligibleBiometricArtifactsByRevokedConsent(cutoff);

        assertEquals(1, result.size());
        assertEquals(revokedOld.getEmployeeId(), result.getFirst().getEmployeeId());
    }

    private EmployeeEntity employee(UUID companyId, String cpf, boolean active) {
        return employee(companyId, cpf, active, null);
    }

    private EmployeeEntity employee(UUID companyId, String cpf, boolean active, String faceS3ObjectKey) {
        return EmployeeEntity.builder()
                .employeeId(UUID.randomUUID())
                .fullName("Employee " + cpf)
                .cpf(cpf)
                .pis(null)
                .jobPosition("Developer")
                .email(cpf + "@kronos.com")
                .salary(3000.0)
                .phone("21999999999")
                .active(active)
                .faceS3ObjectKey(faceS3ObjectKey)
                .address(AddressEmbeddable.builder()
                        .street("Rua A")
                        .number("10")
                        .postalCode("12345678")
                        .city("Rio")
                        .state("RJ")
                        .build())
                .companyId(companyId)
                .homeOffice(false)
                .build();
    }

    private LegalConsentEntity activeBiometricConsent(UUID employeeId) {
        Instant now = Instant.parse("2026-05-26T12:00:00Z");
        return LegalConsentEntity.builder()
                .consentId(UUID.randomUUID())
                .employeeId(employeeId)
                .consentType(ConsentType.BIOMETRIC_AUTHENTICATION)
                .legalBasis(LegalBasis.CONSENT)
                .purpose("Biometric authentication")
                .version("v1")
                .grantedAt(now.minusSeconds(7200))
                .createdAt(now.minusSeconds(7200))
                .updatedAt(now.minusSeconds(7200))
                .build();
    }

    private LegalConsentEntity revokedBiometricConsent(UUID employeeId, Instant revokedAt) {
        return LegalConsentEntity.builder()
                .consentId(UUID.randomUUID())
                .employeeId(employeeId)
                .consentType(ConsentType.BIOMETRIC_AUTHENTICATION)
                .legalBasis(LegalBasis.CONSENT)
                .purpose("Biometric authentication")
                .version("v1")
                .grantedAt(revokedAt.minusSeconds(7200))
                .revokedAt(revokedAt)
                .createdAt(revokedAt.minusSeconds(7200))
                .updatedAt(revokedAt)
                .build();
    }
}
