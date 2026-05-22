package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.RetentionExecutionLogRepository;
import com.kts.kronos.adapter.out.persistence.mapper.RetentionExecutionLogMapper;
import com.kts.kronos.application.port.out.provider.RetentionExecutionLogProvider;
import com.kts.kronos.domain.model.RetentionExecutionLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RetentionExecutionLogProviderImpl implements RetentionExecutionLogProvider {
    private final RetentionExecutionLogRepository repository;
    private final RetentionExecutionLogMapper mapper;

    @Override
    public void save(RetentionExecutionLog log) {
        var entity = mapper.toPersistence(log);
        repository.save(entity);
    }
}
