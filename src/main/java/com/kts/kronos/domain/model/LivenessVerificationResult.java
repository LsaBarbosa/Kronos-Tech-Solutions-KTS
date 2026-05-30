package com.kts.kronos.domain.model;

public record LivenessVerificationResult(
        boolean passed,
        String provider,
        String reasonCode,
        Double confidence
) {
    public static LivenessVerificationResult passed(String provider, Double confidence) {
        return new LivenessVerificationResult(true, provider, "PASSED", confidence);
    }

    public static LivenessVerificationResult failed(String provider, String reasonCode) {
        return new LivenessVerificationResult(false, provider, reasonCode, null);
    }

    public static LivenessVerificationResult error(String provider, String reasonCode) {
        return new LivenessVerificationResult(false, provider, reasonCode, null);
    }
}
