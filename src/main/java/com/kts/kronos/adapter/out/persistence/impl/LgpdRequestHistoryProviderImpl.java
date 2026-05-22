package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.LgpdRequestHistoryRepository;
import com.kts.kronos.adapter.out.persistence.mapper.LgpdRequestHistoryMapper;
import com.kts.kronos.application.port.out.provider.LgpdRequestHistoryProvider;
import com.kts.kronos.domain.model.LgpdRequestHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LgpdRequestHistoryProviderImpl implements LgpdRequestHistoryProvider {
    private final LgpdRequestHistoryRepository repository;
    private final LgpdRequestHistoryMapper mapper;

    @Override
    public LgpdRequestHistory save(LgpdRequestHistory history) {
        return mapper.toDomain(repository.save(mapper.toEntity(history)));
    }

    @Override
    public List<LgpdRequestHistory> findByRequestId(UUID requestId) {
        return repository.findByRequestIdOrderByCreatedAtAsc(requestId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
