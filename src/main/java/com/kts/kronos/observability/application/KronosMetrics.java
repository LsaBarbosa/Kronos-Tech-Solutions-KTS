package com.kts.kronos.observability.application;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class KronosMetrics {

    private static final Set<String> ALLOWED_TAG_KEYS = Set.of(
            "operation",
            "result",
            "reason",
            "scheduler",
            "document_type",
            "legal_document_type",
            "action"
    );

    private final MeterRegistry meterRegistry;
    private final AtomicReference<Double> ntpDriftSeconds = new AtomicReference<>(0.0d);

    public KronosMetrics() {
        this(new SimpleMeterRegistry());
    }

    public KronosMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        Gauge.builder("kronos_ntp_drift_seconds", ntpDriftSeconds, AtomicReference::get)
                .description("Current NTP drift in seconds")
                .register(meterRegistry);
        preRegisterCounters();
    }

    private void preRegisterCounters() {
        String[] names = {
            "kronos_company_created_total", "kronos_company_updated_total",
            "kronos_employee_created_total", "kronos_employee_updated_total",
            "kronos_user_created_total", "kronos_user_updated_total",
            "kronos_consent_accepted_total", "kronos_consent_revoked_total",
            "kronos_geolocation_lookup_success_total",
            "kronos_biometric_enrollment_success_total",
            "kronos_time_adjustment_requested_total", "kronos_time_adjustment_approved_total",
            "kronos_time_adjustment_rejected_total",
            "kronos_vacation_requested_total", "kronos_vacation_approved_total",
            "kronos_vacation_rejected_total",
            "kronos_time_off_requested_total", "kronos_time_off_approved_total",
            "kronos_time_off_rejected_total"
        };
        for (String name : names) {
            Counter.builder(name).register(meterRegistry);
        }
    }

    public void authLoginSuccess() {
        increment("kronos_auth_login_success_total");
    }

    public void authLoginFailure(String reason) {
        increment("kronos_auth_login_failure_total", "reason", reason);
    }

    public void authFaceLoginSuccess() {
        increment("kronos_auth_face_login_success_total");
    }

    public void authFaceLoginFailure(String reason) {
        increment("kronos_auth_face_login_failure_total", "reason", reason);
    }

    public void passwordRecoveryRequested() {
        increment("kronos_password_recovery_request_total");
    }

    public void passwordRecoveryEmailSent() {
        increment("kronos_password_recovery_email_sent_total");
    }

    public void passwordRecoveryFailure(String reason) {
        increment("kronos_password_recovery_failure_total", "reason", reason);
    }

    public void passwordResetSuccess() {
        increment("kronos_password_reset_success_total");
    }

    public void passwordResetFailure(String reason) {
        increment("kronos_password_reset_failure_total", "reason", reason);
    }

    public void timeRecordCheckinSuccess() {
        increment("kronos_time_record_checkin_success_total");
    }

    public void timeRecordCheckoutSuccess() {
        increment("kronos_time_record_checkout_success_total");
    }

    public void timeRecordImplicitBreak() {
        increment("kronos_time_record_implicit_break_total");
    }

    public void timeRecordDayOffConverted() {
        increment("kronos_time_record_day_off_converted_total");
    }

    public void timeRecordAbsenceConverted() {
        increment("kronos_time_record_absence_converted_total");
    }

    public void timeRecordFailure(String reason) {
        increment("kronos_time_record_failure_total", "reason", reason);
    }

    public void recordTimeRecordDuration(String action, Duration duration) {
        record("kronos_time_record_duration_seconds", duration, "action", action);
    }

    public void documentUploadSuccess(String documentType) {
        increment("kronos_document_upload_success_total", "document_type", documentType);
    }

    public void documentUploadFailure(String documentType, String reason) {
        increment("kronos_document_upload_failure_total", "document_type", documentType, "reason", reason);
    }

    public void documentDownloadSuccess(String documentType) {
        increment("kronos_document_download_success_total", "document_type", documentType);
    }

    public void documentDownloadFailure(String documentType, String reason) {
        increment("kronos_document_download_failure_total", "document_type", documentType, "reason", reason);
    }

    public void documentDeleteSuccess(String documentType) {
        increment("kronos_document_delete_success_total", "document_type", documentType);
    }

    public void documentDeleteFailure(String documentType, String reason) {
        increment("kronos_document_delete_failure_total", "document_type", documentType, "reason", reason);
    }

    public void legalSuccess(String documentType) {
        increment(successMetricFor(documentType), "legal_document_type", documentType, "result", "success");
    }

    public void legalFailure(String documentType, String reason) {
        increment(failureMetricFor(documentType), "legal_document_type", documentType, "result", "failure", "reason", reason);
    }

    public void recordLegalDuration(String documentType, Duration duration, String result) {
        record("kronos_legal_generation_duration_seconds", duration, "legal_document_type", documentType, "result", result);
    }

    public void schedulerSuccess(String scheduler) {
        increment("kronos_scheduler_execution_success_total", "scheduler", scheduler, "result", "success");
    }

    public void schedulerFailure(String scheduler) {
        increment("kronos_scheduler_execution_failure_total", "scheduler", scheduler, "result", "failure");
    }

    public void recordSchedulerDuration(String scheduler, Duration duration, String result) {
        record("kronos_scheduler_execution_duration_seconds", duration, "scheduler", scheduler, "result", result);
    }

    public void schedulerRecordsProcessed(String scheduler, double amount) {
        meterRegistry.counter(
                "kronos_scheduler_records_processed_total",
                tags("scheduler", scheduler)
        ).increment(amount);
    }

    public void setNtpDriftMillis(Long offsetMillis) {
        double drift = offsetMillis == null ? 0.0d : offsetMillis / 1000.0d;
        ntpDriftSeconds.set(drift);
    }

    // --- Company ---
    public void companyCreated() { increment("kronos_company_created_total"); }
    public void companyUpdated() { increment("kronos_company_updated_total"); }

    // --- Employee ---
    public void employeeCreated() { increment("kronos_employee_created_total"); }
    public void employeeUpdated() { increment("kronos_employee_updated_total"); }

    // --- Biometric enrollment ---
    public void biometricEnrollmentSuccess() { increment("kronos_biometric_enrollment_success_total"); }
    public void biometricEnrollmentFailure(String reason) {
        increment("kronos_biometric_enrollment_failure_total", "reason", reason);
    }
    public void recordBiometricEnrollmentDuration(Duration duration) {
        record("kronos_biometric_enrollment_duration_seconds", duration);
    }

    // --- User ---
    public void userCreated() { increment("kronos_user_created_total"); }
    public void userUpdated() { increment("kronos_user_updated_total"); }

    // --- Consent/LGPD ---
    public void consentAccepted() { increment("kronos_consent_accepted_total"); }
    public void consentRevoked()  { increment("kronos_consent_revoked_total"); }

    // --- Geolocation ---
    public void geolocationLookupSuccess() { increment("kronos_geolocation_lookup_success_total"); }
    public void geolocationLookupFailure(String reason) {
        increment("kronos_geolocation_lookup_failure_total", "reason", reason);
    }
    public void recordGeolocationDuration(Duration duration) {
        record("kronos_geolocation_lookup_duration_seconds", duration);
    }

    // --- Time adjustments ---
    public void timeAdjustmentRequested() { increment("kronos_time_adjustment_requested_total"); }
    public void timeAdjustmentApproved()  { increment("kronos_time_adjustment_approved_total"); }
    public void timeAdjustmentRejected()  { increment("kronos_time_adjustment_rejected_total"); }

    // --- Vacation ---
    public void vacationRequested() { increment("kronos_vacation_requested_total"); }
    public void vacationApproved()  { increment("kronos_vacation_approved_total"); }
    public void vacationRejected()  { increment("kronos_vacation_rejected_total"); }

    // --- Time off ---
    public void timeOffRequested() { increment("kronos_time_off_requested_total"); }
    public void timeOffApproved()  { increment("kronos_time_off_approved_total"); }
    public void timeOffRejected()  { increment("kronos_time_off_rejected_total"); }

    private void increment(String name, String... tagKeyValues) {
        meterRegistry.counter(name, tags(tagKeyValues)).increment();
    }

    private void record(String name, Duration duration, String... tagKeyValues) {
        Timer.builder(name)
                .tags(tags(tagKeyValues))
                .register(meterRegistry)
                .record(duration);
    }

    private Iterable<io.micrometer.core.instrument.Tag> tags(String... tagKeyValues) {
        if (tagKeyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Tag key/value arguments must be even");
        }

        Tags tags = Tags.empty();
        for (int i = 0; i < tagKeyValues.length; i += 2) {
            String key = tagKeyValues[i];
            String value = tagKeyValues[i + 1];
            if (!ALLOWED_TAG_KEYS.contains(key)) {
                throw new IllegalArgumentException("Unsupported metric tag: " + key);
            }
            tags = tags.and(key, value == null || value.isBlank() ? "unknown" : value);
        }
        return tags;
    }

    private String successMetricFor(String documentType) {
        return switch (documentType) {
            case "afd" -> "kronos_legal_afd_generation_success_total";
            case "aej" -> "kronos_legal_aej_generation_success_total";
            case "point_mirror" -> "kronos_legal_point_mirror_generation_success_total";
            case "technical_certificate" -> "kronos_legal_technical_certificate_success_total";
            default -> throw new IllegalArgumentException("Unsupported legal document type: " + documentType);
        };
    }

    private String failureMetricFor(String documentType) {
        return switch (documentType) {
            case "afd" -> "kronos_legal_afd_generation_failure_total";
            case "aej" -> "kronos_legal_aej_generation_failure_total";
            case "point_mirror" -> "kronos_legal_point_mirror_generation_failure_total";
            case "technical_certificate" -> "kronos_legal_technical_certificate_failure_total";
            default -> throw new IllegalArgumentException("Unsupported legal document type: " + documentType);
        };
    }
}
