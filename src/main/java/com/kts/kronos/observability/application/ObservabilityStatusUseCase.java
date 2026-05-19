package com.kts.kronos.observability.application;

import com.kts.kronos.observability.domain.ObservabilityStatus;

public interface ObservabilityStatusUseCase {

    ObservabilityStatus getStatus();
}
