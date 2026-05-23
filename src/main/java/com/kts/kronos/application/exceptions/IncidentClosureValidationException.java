package com.kts.kronos.application.exceptions;

import java.util.List;
import java.util.UUID;

public class IncidentClosureValidationException extends RuntimeException {
    private static final String CODE = "INCIDENT_CLOSURE_MISSING_EVIDENCE";
    private final UUID incidentId;
    private final List<String> missingFields;
    private final String message;

    public IncidentClosureValidationException(UUID incidentId, List<String> missingFields) {
        super(String.format(
                "Incidente com comunicação obrigatória não pode ser encerrado sem evidência. Campos faltantes: %s",
                String.join(", ", missingFields)
        ));
        this.incidentId = incidentId;
        this.missingFields = missingFields;
        this.message = super.getMessage();
    }

    public String getCode() {
        return CODE;
    }

    public UUID getIncidentId() {
        return incidentId;
    }

    public List<String> getMissingFields() {
        return missingFields;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
