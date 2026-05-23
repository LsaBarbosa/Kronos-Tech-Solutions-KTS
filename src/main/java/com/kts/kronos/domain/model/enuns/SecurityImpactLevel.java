package com.kts.kronos.domain.model.enuns;

public enum SecurityImpactLevel {
    NONE("Sem impacto"),
    LOW("Baixo"),
    MEDIUM("Médio"),
    HIGH("Alto"),
    CRITICAL("Crítico");

    private final String label;

    SecurityImpactLevel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
