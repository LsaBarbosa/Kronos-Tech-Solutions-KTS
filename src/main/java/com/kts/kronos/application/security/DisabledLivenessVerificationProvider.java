package com.kts.kronos.application.security;

import com.kts.kronos.application.port.out.provider.LivenessVerificationProvider;
import com.kts.kronos.domain.model.LivenessVerificationResult;
import com.kts.kronos.domain.model.enuns.LivenessOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@Profile({"prod", "production"})
@ConditionalOnMissingBean(LivenessVerificationProvider.class)
public class DisabledLivenessVerificationProvider implements LivenessVerificationProvider {
    static final String PROVIDER_NAME = "LIVENESS_PROVIDER_NOT_CONFIGURED";

    private final PrivacyLogReferenceService privacyLogReferenceService;

    public DisabledLivenessVerificationProvider(PrivacyLogReferenceService privacyLogReferenceService) {
        this.privacyLogReferenceService = privacyLogReferenceService;
    }

    @Override
    public LivenessVerificationResult verify(String faceImageBase64, LivenessOperation operation, UUID employeeId) {
        log.error("event=liveness_provider_missing operation={} employeeRef={}",
                operation, privacyLogReferenceService.employeeRef(employeeId));
        return LivenessVerificationResult.error(PROVIDER_NAME, "REAL_LIVENESS_PROVIDER_NOT_CONFIGURED");
    }
}
