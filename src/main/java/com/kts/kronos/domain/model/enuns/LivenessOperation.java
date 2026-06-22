package com.kts.kronos.domain.model.enuns;

public enum LivenessOperation {
    ENROLLMENT("Matrícula biométrica"),
    FACE_LOGIN("Login por rosto"),
    CHECKIN("Registro de ponto"),
    CONTRACT_SIGNING("Assinatura de contrato"),
    TIMESHEET_SIGNING("Assinatura de espelho de ponto");

    private final String description;

    LivenessOperation(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
