package com.kts.kronos.domain.model;

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
                "Só é possível fazer checkout de um registro PENDING (atual=" + this + ")"
        );
    }

    public StatusRecord onUpdate() {
        if (this == CREATED || this == UPDATED || this == PENDING) {
            return UPDATED;
        }
        throw new IllegalStateException(
                "Só é possível editar um registro CREATED/UPDATED (atual=" + this + ")"
        );
    }

}
