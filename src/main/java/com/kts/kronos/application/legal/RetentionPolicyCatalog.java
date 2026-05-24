package com.kts.kronos.application.legal;

import com.kts.kronos.domain.model.RetentionPolicyCatalogEntry;
import com.kts.kronos.domain.model.enuns.RetentionPolicyCode;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RetentionPolicyCatalog {

    public List<RetentionPolicyCatalogEntry> getActivePolicies() {
        return List.of(
            new RetentionPolicyCatalogEntry(
                RetentionPolicyCode.RETENTION_BIOMETRIC_ACTIVE_CONSENT,
                "Retenção de dados biométricos enquanto consentimento está ativo",
                -1,
                "PRESERVE_LEGAL_EVIDENCE",
                true,
                true
            ),
            new RetentionPolicyCatalogEntry(
                RetentionPolicyCode.RETENTION_BIOMETRIC_EVIDENCE,
                "Retenção de evidência de consentimento biométrico",
                2555,
                "PRESERVE_LEGAL_EVIDENCE",
                true,
                true
            ),
            new RetentionPolicyCatalogEntry(
                RetentionPolicyCode.RETENTION_TIME_RECORD,
                "Retenção de registros de ponto conforme lei trabalhista",
                1095,
                "PRESERVE_LEGAL_EVIDENCE",
                false,
                true
            ),
            new RetentionPolicyCatalogEntry(
                RetentionPolicyCode.RETENTION_EMPLOYEE_CONTRACT,
                "Retenção de dados de contrato de trabalho",
                2555,
                "PRESERVE_LEGAL_EVIDENCE",
                false,
                true
            ),
            new RetentionPolicyCatalogEntry(
                RetentionPolicyCode.RETENTION_DOCUMENT_GENERAL,
                "Retenção geral de documentos corporativos",
                1825,
                "PRESERVE_LEGAL_EVIDENCE",
                false,
                true
            ),
            new RetentionPolicyCatalogEntry(
                RetentionPolicyCode.RETENTION_DOCUMENT_LABOR,
                "Retenção de documentos trabalhistas",
                2555,
                "PRESERVE_LEGAL_EVIDENCE",
                false,
                true
            ),
            new RetentionPolicyCatalogEntry(
                RetentionPolicyCode.RETENTION_SECURITY_LOG,
                "Retenção de logs de segurança e auditoria",
                365,
                "MINIMIZE",
                false,
                true
            ),
            new RetentionPolicyCatalogEntry(
                RetentionPolicyCode.RETENTION_LGPD_REQUEST,
                "Retenção de solicitações LGPD para conformidade",
                2555,
                "PRESERVE_LEGAL_EVIDENCE",
                true,
                true
            ),
            new RetentionPolicyCatalogEntry(
                RetentionPolicyCode.RETENTION_INTERNAL_MESSAGE,
                "Retenção de mensagens internas",
                730,
                "DELETE",
                false,
                true
            ),
            new RetentionPolicyCatalogEntry(
                RetentionPolicyCode.RETENTION_PASSWORD_RESET_TOKEN,
                "Retenção de tokens de reset de senha",
                1,
                "DELETE",
                false,
                true
            )
        );
    }

    public RetentionPolicyCatalogEntry getPolicyByCode(RetentionPolicyCode code) {
        return getActivePolicies().stream()
            .filter(p -> p.code().equals(code))
            .findFirst()
            .orElse(null);
    }
}
