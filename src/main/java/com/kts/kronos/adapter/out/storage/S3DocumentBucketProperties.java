package com.kts.kronos.adapter.out.storage;

import com.kts.kronos.domain.model.enuns.DocumentType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "aws.s3")
public class S3DocumentBucketProperties {

    private String bucketPayslip;
    private String bucketTimeOff;
    private String bucketDocuments;
    private String bucketEmployeeDocuments;
    private String bucketPointRecordReceipt;
    private String bucketBiometricConsentTerm;
    private String bucketServiceContractTerms;
    private String bucketPointMirrorSignature;
    private String bucketFaceImages;

    public String bucketFor(DocumentType type) {
        if (type == null) {
            throw new IllegalArgumentException("DocumentType não pode ser nulo.");
        }

        return switch (type) {
            case PAYSLIP -> bucketPayslip;
            case TIME_OFF -> bucketTimeOff;
            case DOCUMENTS -> bucketDocuments;
            case EMPLOYEE_DOCUMENTS -> bucketEmployeeDocuments;
            case POINT_RECORD_RECEIPT -> bucketPointRecordReceipt;
            case BIOMETRIC_CONSENT_TERM -> bucketBiometricConsentTerm;
            case SERVICE_CONTRACT_TERMS -> bucketServiceContractTerms;
            case POINT_MIRROR_SIGNATURE ->
                    // Bucket dedicado para espelhos de ponto assinados eletronicamente.
                    // Fallback para bucketPointRecordReceipt se a env var não estiver setada,
                    // evitando NPE em ambientes onde a config ainda não foi propagada.
                    bucketPointMirrorSignature != null && !bucketPointMirrorSignature.isBlank()
                            ? bucketPointMirrorSignature
                            : bucketPointRecordReceipt;
        };
    }
}
