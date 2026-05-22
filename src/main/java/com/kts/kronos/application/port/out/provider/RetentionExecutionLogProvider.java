package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.domain.model.RetentionExecutionLog;

public interface RetentionExecutionLogProvider {
    void save(RetentionExecutionLog log);
}
