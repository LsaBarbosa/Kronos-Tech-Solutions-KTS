package com.kts.kronos.application.security;

import com.kts.kronos.application.port.out.provider.LivenessVerificationProvider;
import com.kts.kronos.application.util.SensitiveDataMasker;
import com.kts.kronos.domain.model.LivenessVerificationResult;
import com.kts.kronos.domain.model.enuns.LivenessOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.UUID;

/**
 * Implementação básica de verificação de liveness que valida apenas
 * propriedades da imagem (tamanho, formato).
 *
 * Esta NÃO é uma verificação real de liveness e não deve ser usada
 * em produção. Serve apenas como fallback seguro.
 */
@Slf4j
@Component
public class BasicImageLivenessVerificationProvider implements LivenessVerificationProvider {

    private static final int MIN_BASE64_LENGTH = 100;
    private static final int MAX_BASE64_LENGTH = 5_000_000;
    private static final String PROVIDER_NAME = "BASIC_IMAGE_VALIDATOR";

    @Override
    public LivenessVerificationResult verify(String faceImageBase64, LivenessOperation operation, UUID employeeId) {
        try {
            if (faceImageBase64 == null || faceImageBase64.isBlank()) {
                log.warn("event=liveness_verification_failed reason=empty_image operation={} employeeRef={}",
                        operation, SensitiveDataMasker.maskEmployeeId(employeeId));
                return LivenessVerificationResult.failed(PROVIDER_NAME, "EMPTY_IMAGE");
            }

            if (faceImageBase64.length() < MIN_BASE64_LENGTH) {
                log.warn("event=liveness_verification_failed reason=image_too_small operation={} employeeRef={}",
                        operation, SensitiveDataMasker.maskEmployeeId(employeeId));
                return LivenessVerificationResult.failed(PROVIDER_NAME, "IMAGE_TOO_SMALL");
            }

            if (faceImageBase64.length() > MAX_BASE64_LENGTH) {
                log.warn("event=liveness_verification_failed reason=image_too_large operation={} employeeRef={}",
                        operation, SensitiveDataMasker.maskEmployeeId(employeeId));
                return LivenessVerificationResult.failed(PROVIDER_NAME, "IMAGE_TOO_LARGE");
            }

            // Validar que é base64 válido
            try {
                Base64.getDecoder().decode(faceImageBase64);
            } catch (IllegalArgumentException e) {
                log.warn("event=liveness_verification_failed reason=invalid_base64 operation={} employeeRef={}",
                        operation, SensitiveDataMasker.maskEmployeeId(employeeId));
                return LivenessVerificationResult.failed(PROVIDER_NAME, "INVALID_BASE64");
            }

            log.info("event=liveness_verification_passed operation={} employeeRef={} provider={}",
                    operation, SensitiveDataMasker.maskEmployeeId(employeeId), PROVIDER_NAME);

            // Retornar "passou" com confiança baixa (não é real liveness)
            return LivenessVerificationResult.passed(PROVIDER_NAME, 0.3);

        } catch (Exception e) {
            log.error("event=liveness_verification_error operation={} employeeRef={} error={}",
                    operation, SensitiveDataMasker.maskEmployeeId(employeeId), e.getMessage(), e);
            return LivenessVerificationResult.error(PROVIDER_NAME, "VERIFICATION_ERROR");
        }
    }
}
