package com.kts.kronos.application.legal;

import com.kts.kronos.domain.model.RetentionPolicyCatalogEntry;
import com.kts.kronos.domain.model.enuns.RetentionAction;
import com.kts.kronos.domain.model.enuns.RetentionPolicyCode;
import com.kts.kronos.domain.model.enuns.RetentionPolicyType;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;

@Component
public class RetentionPolicyCatalog {
    private static final EnumSet<RetentionResourceType> EXECUTABLE_RESOURCE_TYPES = EnumSet.of(
            RetentionResourceType.BIOMETRIC_ARTIFACT,
            RetentionResourceType.LEGAL_CONSENT,
            RetentionResourceType.DOCUMENT,
            RetentionResourceType.AUDIT_LOG,
            RetentionResourceType.LGPD_REQUEST,
            RetentionResourceType.MESSAGE,
            RetentionResourceType.PASSWORD_RESET_TOKEN,
            RetentionResourceType.TIME_RECORD,
            RetentionResourceType.EMPLOYEE_CONTRACT
    );

    public List<RetentionPolicyCatalogEntry> getActivePolicies() {
        return getAllPolicies().stream()
                .filter(RetentionPolicyCatalogEntry::active)
                .peek(this::validateActivePolicy)
                .toList();
    }

    public List<RetentionPolicyCatalogEntry> getAllPolicies() {
        return List.of(
                new RetentionPolicyCatalogEntry(
                        RetentionPolicyCode.RETENTION_BIOMETRIC_ACTIVE_CONSENT,
                        "Retenção de artefatos biométricos enquanto houver consentimento ativo",
                        RetentionPolicyType.CONSENT_BASED,
                        RetentionResourceType.BIOMETRIC_ARTIFACT,
                        null,
                        RetentionAction.PRESERVE_WHILE_CONSENT_ACTIVE,
                        true,
                        true,
                        false,
                        false
                ),
                new RetentionPolicyCatalogEntry(
                        RetentionPolicyCode.RETENTION_BIOMETRIC_EVIDENCE,
                        "Retenção de evidência de consentimento biométrico por prazo jurídico definido",
                        RetentionPolicyType.TIME_BASED,
                        RetentionResourceType.LEGAL_CONSENT,
                        2555,
                        RetentionAction.PRESERVE_LEGAL_EVIDENCE,
                        true,
                        true,
                        false,
                        false
                ),
                new RetentionPolicyCatalogEntry(
                        RetentionPolicyCode.RETENTION_TIME_RECORD,
                        "Retenção de registros de ponto conforme obrigação trabalhista",
                        RetentionPolicyType.TIME_BASED,
                        RetentionResourceType.TIME_RECORD,
                        1095,
                        RetentionAction.PRESERVE_LEGAL_EVIDENCE,
                        true,
                        true,
                        true,
                        false
                ),
                new RetentionPolicyCatalogEntry(
                        RetentionPolicyCode.RETENTION_EMPLOYEE_CONTRACT,
                        "Retenção de dados de contrato de trabalho",
                        RetentionPolicyType.TIME_BASED,
                        RetentionResourceType.EMPLOYEE_CONTRACT,
                        2555,
                        RetentionAction.PRESERVE_LEGAL_EVIDENCE,
                        true,
                        true,
                        true,
                        true
                ),
                new RetentionPolicyCatalogEntry(
                        RetentionPolicyCode.RETENTION_DOCUMENT_GENERAL,
                        "Retenção geral de documentos corporativos",
                        RetentionPolicyType.TIME_BASED,
                        RetentionResourceType.DOCUMENT,
                        1825,
                        RetentionAction.PRESERVE_LEGAL_EVIDENCE,
                        false,
                        true,
                        false,
                        false
                ),
                new RetentionPolicyCatalogEntry(
                        RetentionPolicyCode.RETENTION_DOCUMENT_LABOR,
                        "Retenção de documentos trabalhistas com preservação de dados obrigatórios",
                        RetentionPolicyType.TIME_BASED,
                        RetentionResourceType.DOCUMENT,
                        2555,
                        RetentionAction.PRESERVE_LEGAL_EVIDENCE,
                        false,
                        true,
                        true,
                        true
                ),
                new RetentionPolicyCatalogEntry(
                        RetentionPolicyCode.RETENTION_SECURITY_LOG,
                        "Retenção de logs de segurança e auditoria",
                        RetentionPolicyType.TIME_BASED,
                        RetentionResourceType.AUDIT_LOG,
                        365,
                        RetentionAction.MINIMIZE,
                        false,
                        true,
                        false,
                        false
                ),
                new RetentionPolicyCatalogEntry(
                        RetentionPolicyCode.RETENTION_LGPD_REQUEST,
                        "Retenção de solicitações LGPD para conformidade e defesa administrativa",
                        RetentionPolicyType.TIME_BASED,
                        RetentionResourceType.LGPD_REQUEST,
                        2555,
                        RetentionAction.PRESERVE_LEGAL_EVIDENCE,
                        true,
                        true,
                        false,
                        false
                ),
                new RetentionPolicyCatalogEntry(
                        RetentionPolicyCode.RETENTION_INTERNAL_MESSAGE,
                        "Retenção de mensagens internas",
                        RetentionPolicyType.TIME_BASED,
                        RetentionResourceType.MESSAGE,
                        730,
                        RetentionAction.DELETE,
                        false,
                        true,
                        false,
                        false
                ),
                new RetentionPolicyCatalogEntry(
                        RetentionPolicyCode.RETENTION_PASSWORD_RESET_TOKEN,
                        "Retenção de tokens de reset de senha",
                        RetentionPolicyType.TIME_BASED,
                        RetentionResourceType.PASSWORD_RESET_TOKEN,
                        1,
                        RetentionAction.DELETE,
                        true,
                        true,
                        false,
                        false
                )
        );
    }

    public RetentionPolicyCatalogEntry getPolicyByCode(RetentionPolicyCode code) {
        return getAllPolicies().stream()
                .filter(policy -> policy.code().equals(code))
                .findFirst()
                .orElse(null);
    }

    private void validateActivePolicy(RetentionPolicyCatalogEntry policy) {
        if (policy.code() == null) {
            throw new IllegalStateException("Active retention policy is missing policyCode");
        }
        if (policy.policyType() == null) {
            throw new IllegalStateException("Active retention policy is missing policyType: " + policy.code());
        }
        if (policy.resourceType() == null) {
            throw new IllegalStateException("Active retention policy is missing resourceType: " + policy.code());
        }
        if (policy.action() == null) {
            throw new IllegalStateException("Active retention policy is missing action: " + policy.code());
        }
        if (policy.policyType() == RetentionPolicyType.TIME_BASED) {
            if (policy.retentionDays() == null || policy.retentionDays() <= 0) {
                throw new IllegalStateException(
                        "TIME_BASED active retention policy must define positive retentionDays: " + policy.code()
                );
            }
        } else if (policy.retentionDays() != null && policy.retentionDays() <= 0) {
            throw new IllegalStateException(
                    "Non-time-based active retention policy cannot use non-positive retentionDays: " + policy.code()
            );
        }
        if (!EXECUTABLE_RESOURCE_TYPES.contains(policy.resourceType())) {
            throw new IllegalStateException("Active retention policy has no processor available: " + policy.code());
        }
    }
}
