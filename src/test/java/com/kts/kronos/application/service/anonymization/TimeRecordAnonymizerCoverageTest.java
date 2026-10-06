package com.kts.kronos.application.service.anonymization;

import com.kts.kronos.adapter.out.persistence.TimeRecordRepository;
import com.kts.kronos.application.security.PrivacyLogReferenceService;
import com.kts.kronos.domain.model.AnonymizationPlan;
import com.kts.kronos.domain.model.enuns.AnonymizationResourceType;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import com.kts.kronos.adapter.out.persistence.entity.TimeRecordEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Covers the || chain branches in hasGeolocation / hadGeolocation:
 *   lat==null && lng!=null  → covers `longitude != null` TRUE
 *   lat==null && lng==null && endLat!=null → covers `endLatitude != null` TRUE
 *   lat==null && lng==null && endLat==null && endLng!=null → covers `endLongitude != null` TRUE
 */
@ExtendWith(MockitoExtension.class)
class TimeRecordAnonymizerCoverageTest {

    @Mock
    private PrivacyLogReferenceService privacyLogReferenceService;

    @Mock
    private TimeRecordRepository timeRecordRepository;

    @InjectMocks
    private TimeRecordAnonymizer anonymizer;

    // ── DRY_RUN: lat=null, lng!=null → covers longitude!=null TRUE branch ────

    @Test
    void executeDryRun_withOnlyLongitudeSet_countsAsAffected() {
        var record = buildRecord();
        record.setLatitude(null);
        record.setLongitude(-46.6333);  // only lng → lat==null branch → lng!=null=TRUE
        record.setEndLatitude(null);
        record.setEndLongitude(null);

        when(timeRecordRepository.findByEmployeeId(any())).thenReturn(List.of(record));

        var result = anonymizer.execute(createPlan(true), "DRY_RUN");
        assertEquals(1, result.affectedCount()); // hasGeolocation = TRUE (via lng)
        assertEquals(0, result.skippedCount());
    }

    // ── DRY_RUN: lat=null, lng=null, endLat!=null → covers endLatitude!=null TRUE ─

    @Test
    void executeDryRun_withOnlyEndLatitudeSet_countsAsAffected() {
        var record = buildRecord();
        record.setLatitude(null);
        record.setLongitude(null);
        record.setEndLatitude(-23.5505);  // lat/lng null, endLat!=null → TRUE
        record.setEndLongitude(null);

        when(timeRecordRepository.findByEmployeeId(any())).thenReturn(List.of(record));

        var result = anonymizer.execute(createPlan(true), "DRY_RUN");
        assertEquals(1, result.affectedCount());
    }

    // ── DRY_RUN: all null except endLng → covers endLongitude!=null TRUE ────

    @Test
    void executeDryRun_withOnlyEndLongitudeSet_countsAsAffected() {
        var record = buildRecord();
        record.setLatitude(null);
        record.setLongitude(null);
        record.setEndLatitude(null);
        record.setEndLongitude(-46.6333);  // only endLng → covers last OR branch TRUE

        when(timeRecordRepository.findByEmployeeId(any())).thenReturn(List.of(record));

        var result = anonymizer.execute(createPlan(true), "DRY_RUN");
        assertEquals(1, result.affectedCount());
    }

    // ── APPLY: lat=null, lng!=null → covers longitude!=null TRUE branch ──────

    @Test
    void executeApply_withOnlyLongitudeSet_anonymizesRecord() {
        var record = buildRecord();
        record.setLatitude(null);
        record.setLongitude(-46.6333);
        record.setEndLatitude(null);
        record.setEndLongitude(null);

        when(timeRecordRepository.findByEmployeeId(any())).thenReturn(List.of(record));

        var result = anonymizer.execute(createPlan(true), "APPLY");
        assertEquals(1, result.affectedCount());
    }

    // ── APPLY: lat=null, lng=null, endLat!=null → covers endLatitude!=null TRUE ─

    @Test
    void executeApply_withOnlyEndLatitudeSet_anonymizesRecord() {
        var record = buildRecord();
        record.setLatitude(null);
        record.setLongitude(null);
        record.setEndLatitude(-23.5505);
        record.setEndLongitude(null);

        when(timeRecordRepository.findByEmployeeId(any())).thenReturn(List.of(record));

        var result = anonymizer.execute(createPlan(true), "APPLY");
        assertEquals(1, result.affectedCount());
    }

    // ── APPLY: only endLng → covers endLongitude!=null TRUE branch ──────────

    @Test
    void executeApply_withOnlyEndLongitudeSet_anonymizesRecord() {
        var record = buildRecord();
        record.setLatitude(null);
        record.setLongitude(null);
        record.setEndLatitude(null);
        record.setEndLongitude(-46.6333);

        when(timeRecordRepository.findByEmployeeId(any())).thenReturn(List.of(record));

        var result = anonymizer.execute(createPlan(true), "APPLY");
        assertEquals(1, result.affectedCount());
    }

    private AnonymizationPlan createPlan(boolean preserveLaborData) {
        return new AnonymizationPlan(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Test", preserveLaborData, true, false, false, false, false
        );
    }

    private TimeRecordEntity buildRecord() {
        return TimeRecordEntity.builder()
                .timeRecordId(1L)
                .employeeId(UUID.randomUUID())
                .startWork(LocalDateTime.now())
                .endWork(LocalDateTime.now().plusHours(8))
                .statusRecord(StatusRecord.CREATED)
                .active(true)
                .build();
    }
}
