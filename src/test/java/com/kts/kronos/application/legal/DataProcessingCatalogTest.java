package com.kts.kronos.application.legal;

import com.kts.kronos.adapter.in.web.dto.lgpd.PublicDataProcessingPurposeResponse;
import com.kts.kronos.domain.model.DataProcessingPurpose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

class DataProcessingCatalogTest {

    private DataProcessingCatalog catalog;

    @BeforeEach
    void setUp() {
        catalog = new DataProcessingCatalog();
    }

    @Test
    void shouldReturnNonEmptyActiveTreatments() {
        List<DataProcessingPurpose> treatments = catalog.getActiveTreatments();
        assertNotNull(treatments);
        assertFalse(treatments.isEmpty());
    }

    @Test
    void shouldContainKnownTreatmentCodes() {
        List<DataProcessingPurpose> treatments = catalog.getActiveTreatments();
        List<String> codes = treatments.stream().map(DataProcessingPurpose::code).toList();
        assertTrue(codes.contains("EMPLOYEE_IDENTIFICATION"));
        assertTrue(codes.contains("BIOMETRIC_AUTHENTICATION"));
        assertTrue(codes.contains("TIME_RECORD_CONTROL"));
    }

    @Test
    void shouldFindTreatmentByKnownCode() {
        DataProcessingPurpose result = catalog.getTreatmentByCode("EMPLOYEE_IDENTIFICATION");
        assertNotNull(result);
        assertEquals("EMPLOYEE_IDENTIFICATION", result.code());
    }

    @Test
    void shouldReturnNullForUnknownCode() {
        DataProcessingPurpose result = catalog.getTreatmentByCode("NONEXISTENT_CODE");
        assertNull(result);
    }

    @Test
    void shouldReturnPublicTreatmentsForActivePurposes() {
        List<PublicDataProcessingPurposeResponse> publicTreatments = catalog.getPublicTreatments();
        assertNotNull(publicTreatments);
        assertFalse(publicTreatments.isEmpty());
        // All returned items must have non-null code
        publicTreatments.forEach(t -> assertNotNull(t.code()));
    }

    @Test
    void shouldFindBiometricTreatment() {
        DataProcessingPurpose biometric = catalog.getTreatmentByCode("BIOMETRIC_AUTHENTICATION");
        assertNotNull(biometric);
        // biometric requires consent
        assertTrue(biometric.sensitive()); // biometric is sensitive data
    }

    @Test
    void shouldMapAllKnownCodesToPublicPurpose() {
        List<DataProcessingPurpose> all = catalog.getActiveTreatments();
        List<PublicDataProcessingPurposeResponse> publicList = catalog.getPublicTreatments();
        // getPublicTreatments filters only active ones
        long activeCount = all.stream().filter(DataProcessingPurpose::active).count();
        assertEquals(activeCount, publicList.size());
    }

    @Test
    void getPublicPurpose_unknownCode_returnsOriginalTechnicalPurpose() throws Exception {
        var method = DataProcessingCatalog.class.getDeclaredMethod("getPublicPurpose", String.class, String.class);
        method.setAccessible(true);
        var result = method.invoke(catalog, "UNKNOWN_CODE_XYZ", "my technical purpose");
        assertEquals("my technical purpose", result);
    }

}