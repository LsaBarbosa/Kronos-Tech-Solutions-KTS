package com.kts.kronos.adapter.out.persistence.mapper;

import com.kts.kronos.adapter.out.persistence.entity.SecurityIncidentEntity;
import com.kts.kronos.domain.model.SecurityIncident;
import org.springframework.stereotype.Component;

@Component
public class SecurityIncidentMapper {

    public SecurityIncident toDomain(SecurityIncidentEntity entity) {
        if (entity == null) {
            return null;
        }
        return entity.toDomain();
    }

    public SecurityIncidentEntity toEntity(SecurityIncident domain) {
        if (domain == null) {
            return null;
        }
        return SecurityIncidentEntity.builder()
                .incidentId(domain.incidentId())
                .title(domain.title())
                .description(domain.description())
                .detectedAt(domain.detectedAt())
                .confirmedAt(domain.confirmedAt())
                .severity(domain.severity())
                .personalDataInvolved(domain.personalDataInvolved())
                .sensitiveDataInvolved(domain.sensitiveDataInvolved())
                .affectedSubjectsEstimate(domain.affectedSubjectsEstimate())
                .status(domain.status())
                .notifiedAnpdAt(domain.notifiedAnpdAt())
                .notifiedSubjectsAt(domain.notifiedSubjectsAt())
                .createdByUserId(domain.createdByUserId())
                .createdAt(domain.createdAt())
                .updatedAt(domain.updatedAt())
                .build();
    }
}
