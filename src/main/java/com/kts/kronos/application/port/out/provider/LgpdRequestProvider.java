package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.domain.model.LgpdRequest;
import com.kts.kronos.domain.model.enuns.LgpdRequestStatus;
import com.kts.kronos.domain.model.enuns.LgpdRequestType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LgpdRequestProvider {
    LgpdRequest save(LgpdRequest request);

    Optional<LgpdRequest> findById(UUID requestId);

    List<LgpdRequest> findByEmployeeId(UUID employeeId);

    List<LgpdRequest> findByCompanyId(UUID companyId);

    List<LgpdRequest> findAll();

    Page<LgpdRequest> findByCompanyId(UUID companyId, Pageable pageable);

    Page<LgpdRequest> findAll(Pageable pageable);

    Page<LgpdRequest> findAdminRequests(
            UUID companyId,
            LgpdRequestType type,
            LgpdRequestStatus status,
            Pageable pageable
    );
}
