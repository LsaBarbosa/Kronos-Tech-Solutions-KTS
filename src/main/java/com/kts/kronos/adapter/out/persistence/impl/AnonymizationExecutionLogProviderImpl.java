package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.AnonymizationExecutionLogRepository;
import com.kts.kronos.adapter.out.persistence.mapper.AnonymizationExecutionLogMapper;
import com.kts.kronos.application.port.out.provider.AnonymizationExecutionLogProvider;
import com.kts.kronos.domain.model.AnonymizationExecutionLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnonymizationExecutionLogProviderImpl implements AnonymizationExecutionLogProvider {
    private final AnonymizationExecutionLogRepository repository;
    private final AnonymizationExecutionLogMapper mapper;

    @Override
    public void save(AnonymizationExecutionLog log) {
        var entity = mapper.toPersistence(log);
        repository.save(entity);
    }
}
