package com.kts.kronos.application.service.anonymization;

import com.kts.kronos.application.security.PrivacyLogReferenceService;
import com.kts.kronos.adapter.out.persistence.TimeRecordRepository;
import com.kts.kronos.adapter.out.persistence.entity.TimeRecordEntity;
import com.kts.kronos.domain.model.AnonymizationPlan;
import com.kts.kronos.domain.model.enuns.AnonymizationResourceType;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeRecordAnonymizerTest {


    @Mock
    private PrivacyLogReferenceService privacyLogReferenceService;

    @Mock
    private TimeRecordRepository timeRecordRepository;

    @InjectMocks
    private TimeRecordAnonymizer anonymizer;

    @Test
    void testSupports() {
        assertEquals(AnonymizationResourceType.TIME_RECORD, anonymizer.supports());
    }

    @Test
    void testExecuteDryRunWithNoTimeRecords() {
        when(timeRecordRepository.findByEmployeeId(any())).thenReturn(new ArrayList<>());

        var plan = createPlan(true);
        var result = anonymizer.execute(plan, "DRY_RUN");

        assertEquals("DRY_RUN", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(0, result.scannedCount());
        assertEquals(0, result.affectedCount());
        assertEquals(0, result.skippedCount());
    }

    @Test
    void testExecuteDryRunWithGeolocationWhenPreserveLaborData() {
        var record1 = createTimeRecord();
        record1.setLatitude(-23.5505);
        record1.setLongitude(-46.6333);

        var record2 = createTimeRecord();

        when(timeRecordRepository.findByEmployeeId(any())).thenReturn(Arrays.asList(record1, record2));

        var plan = createPlan(true);
        var result = anonymizer.execute(plan, "DRY_RUN");

        assertEquals("DRY_RUN", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(2, result.scannedCount());
        assertEquals(1, result.affectedCount());
        assertEquals(1, result.skippedCount());
    }

    @Test
    void testExecuteDryRunAllAffectedWhenPreserveLaborDataFalse() {
        var record1 = createTimeRecord();
        var record2 = createTimeRecord();

        when(timeRecordRepository.findByEmployeeId(any())).thenReturn(Arrays.asList(record1, record2));

        var plan = createPlan(false);
        var result = anonymizer.execute(plan, "DRY_RUN");

        assertEquals("DRY_RUN", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(2, result.scannedCount());
        assertEquals(2, result.affectedCount());
        assertEquals(0, result.skippedCount());
    }

    @Test
    void testExecuteApplyWithPreserveLaborDataTrue() {
        var record1 = createTimeRecord();
        record1.setLatitude(-23.5505);
        record1.setLongitude(-46.6333);
        record1.setEndLatitude(-23.5505);
        record1.setEndLongitude(-46.6333);

        var record2 = createTimeRecord();

        when(timeRecordRepository.findByEmployeeId(any())).thenReturn(Arrays.asList(record1, record2));

        var plan = createPlan(true);
        var result = anonymizer.execute(plan, "APPLY");

        assertEquals("APPLY", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(2, result.scannedCount());
        assertEquals(1, result.affectedCount());
        assertEquals(1, result.skippedCount());

        verify(timeRecordRepository, times(1)).save(any());
    }

    @Test
    void testExecuteApplyWithPreserveLaborDataFalse() {
        var record1 = createTimeRecord();
        record1.setLatitude(-23.5505);
        record1.setLongitude(-46.6333);

        var record2 = createTimeRecord();

        when(timeRecordRepository.findByEmployeeId(any())).thenReturn(Arrays.asList(record1, record2));

        var plan = createPlan(false);
        var result = anonymizer.execute(plan, "APPLY");

        assertEquals("APPLY", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(2, result.scannedCount());
        assertEquals(2, result.affectedCount());
        assertEquals(0, result.skippedCount());

        verify(timeRecordRepository, times(2)).save(any());
    }

    @Test
    void testExecuteApplyRemovesLocationCoordinates() {
        var record = createTimeRecord();
        record.setLatitude(-23.5505);
        record.setLongitude(-46.6333);
        when(timeRecordRepository.findByEmployeeId(any())).thenReturn(Arrays.asList(record));

        anonymizer.execute(createPlan(true), "APPLY");

        var savedCaptor = org.mockito.ArgumentCaptor.forClass(TimeRecordEntity.class);
        verify(timeRecordRepository).save(savedCaptor.capture());

        var saved = savedCaptor.getValue();
        assertNull(saved.getLatitude());
        assertNull(saved.getLongitude());
        assertNull(saved.getEndLatitude());
        assertNull(saved.getEndLongitude());
    }

    @Test
    void testExecuteApplyHandlesException() {
        when(timeRecordRepository.findByEmployeeId(any())).thenThrow(new RuntimeException("DB error"));

        var plan = createPlan(true);
        var result = anonymizer.execute(plan, "APPLY");

        assertEquals("APPLY", result.executionMode());
        assertEquals("ERROR", result.status());
        assertEquals(1, result.errorCount());
    }

    private AnonymizationPlan createPlan(boolean preserveLaborData) {
        return new AnonymizationPlan(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Test anonymization",
                preserveLaborData,
                true,
                false,
                false,
                false,
                false
        );
    }

    private TimeRecordEntity createTimeRecord() {
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
