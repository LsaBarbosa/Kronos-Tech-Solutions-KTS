package com.kts.kronos.observability.application;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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

        // New metrics
        metrics.companyCreated();
        metrics.companyUpdated();
        metrics.employeeCreated();
        metrics.employeeUpdated();
        metrics.biometricEnrollmentSuccess();
        metrics.biometricEnrollmentFailure("storage_error");
        metrics.recordBiometricEnrollmentDuration(Duration.ofMillis(300));
        metrics.userCreated();
        metrics.userUpdated();
        metrics.consentAccepted();
        metrics.consentRevoked();
        metrics.geolocationLookupSuccess();
        metrics.geolocationLookupFailure("provider_error");
        metrics.recordGeolocationDuration(Duration.ofMillis(150));
        metrics.timeAdjustmentRequested();
        metrics.timeAdjustmentApproved();
        metrics.timeAdjustmentRejected();
        metrics.vacationRequested();
        metrics.vacationApproved();
        metrics.vacationRejected();
        metrics.timeOffRequested();
        metrics.timeOffApproved();
        metrics.timeOffRejected();

        assertEquals(1.0d, registry.get("kronos_auth_login_total").tag("method", "password").tag("result", "success").tag("reason", "none").counter().count());
        assertEquals(1.0d, registry.get("kronos_auth_login_total").tag("method", "password").tag("result", "failure").tag("reason", "invalid_credentials").counter().count());
        assertEquals(1.0d, registry.get("kronos_auth_login_total").tag("method", "face").tag("result", "success").tag("reason", "none").counter().count());
        assertEquals(1.0d, registry.get("kronos_auth_login_total").tag("method", "face").tag("result", "failure").tag("reason", "face_not_recognized").counter().count());
        assertEquals(1.0d, registry.get("kronos_auth_password_recovery_total").tag("result", "accepted").tag("reason", "request_received").counter().count());
        assertEquals(1.0d, registry.get("kronos_auth_password_recovery_total").tag("result", "success").tag("reason", "email_sent").counter().count());
        assertEquals(1.0d, registry.get("kronos_auth_password_recovery_total").tag("result", "failure").tag("reason", "rate_limited").counter().count());
        assertEquals(1.0d, registry.get("kronos_auth_password_reset_total").tag("result", "success").tag("reason", "none").counter().count());
        assertEquals(1.0d, registry.get("kronos_auth_password_reset_total").tag("result", "failure").tag("reason", "validation").counter().count());
        assertEquals(1.0d, registry.get("kronos_time_record_operation_total").tag("operation", "checkin").tag("result", "success").tag("reason", "none").counter().count());
        assertEquals(1.0d, registry.get("kronos_time_record_operation_total").tag("operation", "checkout").tag("result", "success").tag("reason", "none").counter().count());
        assertEquals(1.0d, registry.get("kronos_time_record_operation_total").tag("operation", "implicit_break").tag("result", "success").tag("reason", "none").counter().count());
        assertEquals(1.0d, registry.get("kronos_time_record_operation_total").tag("operation", "checkin_on_day_off").tag("result", "success").tag("reason", "none").counter().count());
        assertEquals(1.0d, registry.get("kronos_time_record_operation_total").tag("operation", "absence_converted").tag("result", "success").tag("reason", "none").counter().count());
        assertEquals(1.0d, registry.get("kronos_time_record_operation_total").tag("operation", "register").tag("result", "failure").tag("reason", "ntp").counter().count());
        assertEquals(1L, registry.get("kronos_time_record_operation_duration_seconds").tag("operation", "checkin").tag("result", "success").timer().count());
        assertEquals(1.0d, registry.get("kronos_document_operation_total").tag("operation", "upload").tag("document_type", "time_off").tag("result", "success").tag("reason", "none").counter().count());
        assertEquals(1.0d, registry.get("kronos_document_operation_total").tag("operation", "upload").tag("document_type", "time_off").tag("result", "failure").tag("reason", "validation").counter().count());
        assertEquals(1.0d, registry.get("kronos_document_operation_total").tag("operation", "download").tag("document_type", "time_off").tag("result", "success").tag("reason", "none").counter().count());
        assertEquals(1.0d, registry.get("kronos_document_operation_total").tag("operation", "download").tag("document_type", "time_off").tag("result", "failure").tag("reason", "not_found").counter().count());
        assertEquals(1.0d, registry.get("kronos_document_operation_total").tag("operation", "delete").tag("document_type", "time_off").tag("result", "success").tag("reason", "none").counter().count());
        assertEquals(1.0d, registry.get("kronos_document_operation_total").tag("operation", "delete").tag("document_type", "time_off").tag("result", "failure").tag("reason", "validation").counter().count());
        assertEquals(1.0d, registry.get("kronos_legal_generation_total").tag("legal_document_type", "afd").tag("result", "success").tag("reason", "none").counter().count());
        assertEquals(1.0d, registry.get("kronos_legal_generation_total").tag("legal_document_type", "afd").tag("result", "failure").tag("reason", "generation").counter().count());
        assertEquals(1L, registry.get("kronos_legal_generation_duration_seconds").tag("legal_document_type", "afd").tag("result", "success").timer().count());
        assertEquals(1.0d, registry.get("kronos_scheduler_execution_total").tag("scheduler", "time_sync").tag("result", "success").tag("reason", "none").counter().count());
        assertEquals(1.0d, registry.get("kronos_scheduler_execution_total").tag("scheduler", "time_sync").tag("result", "failure").tag("reason", "unknown").counter().count());
        assertEquals(1L, registry.get("kronos_scheduler_execution_duration_seconds").tag("scheduler", "time_sync").tag("result", "failure").timer().count());
        assertEquals(4.0d, registry.get("kronos_scheduler_records_processed_total").tag("scheduler", "time_sync").counter().count());
        assertEquals(5.0d, registry.get("kronos_ntp_drift_seconds").gauge().value());

        // New metric assertions
        assertEquals(1.0d, registry.get("kronos_company_created_total").counter().count());
        assertEquals(1.0d, registry.get("kronos_company_updated_total").counter().count());
        assertEquals(1.0d, registry.get("kronos_employee_created_total").counter().count());
        assertEquals(1.0d, registry.get("kronos_employee_updated_total").counter().count());
        assertEquals(1.0d, registry.get("kronos_biometric_enrollment_success_total").counter().count());
        assertEquals(1.0d, registry.get("kronos_biometric_enrollment_failure_total").tag("reason", "storage_error").counter().count());
        assertEquals(1L,   registry.get("kronos_biometric_enrollment_duration_seconds").timer().count());
        assertEquals(1.0d, registry.get("kronos_user_created_total").counter().count());
        assertEquals(1.0d, registry.get("kronos_user_updated_total").counter().count());
        assertEquals(1.0d, registry.get("kronos_consent_accepted_total").counter().count());
        assertEquals(1.0d, registry.get("kronos_consent_revoked_total").counter().count());
        assertEquals(1.0d, registry.get("kronos_geolocation_lookup_success_total").counter().count());
        assertEquals(1.0d, registry.get("kronos_geolocation_lookup_failure_total").tag("reason", "provider_error").counter().count());
        assertEquals(1L,   registry.get("kronos_geolocation_lookup_duration_seconds").timer().count());
        assertEquals(1.0d, registry.get("kronos_time_adjustment_requested_total").counter().count());
        assertEquals(1.0d, registry.get("kronos_time_adjustment_approved_total").counter().count());
        assertEquals(1.0d, registry.get("kronos_time_adjustment_rejected_total").counter().count());
        assertEquals(1.0d, registry.get("kronos_vacation_requested_total").counter().count());
        assertEquals(1.0d, registry.get("kronos_vacation_approved_total").counter().count());
        assertEquals(1.0d, registry.get("kronos_vacation_rejected_total").counter().count());
        assertEquals(1.0d, registry.get("kronos_time_off_requested_total").counter().count());
        assertEquals(1.0d, registry.get("kronos_time_off_approved_total").counter().count());
        assertEquals(1.0d, registry.get("kronos_time_off_rejected_total").counter().count());
    }

    @Test
    void shouldRejectUnsupportedMetricTagKeys() {
        var metrics = new KronosMetrics(new SimpleMeterRegistry());

        assertThrows(
                IllegalArgumentException.class,
                () -> ReflectionTestUtils.invokeMethod(metrics, "increment", "kronos_test_total", new String[]{"employeeId", "123"})
        );
    }

    // ── recordDocumentOperationDuration / recordSecurityIncident / recordFrontendEvent ─
    @Test
    void shouldCoverUncalledMetricsMethods() {
        var registry = new SimpleMeterRegistry();
        var metrics = new KronosMetrics(registry);

        metrics.recordDocumentOperationDuration("upload", "time_off", Duration.ofMillis(120), "success");
        metrics.recordSecurityIncident("login_failure", "MEDIUM", "active", "failure");
        metrics.recordFrontendEvent("page_load", "success", "none");

        assertEquals(1L, registry.get("kronos_document_operation_duration_seconds")
            .tag("operation", "upload").tag("document_type", "time_off").tag("result", "success").timer().count());
        assertEquals(1.0d, registry.get("kronos_security_incident_total")
            .tag("event_type", "login_failure").tag("severity", "medium").tag("status", "active").tag("result", "failure").counter().count());
        assertEquals(1.0d, registry.get("kronos_frontend_event_total")
            .tag("event_type", "page_load").tag("result", "success").tag("reason", "none").counter().count());
    }

    @Test
    void shouldExposeOnlyGaugeBeforeCountersAreEmitted() {
        var registry = new SimpleMeterRegistry();
        new KronosMetrics(registry);

        assertEquals(0.0d, registry.get("kronos_ntp_drift_seconds").gauge().value());
        assertEquals(0.0d, registry.get("kronos_company_created_total").counter().count());
        assertEquals(0.0d, registry.get("kronos_consent_accepted_total").counter().count());
        assertEquals(0.0d, registry.get("kronos_vacation_requested_total").counter().count());
        assertEquals(0.0d, registry.get("kronos_time_off_approved_total").counter().count());
    }

    @Test
    void tags_oddNumberOfArgs_throwsIllegalArgumentException() throws Exception {
        var registry = new SimpleMeterRegistry();
        var metrics = new KronosMetrics(registry);
        Method tagsMethod = KronosMetrics.class.getDeclaredMethod("tags", String[].class);
        tagsMethod.setAccessible(true);
        var ex = assertThrows(InvocationTargetException.class,
                () -> tagsMethod.invoke(metrics, (Object) new String[]{"single_key"}));
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
    }

}