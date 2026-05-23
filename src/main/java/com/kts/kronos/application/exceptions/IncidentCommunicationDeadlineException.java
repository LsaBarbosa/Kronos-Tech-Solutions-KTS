package com.kts.kronos.application.exceptions;

import java.util.UUID;

public class IncidentCommunicationDeadlineException extends RuntimeException {
    private static final String CODE = "INCIDENT_COMMUNICATION_DEADLINE_REQUIRED";
    private final UUID incidentId;
    private final String message;

    public IncidentCommunicationDeadlineException(UUID incidentId, String details) {
        super(String.format(
                "Prazos de comunicação à ANPD e aos titulares são obrigatórios quando a comunicação é requerida. Detalhes: %s",
                details
        ));
        this.incidentId = incidentId;
        this.message = super.getMessage();
    }

    public IncidentCommunicationDeadlineException(UUID incidentId) {
        super("Prazos de comunicação à ANPD e aos titulares são obrigatórios quando a comunicação é requerida.");
        this.incidentId = incidentId;
        this.message = super.getMessage();
    }

    public String getCode() {
        return CODE;
    }

    public UUID getIncidentId() {
        return incidentId;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
