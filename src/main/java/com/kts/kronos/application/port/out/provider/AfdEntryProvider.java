package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.domain.model.AfdEntry;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public interface AfdEntryProvider {
    AfdEntry save(AfdEntry afdEntry);

    // Retorna apenas o hash para otimizar, sem trazer a entidade toda
    Optional<String> findLastHashByCompanyId(UUID companyId);

    // Stream é crucial para performance em produção
    Stream<AfdEntry> streamByCompanyIdOrderByNsr(UUID companyId);
}