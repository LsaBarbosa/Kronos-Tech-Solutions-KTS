package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.SecurityIncidentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SecurityIncidentRepository extends JpaRepository<SecurityIncidentEntity, UUID> {
    Page<SecurityIncidentEntity> findAllByOrderByDetectedAtDesc(Pageable pageable);
    Optional<SecurityIncidentEntity> findByIncidentId(UUID incidentId);
}
