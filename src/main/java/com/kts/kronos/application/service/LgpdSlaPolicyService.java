package com.kts.kronos.application.service;

import com.kts.kronos.domain.model.enuns.LgpdRequestType;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class LgpdSlaPolicyService {

    public Instant calculateDueAt(LgpdRequestType type, Instant createdAt) {
        int days = switch (type) {
            case CONFIRM_PROCESSING -> 15;
            case ACCESS -> 15;
            case CORRECTION -> 15;
            case ANONYMIZATION -> 15;
            case BLOCKING -> 15;
            case DELETION -> 15;
            case PORTABILITY -> 15;
            case CONSENT_REVOCATION -> 2;
            case SHARING_INFORMATION -> 15;
        };

        return createdAt.plus(days, ChronoUnit.DAYS);
    }

    public boolean isOverdue(Instant dueAt) {
        return dueAt != null && Instant.now().isAfter(dueAt);
    }

    public long daysRemaining(Instant dueAt) {
        if (dueAt == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(Instant.now(), dueAt);
    }

    public String priorityBySla(Instant dueAt) {
        if (dueAt == null) {
            return "NORMAL";
        }

        long daysRemaining = daysRemaining(dueAt);
        if (daysRemaining < 0) {
            return "OVERDUE";
        } else if (daysRemaining <= 3) {
            return "URGENT";
        } else if (daysRemaining <= 7) {
            return "HIGH";
        } else {
            return "NORMAL";
        }
    }
}
