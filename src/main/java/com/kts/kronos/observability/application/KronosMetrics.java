package com.kts.kronos.observability.application;

import com.kts.kronos.observability.support.ObservabilityTagSanitizer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class KronosMetrics {

    private final MeterRegistry meterRegistry;
    private final ObservabilityTagSanitizer tagSanitizer;
    private final AtomicReference<Double> ntpDriftSeconds = new AtomicReference<>(0.0d);

    public KronosMetrics(MeterRegistry meterRegistry) {
        this(meterRegistry, new ObservabilityTagSanitizer());
    }

    @Autowired
    public KronosMetrics(MeterRegistry meterRegistry, ObservabilityTagSanitizer tagSanitizer) {
    this.meterRegistry = meterRegistry;
    this.tagSanitizer = tagSanitizer;
    Gauge.builder("kronos_ntp_drift_seconds", ntpDriftSeconds, AtomicReference::get)
            .description("Current NTP drift in seconds")
            .register(meterRegistry);
    }

    public void recordAuthLogin(String method, String result, String reason) {
        increment("kronos_auth_login_total",
                "method", method,
                "result", result,
                "reason", reason
        );
    }

    public void recordPasswordRecovery(String result, String reason) {
        increment("kronos_auth_password_recovery_total",
                "result", result,
                "reason", reason
        );
    }

    public void recordPasswordReset(String result, String reason) {
        increment("kronos_auth_password_reset_total",
                "result", result,
                "reason", reason
        );
    }

    public void recordTokenRefresh(String result, String reason) {
        increment("kronos_auth_token_refresh_total",
                "result", result,
                "reason", reason
        );
    }

    public void recordTimeRecordOperation(String operation, String result, String reason) {
        increment("kronos_time_record_operation_total",
                "operation", operation,
                "result", result,
                "reason", reason
        );
    }

    public void recordTimeRecordOperationDuration(String operation, Duration duration, String result) {
        record("kronos_time_record_operation_duration_seconds", duration,
                "operation", operation,
                "result", result
        );
    }

    public void recordDocumentOperation(String operation, String documentType, String result, String reason) {
        increment("kronos_document_operation_total",
                "operation", operation,
                "document_type", documentType,
                "result", result,
                "reason", reason
        );
    }

    public void recordDocumentOperationDuration(String operation, String documentType, Duration duration, String result) {
        record("kronos_document_operation_duration_seconds", duration,
                "operation", operation,
                "document_type", documentType,
                "result", result
        );
    }

    public void recordLegalGeneration(String documentType, String result, String reason) {
        increment("kronos_legal_generation_total",
                "legal_document_type", documentType,
                "result", result,
                "reason", reason
        );
    }

    public void recordLegalGenerationDuration(String documentType, Duration duration, String result) {
        record("kronos_legal_generation_duration_seconds", duration,
                "legal_document_type", documentType,
                "result", result
        );
    }

    public void recordLgpdRequest(String eventType, String status, String result) {
        increment("kronos_lgpd_request_total",
                "event_type", eventType,
                "status", status,
                "result", result
        );
    }

    public void recordLgpdAnonymization(String mode, String result, String reason) {
        increment("kronos_lgpd_anonymization_total",
                "mode", mode,
                "result", result,
                "reason", reason
        );
    }

    public void recordRetentionExecution(String mode, String result, String reason) {
        increment("kronos_retention_execution_total",
                "mode", mode,
                "result", result,
                "reason", reason
        );
    }

    public void recordSecurityIncident(String eventType, String severity, String status, String result) {
        increment("kronos_security_incident_total",
                "event_type", eventType,
                "severity", severity,
                "status", status,
                "result", result
        );
    }

    public void recordSchedulerExecution(String scheduler, String result, String reason) {
        increment("kronos_scheduler_execution_total",
                "scheduler", scheduler,
                "result", result,
                "reason", reason
        );
    }

    public void recordSchedulerDuration(String scheduler, Duration duration, String result) {
        record("kronos_scheduler_execution_duration_seconds", duration,
                "scheduler", scheduler,
                "result", result
        );
    }

    public void recordExternalProviderRequest(String provider, String operation, String result, String reason) {
        increment("kronos_external_provider_request_total",
                "provider", provider,
                "operation", operation,
                "result", result,
                "reason", reason
        );
    }

    public void recordExternalProviderRequestDuration(String provider, String operation, Duration duration, String result) {
        record("kronos_external_provider_request_duration_seconds", duration,
                "provider", provider,
                "operation", operation,
                "result", result
        );
    }

    public void recordFrontendEvent(String eventType, String result, String reason) {
        increment("kronos_frontend_event_total",
                "event_type", eventType,
                "result", result,
                "reason", reason
        );
    }

    public void setNtpDriftMillis(Long offsetMillis) {
        double drift = offsetMillis == null ? 0.0d : offsetMillis / 1000.0d;
        ntpDriftSeconds.set(drift);
    }

    public void authLoginSuccess() {
        recordAuthLogin("password", "success", "none");
    }

    public void authLoginFailure(String reason) {
        recordAuthLogin("password", "failure", reason);
    }

    public void authFaceLoginSuccess() {
        recordAuthLogin("face", "success", "none");
    }

    public void authFaceLoginFailure(String reason) {
        recordAuthLogin("face", "failure", reason);
    }

    public void passwordRecoveryRequested() {
        recordPasswordRecovery("accepted", "request_received");
    }

    public void passwordRecoveryEmailSent() {
        recordPasswordRecovery("success", "email_sent");
    }

    public void passwordRecoveryFailure(String reason) {
        recordPasswordRecovery("failure", reason);
    }

    public void passwordResetSuccess() {
        recordPasswordReset("success", "none");
    }

    public void passwordResetFailure(String reason) {
        recordPasswordReset("failure", reason);
    }

    public void timeRecordCheckinSuccess() {
        recordTimeRecordOperation("checkin", "success", "none");
    }

    public void timeRecordCheckoutSuccess() {
        recordTimeRecordOperation("checkout", "success", "none");
    }

    public void timeRecordImplicitBreak() {
        recordTimeRecordOperation("implicit_break", "success", "none");
    }

    public void timeRecordDayOffConverted() {
        recordTimeRecordOperation("checkin_on_day_off", "success", "none");
    }

    public void timeRecordAbsenceConverted() {
        recordTimeRecordOperation("absence_converted", "success", "none");
    }

    public void timeRecordFailure(String reason) {
        recordTimeRecordOperation("register", "failure", reason);
    }

    public void recordTimeRecordDuration(String operation, Duration duration) {
        recordTimeRecordOperationDuration(operation, duration, "success");
    }

    public void documentUploadSuccess(String documentType) {
        recordDocumentOperation("upload", documentType, "success", "none");
    }

    public void documentUploadFailure(String documentType, String reason) {
        recordDocumentOperation("upload", documentType, "failure", reason);
    }

    public void documentDownloadSuccess(String documentType) {
        recordDocumentOperation("download", documentType, "success", "none");
    }

    public void documentDownloadFailure(String documentType, String reason) {
        recordDocumentOperation("download", documentType, "failure", reason);
    }

    public void documentDeleteSuccess(String documentType) {
        recordDocumentOperation("delete", documentType, "success", "none");
    }

    public void documentDeleteFailure(String documentType, String reason) {
        recordDocumentOperation("delete", documentType, "failure", reason);
    }

    public void legalSuccess(String documentType) {
        recordLegalGeneration(documentType, "success", "none");
    }

    public void legalFailure(String documentType, String reason) {
        recordLegalGeneration(documentType, "failure", reason);
    }

    public void recordLegalDuration(String documentType, Duration duration, String result) {
        recordLegalGenerationDuration(documentType, duration, result);
    }

    public void schedulerSuccess(String scheduler) {
        recordSchedulerExecution(scheduler, "success", "none");
    }

    public void schedulerFailure(String scheduler) {
        recordSchedulerExecution(scheduler, "failure", "unknown");
    }

    public void schedulerRecordsProcessed(String scheduler, double amount) {
        meterRegistry.counter("kronos_scheduler_records_processed_total",
                tags("scheduler", scheduler)
        ).increment(amount);
    }

    public void companyCreated() { increment("kronos_company_created_total"); }
    public void companyUpdated() { increment("kronos_company_updated_total"); }
    public void employeeCreated() { increment("kronos_employee_created_total"); }
    public void employeeUpdated() { increment("kronos_employee_updated_total"); }
    public void userCreated() { increment("kronos_user_created_total"); }
    public void userUpdated() { increment("kronos_user_updated_total"); }
    public void consentAccepted() { increment("kronos_consent_accepted_total"); }
    public void consentRevoked() { increment("kronos_consent_revoked_total"); }

    public void biometricEnrollmentSuccess() {
        increment("kronos_biometric_enrollment_success_total");
    }

    public void biometricEnrollmentFailure(String reason) {
        increment("kronos_biometric_enrollment_failure_total", "reason", reason);
    }

    public void recordBiometricEnrollmentDuration(Duration duration) {
        record("kronos_biometric_enrollment_duration_seconds", duration);
    }

    public void geolocationLookupSuccess() {
        increment("kronos_geolocation_lookup_success_total");
    }

    public void geolocationLookupFailure(String reason) {
        increment("kronos_geolocation_lookup_failure_total", "reason", reason);
    }

    public void recordGeolocationDuration(Duration duration) {
        record("kronos_geolocation_lookup_duration_seconds", duration);
    }

    public void timeAdjustmentRequested() { increment("kronos_time_adjustment_requested_total"); }
    public void timeAdjustmentApproved() { increment("kronos_time_adjustment_approved_total"); }
    public void timeAdjustmentRejected() { increment("kronos_time_adjustment_rejected_total"); }
    public void vacationRequested() { increment("kronos_vacation_requested_total"); }
    public void vacationApproved() { increment("kronos_vacation_approved_total"); }
    public void vacationRejected() { increment("kronos_vacation_rejected_total"); }
    public void timeOffRequested() { increment("kronos_time_off_requested_total"); }
    public void timeOffApproved() { increment("kronos_time_off_approved_total"); }
    public void timeOffRejected() { increment("kronos_time_off_rejected_total"); }

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
        Tags tags = Tags.empty();
        if (tagKeyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Metric tag key/value arguments must be even");
        }

        for (int i = 0; i < tagKeyValues.length; i += 2) {
            tags = tags.and(
                    tagSanitizer.sanitizeTagKey(tagKeyValues[i]),
                    tagSanitizer.sanitizeTagValue(tagKeyValues[i], tagKeyValues[i + 1])
            );
        }
        return tags;
    }
}
