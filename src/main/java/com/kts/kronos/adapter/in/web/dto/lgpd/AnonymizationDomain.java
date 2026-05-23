package com.kts.kronos.adapter.in.web.dto.lgpd;

public record AnonymizationDomain(
        String resourceType,
        long scanned,
        long affected,
        long skipped,
        String action,
        String warning
) {
    public static AnonymizationDomain timeRecord(long scanned, long affected, long skipped, boolean preserveLaborData) {
        String action = "REMOVE_PRECISE_GEOLOCATION";
        String warning = preserveLaborData
                ? "Registros trabalhistas serão preservados. Apenas geolocalização será removida."
                : "Registros de ponto serão completamente anonimizados.";

        return new AnonymizationDomain(
                "TIME_RECORD",
                scanned,
                affected,
                skipped,
                action,
                warning
        );
    }
}
