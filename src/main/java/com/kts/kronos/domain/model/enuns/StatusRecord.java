package com.kts.kronos.domain.model.enuns;

import static com.kts.kronos.constants.Messages.STATUS_CHECKOUT;
import static com.kts.kronos.constants.Messages.STATUS_UPDATE;

public enum StatusRecord {
    CREATED,
    PENDING,
    UPDATED,
    UPDATE_REJECTED,
    DAY_OFF,
    ABSENCE,
    PENDING_APPROVAL,
    DOCTOR_APPOINTMENT,
    BREAK_IN_PROGRESS,
    BREAK;

    public StatusRecord onCheckout() {
        if (this == PENDING) {
            return CREATED;
        }
        throw new IllegalStateException(
                STATUS_CHECKOUT + this + ")"
        );
    }

    public StatusRecord onBreakEnd() {
        if (this == BREAK_IN_PROGRESS) {
            return BREAK;
        }
        throw new IllegalStateException(
                "Só é possível encerrar uma pausa com status BREAK_IN_PROGRESS (atual=" + this + ")"
        );
    }

    public StatusRecord onUpdate() {
        if (this == CREATED || this == UPDATED || this == PENDING || this == PENDING_APPROVAL) {
            return UPDATED;
        }
        throw new IllegalStateException(
                STATUS_UPDATE + this + ")"
        );
    }

}
