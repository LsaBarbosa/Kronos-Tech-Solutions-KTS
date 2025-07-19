package com.kts.kronos.domain.model;

import static com.kts.kronos.constants.Messages.STATUS_CHECKOUT;
import static com.kts.kronos.constants.Messages.STATUS_UPDATE;

public enum StatusRecord {
    CREATED,
    PENDING,
    UPDATED,
    DAY_OFF,
    ABSENCE,
    DOCTOR_APPOINTMENT;


    public StatusRecord onCheckout() {
        if (this == PENDING) {
            return CREATED;
        }
        throw new IllegalStateException(
                STATUS_CHECKOUT + this + ")"
        );
    }

    public StatusRecord onUpdate() {
        if (this == CREATED || this == UPDATED || this == PENDING) {
            return UPDATED;
        }
        throw new IllegalStateException(
                STATUS_UPDATE + this + ")"
        );
    }

}
