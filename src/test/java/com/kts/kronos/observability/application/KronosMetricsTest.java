package com.kts.kronos.observability.application;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KronosMetricsTest {

    @Test
    void shouldRegisterBusinessCountersTimersAndGaugeWithoutSensitiveTags() {
        var registry = new SimpleMeterRegistry();
        var metrics = new KronosMetrics(registry);

        metrics.authLoginSuccess();
        metrics.authLoginFailure("invalid_credentials");
        metrics.authFaceLoginSuccess();
        metrics.authFaceLoginFailure("face_not_recognized");
        metrics.passwordRecoveryRequested();
        metrics.passwordRecoveryEmailSent();
        metrics.passwordRecoveryFailure("rate_limited");
        metrics.passwordResetSuccess();
        metrics.passwordResetFailure("validation");
        metrics.timeRecordCheckinSuccess();
        metrics.timeRecordCheckoutSuccess();
        metrics.timeRecordImplicitBreak();
        metrics.timeRecordDayOffConverted();
        metrics.timeRecordAbsenceConverted();
        metrics.timeRecordFailure("ntp");
        metrics.recordTimeRecordDuration("checkin", Duration.ofSeconds(2));
        metrics.documentUploadSuccess("time_off");
        metrics.documentUploadFailure("time_off", "validation");
        metrics.documentDownloadSuccess("time_off");
        metrics.documentDownloadFailure("time_off", "not_found");
        metrics.documentDeleteSuccess("time_off");
        metrics.documentDeleteFailure("time_off", "validation");
        metrics.legalSuccess("afd");
        metrics.legalFailure("afd", "generation");
        metrics.recordLegalDuration("afd", Duration.ofMillis(250), "success");
        metrics.schedulerSuccess("time_sync");
        metrics.schedulerFailure("time_sync");
        metrics.recordSchedulerDuration("time_sync", Duration.ofMillis(125), "failure");
        metrics.schedulerRecordsProcessed("time_sync", 4);
        metrics.setNtpDriftMillis(5_000L);

        assertEquals(1.0d, registry.get("kronos_auth_login_success_total").counter().count());
        assertEquals(1.0d, registry.get("kronos_auth_login_failure_total").tag("reason", "invalid_credentials").counter().count());
        assertEquals(1.0d, registry.get("kronos_auth_face_login_success_total").counter().count());
        assertEquals(1.0d, registry.get("kronos_auth_face_login_failure_total").tag("reason", "face_not_recognized").counter().count());
        assertEquals(1.0d, registry.get("kronos_password_recovery_request_total").counter().count());
        assertEquals(1.0d, registry.get("kronos_password_recovery_email_sent_total").counter().count());
        assertEquals(1.0d, registry.get("kronos_password_recovery_failure_total").tag("reason", "rate_limited").counter().count());
        assertEquals(1.0d, registry.get("kronos_password_reset_success_total").counter().count());
        assertEquals(1.0d, registry.get("kronos_password_reset_failure_total").tag("reason", "validation").counter().count());
        assertEquals(1.0d, registry.get("kronos_time_record_checkin_success_total").counter().count());
        assertEquals(1.0d, registry.get("kronos_time_record_checkout_success_total").counter().count());
        assertEquals(1.0d, registry.get("kronos_time_record_implicit_break_total").counter().count());
        assertEquals(1.0d, registry.get("kronos_time_record_day_off_converted_total").counter().count());
        assertEquals(1.0d, registry.get("kronos_time_record_absence_converted_total").counter().count());
        assertEquals(1.0d, registry.get("kronos_time_record_failure_total").tag("reason", "ntp").counter().count());
        assertEquals(1L, registry.get("kronos_time_record_duration_seconds").tag("action", "checkin").timer().count());
        assertEquals(1.0d, registry.get("kronos_document_upload_success_total").tag("document_type", "time_off").counter().count());
        assertEquals(1.0d, registry.get("kronos_document_upload_failure_total").tag("document_type", "time_off").tag("reason", "validation").counter().count());
        assertEquals(1.0d, registry.get("kronos_document_download_success_total").tag("document_type", "time_off").counter().count());
        assertEquals(1.0d, registry.get("kronos_document_download_failure_total").tag("document_type", "time_off").tag("reason", "not_found").counter().count());
        assertEquals(1.0d, registry.get("kronos_document_delete_success_total").tag("document_type", "time_off").counter().count());
        assertEquals(1.0d, registry.get("kronos_document_delete_failure_total").tag("document_type", "time_off").tag("reason", "validation").counter().count());
        assertEquals(1.0d, registry.get("kronos_legal_afd_generation_success_total").tag("legal_document_type", "afd").tag("result", "success").counter().count());
        assertEquals(1.0d, registry.get("kronos_legal_afd_generation_failure_total").tag("legal_document_type", "afd").tag("result", "failure").tag("reason", "generation").counter().count());
        assertEquals(1L, registry.get("kronos_legal_generation_duration_seconds").tag("legal_document_type", "afd").tag("result", "success").timer().count());
        assertEquals(1.0d, registry.get("kronos_scheduler_execution_success_total").tag("scheduler", "time_sync").tag("result", "success").counter().count());
        assertEquals(1.0d, registry.get("kronos_scheduler_execution_failure_total").tag("scheduler", "time_sync").tag("result", "failure").counter().count());
        assertEquals(1L, registry.get("kronos_scheduler_execution_duration_seconds").tag("scheduler", "time_sync").tag("result", "failure").timer().count());
        assertEquals(4.0d, registry.get("kronos_scheduler_records_processed_total").tag("scheduler", "time_sync").counter().count());
        assertEquals(5.0d, registry.get("kronos_ntp_drift_seconds").gauge().value());
    }

    @Test
    void shouldRejectUnsupportedMetricTagKeys() {
        var metrics = new KronosMetrics(new SimpleMeterRegistry());

        assertThrows(
                IllegalArgumentException.class,
                () -> ReflectionTestUtils.invokeMethod(metrics, "increment", "kronos_test_total", new String[]{"employeeId", "123"})
        );
    }
}
