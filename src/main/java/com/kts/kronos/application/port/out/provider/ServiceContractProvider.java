package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.domain.model.ServiceContract;
import com.kts.kronos.domain.model.enuns.ServiceContractStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ServiceContractProvider {

    ServiceContract save(ServiceContract contract);

    Optional<ServiceContract> findById(UUID contractId);

    Page<ServiceContract> findByCompanyFiltered(Pageable pageable, UUID companyId, ServiceContractStatus status);
}
