package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.RetentionExecutionLogRepository;
import com.kts.kronos.adapter.out.persistence.mapper.RetentionExecutionLogMapper;
import com.kts.kronos.application.port.out.provider.RetentionExecutionLogProvider;
import com.kts.kronos.domain.model.RetentionExecutionLog;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

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

    @Override
    public List<RetentionExecutionLog> findRecent(int limit) {
        return repository.findAll(PageRequest.of(0, limit)).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Page<RetentionExecutionLog> findAll(Pageable pageable) {
        return repository.findAll(pageable)
                .map(mapper::toDomain);
    }
}
