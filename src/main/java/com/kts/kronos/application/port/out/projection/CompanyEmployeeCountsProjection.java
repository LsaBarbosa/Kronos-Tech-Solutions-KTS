package com.kts.kronos.application.port.out.projection;

import java.util.UUID;

public interface CompanyEmployeeCountsProjection {
    UUID getCompanyId();
    Long getActiveCount();
    Long getInactiveCount();
}