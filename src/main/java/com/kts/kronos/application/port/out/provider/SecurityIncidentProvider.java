package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.domain.model.SecurityIncident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface SecurityIncidentProvider {
    SecurityIncident save(SecurityIncident incident);
    Optional<SecurityIncident> findById(UUID incidentId);
    Page<SecurityIncident> findAll(Pageable pageable);
}
