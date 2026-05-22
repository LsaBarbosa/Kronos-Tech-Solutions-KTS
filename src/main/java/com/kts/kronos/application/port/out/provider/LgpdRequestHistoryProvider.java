package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.domain.model.LgpdRequestHistory;

import java.util.List;
import java.util.UUID;

public interface LgpdRequestHistoryProvider {
    LgpdRequestHistory save(LgpdRequestHistory history);

    List<LgpdRequestHistory> findByRequestId(UUID requestId);
}
