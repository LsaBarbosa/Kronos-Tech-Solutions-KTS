package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.domain.model.TimesheetSignature;
import com.kts.kronos.domain.model.enuns.TimesheetSignatureStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface TimesheetSignatureProvider {

    TimesheetSignature save(TimesheetSignature signature);

    Optional<TimesheetSignature> findById(UUID signatureId);

    Optional<TimesheetSignature> findActiveByEmployeeAndPeriod(
            UUID employeeId,
            int referenceYear,
            int referenceMonth
    );

    Page<TimesheetSignature> findAdminFiltered(
            Pageable pageable,
            UUID companyId,
            Integer year,
            Integer month,
            TimesheetSignatureStatus status,
            Collection<UUID> employeeIds
    );
}
