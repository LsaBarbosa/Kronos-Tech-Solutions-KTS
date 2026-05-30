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

    public static AnonymizationDomain user(long scanned, long affected, long skipped) {
        String action = "ANONYMIZE_AND_DEACTIVATE";
        String warning = "Conta de usuário será anonimizada, senha invalidada e sessões encerradas.";

        return new AnonymizationDomain(
                "USER",
                scanned,
                affected,
                skipped,
                action,
                warning
        );
    }

    public static AnonymizationDomain document(long scanned, long affected, long skipped) {
        String action = "DELETE_AND_ANONYMIZE";
        String warning = "Documentos serão removidos do armazenamento e referências anonimizadas.";

        return new AnonymizationDomain(
                "DOCUMENT",
                scanned,
                affected,
                skipped,
                action,
                warning
        );
    }

    public static AnonymizationDomain biometricArtifact(long scanned, long affected, long skipped) {
        String action = "DELETE_FACE_IMAGES";
        String warning = "Imagens faciais serão permanentemente removidas do sistema de reconhecimento.";

        return new AnonymizationDomain(
                "BIOMETRIC_ARTIFACT",
                scanned,
                affected,
                skipped,
                action,
                warning
        );
    }

    public static AnonymizationDomain employee(long scanned, long affected, long skipped) {
        String action = "ANONYMIZE_EMPLOYEE_RECORD";
        String warning = "Dados pessoais do colaborador serão anonimizados mantendo registros operacionais.";

        return new AnonymizationDomain(
                "EMPLOYEE",
                scanned,
                affected,
                skipped,
                action,
                warning
        );
    }

    public static AnonymizationDomain message(long scanned, long affected, long skipped) {
        String action = "ANONYMIZE_MESSAGE_CONTENT";
        String warning = "Mensagens será anonimizadas removendo conteúdo pessoal.";

        return new AnonymizationDomain(
                "MESSAGE",
                scanned,
                affected,
                skipped,
                action,
                warning
        );
    }

    public static AnonymizationDomain auditLog(long scanned, long affected, long skipped) {
        String action = "MASK_SENSITIVE_DATA";
        String warning = "Registros de auditoria serão mascarados removendo dados pessoais e tokens.";

        return new AnonymizationDomain(
                "AUDIT_LOG",
                scanned,
                affected,
                skipped,
                action,
                warning
        );
    }
}
