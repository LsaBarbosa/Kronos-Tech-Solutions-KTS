package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.domain.model.LgpdRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LgpdRequestProvider {
    LgpdRequest save(LgpdRequest request);

    Optional<LgpdRequest> findById(UUID requestId);

    List<LgpdRequest> findByEmployeeId(UUID employeeId);

    List<LgpdRequest> findByCompanyId(UUID companyId);

    List<LgpdRequest> findAll();
}
