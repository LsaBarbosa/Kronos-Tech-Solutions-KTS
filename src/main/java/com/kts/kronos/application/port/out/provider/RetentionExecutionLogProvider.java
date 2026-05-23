package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.domain.model.RetentionExecutionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RetentionExecutionLogProvider {
    void save(RetentionExecutionLog log);

    List<RetentionExecutionLog> findRecent(int limit);

    Page<RetentionExecutionLog> findAll(Pageable pageable);
}
