package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.domain.model.ServiceContractSignature;
import com.kts.kronos.domain.model.enuns.ContractSignatureStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ServiceContractSignatureProvider {

    ServiceContractSignature save(ServiceContractSignature signature);

    Optional<ServiceContractSignature> findById(UUID signatureId);

    Optional<ServiceContractSignature> findActiveByAssignment(UUID assignmentId);

    Page<ServiceContractSignature> findAdminFiltered(
            Pageable pageable,
            UUID companyId,
            UUID contractId,
            ContractSignatureStatus status
    );
}
