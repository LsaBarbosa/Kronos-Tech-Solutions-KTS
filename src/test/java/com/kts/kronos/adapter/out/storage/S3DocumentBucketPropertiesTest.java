package com.kts.kronos.adapter.out.storage;

import com.kts.kronos.domain.model.enuns.DocumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class S3DocumentBucketPropertiesTest {

    private S3DocumentBucketProperties properties;

    @BeforeEach
    void setUp() {
        properties = new S3DocumentBucketProperties();
        properties.setBucketPayslip("kronos-docs-payslip-prod");
        properties.setBucketTimeOff("kronos-docs-time-off-prod");
        properties.setBucketDocuments("kronos-docs-general-prod");
        properties.setBucketEmployeeDocuments("kronos-docs-employee-prod");
        properties.setBucketPointRecordReceipt("kronos-docs-point-record-receipt-prod");
        properties.setBucketBiometricConsentTerm("kronos-docs-biometric-consent-prod");
        properties.setBucketServiceContractTerms("kronos-docs-service-contract-prod");
    }

    @Test
    void testPayslipBucket() {
        assertEquals("kronos-docs-payslip-prod", properties.bucketFor(DocumentType.PAYSLIP));
    }

    @Test
    void testTimeOffBucket() {
        assertEquals("kronos-docs-time-off-prod", properties.bucketFor(DocumentType.TIME_OFF));
    }

    @Test
    void testDocumentsBucket() {
        assertEquals("kronos-docs-general-prod", properties.bucketFor(DocumentType.DOCUMENTS));
    }

    @Test
    void testEmployeeDocumentsBucket() {
        assertEquals("kronos-docs-employee-prod", properties.bucketFor(DocumentType.EMPLOYEE_DOCUMENTS));
    }

    @Test
    void testPointRecordReceiptBucket() {
        assertEquals("kronos-docs-point-record-receipt-prod", properties.bucketFor(DocumentType.POINT_RECORD_RECEIPT));
    }

    @Test
    void testBiometricConsentTermBucket() {
        assertEquals("kronos-docs-biometric-consent-prod", properties.bucketFor(DocumentType.BIOMETRIC_CONSENT_TERM));
    }

    @Test
    void testServiceContractTermsBucket() {
        assertEquals("kronos-docs-service-contract-prod", properties.bucketFor(DocumentType.SERVICE_CONTRACT_TERMS));
    }

    @Test
    void testNullDocumentTypeThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> properties.bucketFor(null));
    }
}
