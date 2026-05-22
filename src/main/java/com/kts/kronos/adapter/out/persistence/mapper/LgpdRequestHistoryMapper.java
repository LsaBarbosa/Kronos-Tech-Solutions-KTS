package com.kts.kronos.adapter.out.persistence.mapper;

import com.kts.kronos.adapter.out.persistence.entity.LgpdRequestHistoryEntity;
import com.kts.kronos.domain.model.LgpdRequestHistory;
import org.springframework.stereotype.Component;

@Component
public class LgpdRequestHistoryMapper {
    public LgpdRequestHistory toDomain(LgpdRequestHistoryEntity entity) {
        return new LgpdRequestHistory(
                entity.getHistoryId(),
                entity.getRequestId(),
                entity.getStatus(),
                entity.getNotes(),
                entity.getChangedByUserId(),
                entity.getCreatedAt(),
                entity.getEventType(),
                entity.getPreviousStatus(),
                entity.getNewStatus(),
                entity.getPublicNote(),
                entity.getInternalNote(),
                entity.getActorUserId(),
                entity.getVisibleToDataSubject()
        );
    }

    public LgpdRequestHistoryEntity toEntity(LgpdRequestHistory domain) {
        return LgpdRequestHistoryEntity.builder()
                .historyId(domain.historyId())
                .requestId(domain.requestId())
                .status(domain.status())
                .notes(domain.notes())
                .changedByUserId(domain.changedByUserId())
                .createdAt(domain.createdAt())
                .eventType(domain.eventType())
                .previousStatus(domain.previousStatus())
                .newStatus(domain.newStatus())
                .publicNote(domain.publicNote())
                .internalNote(domain.internalNote())
                .actorUserId(domain.actorUserId())
                .visibleToDataSubject(domain.visibleToDataSubject())
                .build();
    }
}
