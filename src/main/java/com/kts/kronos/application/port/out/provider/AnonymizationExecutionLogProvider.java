package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.domain.model.AnonymizationExecutionLog;

public interface AnonymizationExecutionLogProvider {
    void save(AnonymizationExecutionLog log);
}
