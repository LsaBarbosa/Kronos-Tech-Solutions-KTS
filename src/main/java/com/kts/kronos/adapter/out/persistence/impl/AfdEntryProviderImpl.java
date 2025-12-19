package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.AfdEntryRepository;
import com.kts.kronos.adapter.out.persistence.entity.AfdEntryEntity;
import com.kts.kronos.application.port.out.provider.AfdEntryProvider;
import com.kts.kronos.domain.model.AfdEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class AfdEntryProviderImpl implements AfdEntryProvider {

    private final AfdEntryRepository repository;

    @Override
    public AfdEntry save(AfdEntry domain) {
        AfdEntryEntity entity = toEntity(domain);
        AfdEntryEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<String> findLastHashByCompanyId(UUID companyId) {
        return repository.findLastHashByCompanyId(companyId);
    }

    @Override
    public Stream<AfdEntry> streamByCompanyIdOrderByNsr(UUID companyId) {
        // Mapeia o Stream de Entidade para Domínio sem carregar tudo na lista
        return repository.streamAllByCompanyIdOrderByNsrAsc(companyId)
                .map(this::toDomain);
    }

    private AfdEntryEntity toEntity(AfdEntry d) {
        return AfdEntryEntity.builder()
                .id(d.id())
                .nsr(d.nsr())
                .recordType(d.recordType())
                .recordDate(d.recordDate())
                .employeeCpf(d.employeeCpf())
                .employeePis(d.employeePis())
                .companyId(d.companyId())
                .employeeId(d.employeeId())
                .previousHash(d.previousHash())
                .currentHash(d.currentHash())
                .build();
    }

    private AfdEntry toDomain(AfdEntryEntity e) {
        return new AfdEntry(
                e.getId(),
                e.getNsr(),
                e.getRecordType(),
                e.getRecordDate(),
                e.getEmployeeCpf(),
                e.getEmployeePis(),
                e.getCompanyId(),
                e.getEmployeeId(),
                e.getPreviousHash(),
                e.getCurrentHash()
        );
    }
}