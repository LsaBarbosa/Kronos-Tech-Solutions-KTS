package com.kts.kronos.application.port.out.provider;
import com.kts.kronos.domain.model.BreakRecord;

import java.util.List;
import java.util.Optional;
public interface BreakRecordProvider {
    BreakRecord save(BreakRecord breakRecord);
    Optional<BreakRecord> findOpenBreakByTimeRecordId(Long timeRecordId);
    List<BreakRecord> findByTimeRecordId(Long timeRecordId);
}
