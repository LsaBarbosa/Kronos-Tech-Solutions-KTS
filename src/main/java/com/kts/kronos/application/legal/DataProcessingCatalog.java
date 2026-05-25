package com.kts.kronos.application.legal;

import com.kts.kronos.adapter.in.web.dto.lgpd.PublicDataProcessingPurposeResponse;
import com.kts.kronos.domain.model.DataProcessingPurpose;
import com.kts.kronos.domain.model.enuns.DataCategory;
import com.kts.kronos.domain.model.enuns.LegalBasis;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataProcessingCatalog {

    public List<DataProcessingPurpose> getActiveTreatments() {
        return List.of(
            new DataProcessingPurpose(
                "EMPLOYEE_IDENTIFICATION",
                DataCategory.IDENTIFICATION,
                LegalBasis.CONTRACT_EXECUTION,
                "Identificação de colaboradores para cumprimento de contrato de trabalho",
                "RETENTION_EMPLOYEE_CONTRACT",
                false,
                true
            ),
            new DataProcessingPurpose(
                "EMPLOYEE_CONTACT",
                DataCategory.CONTACT,
                LegalBasis.CONTRACT_EXECUTION,
                "Dados de contato para comunicações trabalhistas e emergenciais",
                "RETENTION_EMPLOYEE_CONTRACT",
                false,
                true
            ),
            new DataProcessingPurpose(
                "EMPLOYEE_CONTRACT_DATA",
                DataCategory.EMPLOYMENT,
                LegalBasis.CONTRACT_EXECUTION,
                "Dados contratuais e de emprego para cumprimento de obrigações legais",
                "RETENTION_EMPLOYEE_CONTRACT",
                false,
                true
            ),
            new DataProcessingPurpose(
                "EMPLOYEE_PAYROLL_DATA",
                DataCategory.PAYROLL,
                LegalBasis.LEGAL_OBLIGATION,
                "Dados de folha de pagamento para cumprimento de obrigações fiscais e previdenciárias",
                "RETENTION_EMPLOYEE_CONTRACT",
                false,
                true
            ),
            new DataProcessingPurpose(
                "TIME_RECORD_CONTROL",
                DataCategory.TIME_RECORD,
                LegalBasis.LEGAL_OBLIGATION,
                "Registro de jornada de trabalho conforme lei trabalhista",
                "RETENTION_TIME_RECORD",
                false,
                true
            ),
            new DataProcessingPurpose(
                "TIME_RECORD_GEOLOCATION",
                DataCategory.GEOLOCATION,
                LegalBasis.LEGAL_OBLIGATION,
                "Geolocalização para controle de jornada e comprovação de local de trabalho",
                "RETENTION_TIME_RECORD",
                false,
                true
            ),
            new DataProcessingPurpose(
                "BIOMETRIC_AUTHENTICATION",
                DataCategory.BIOMETRIC,
                LegalBasis.CONSENT,
                "Dados biométricos para autenticação e segurança de acesso aos sistemas",
                "RETENTION_BIOMETRIC_ACTIVE_CONSENT",
                true,
                true
            ),
            new DataProcessingPurpose(
                "DOCUMENT_MANAGEMENT",
                DataCategory.DOCUMENT,
                LegalBasis.LEGAL_OBLIGATION,
                "Gerenciamento de documentos corporativos e pessoais para conformidade legal",
                "RETENTION_DOCUMENT_LABOR",
                false,
                true
            ),
            new DataProcessingPurpose(
                "INTERNAL_MESSAGES",
                DataCategory.MESSAGE,
                LegalBasis.LEGITIMATE_INTEREST,
                "Mensagens internas para comunicação corporativa e documentação",
                "RETENTION_INTERNAL_MESSAGE",
                false,
                true
            ),
            new DataProcessingPurpose(
                "SECURITY_AUDIT_LOGS",
                DataCategory.SECURITY_LOG,
                LegalBasis.LEGITIMATE_INTEREST,
                "Logs de segurança e auditoria para proteção de sistemas e detecção de fraudes",
                "RETENTION_SECURITY_LOG",
                false,
                true
            ),
            new DataProcessingPurpose(
                "LGPD_REQUEST_MANAGEMENT",
                DataCategory.LGPD_REQUEST,
                LegalBasis.LEGAL_OBLIGATION,
                "Gerenciamento de solicitações LGPD (acesso, retificação, exclusão)",
                "RETENTION_LGPD_REQUEST",
                false,
                true
            ),
            new DataProcessingPurpose(
                "LEGAL_CONSENT_EVIDENCE",
                DataCategory.LEGAL_CONSENT,
                LegalBasis.CONSENT,
                "Registro e evidência de consentimentos legais (biometria, processamento)",
                "RETENTION_BIOMETRIC_EVIDENCE",
                false,
                true
            )
        );
    }

    public DataProcessingPurpose getTreatmentByCode(String code) {
        return getActiveTreatments().stream()
            .filter(t -> t.code().equals(code))
            .findFirst()
            .orElse(null);
    }

    public List<PublicDataProcessingPurposeResponse> getPublicTreatments() {
        return getActiveTreatments().stream()
            .filter(DataProcessingPurpose::active)
            .map(this::toPublic)
            .toList();
    }

    private PublicDataProcessingPurposeResponse toPublic(DataProcessingPurpose treatment) {
        String publicPurpose = getPublicPurpose(treatment.code(), treatment.purpose());

        return new PublicDataProcessingPurposeResponse(
            treatment.code(),
            treatment.dataCategory(),
            treatment.legalBasis(),
            publicPurpose,
            treatment.retentionPolicyCode(),
            treatment.sensitive(),
            treatment.active()
        );
    }

    private String getPublicPurpose(String code, String technicalPurpose) {
        return switch (code) {
            case "EMPLOYEE_IDENTIFICATION" ->
                "Gerenciamento da sua identificação para cumprimento do contrato de trabalho.";
            case "EMPLOYEE_CONTACT" ->
                "Dados de contato para comunicações relacionadas ao trabalho.";
            case "EMPLOYEE_CONTRACT_DATA" ->
                "Informações sobre seu contrato e relação de emprego.";
            case "EMPLOYEE_PAYROLL_DATA" ->
                "Processamento do seu pagamento e obrigações fiscais.";
            case "TIME_RECORD_CONTROL" ->
                "Registro da sua jornada de trabalho.";
            case "TIME_RECORD_GEOLOCATION" ->
                "Localização para controle e registro de sua jornada.";
            case "BIOMETRIC_AUTHENTICATION" ->
                "Autenticação segura com dados biométricos (requer consentimento).";
            case "DOCUMENT_MANAGEMENT" ->
                "Gerenciamento de documentos corporativos e pessoais.";
            case "INTERNAL_MESSAGES" ->
                "Comunicação interna e troca de mensagens.";
            case "SECURITY_AUDIT_LOGS" ->
                "Proteção de sistemas e detecção de atividades fraudulentas.";
            case "LGPD_REQUEST_MANAGEMENT" ->
                "Processamento de seus direitos de acesso, correção e exclusão.";
            case "LEGAL_CONSENT_EVIDENCE" ->
                "Registro legal dos consentimentos que você forneceu.";
            default -> technicalPurpose;
        };
    }
}
