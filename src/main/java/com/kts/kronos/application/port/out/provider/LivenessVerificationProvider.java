package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.domain.model.LivenessVerificationResult;
import com.kts.kronos.domain.model.enuns.LivenessOperation;

import java.util.UUID;

public interface LivenessVerificationProvider {
    /**
     * Verifica se a imagem de rosto fornecida passou em validação de liveness.
     *
     * @param faceImageBase64 Imagem do rosto em formato base64
     * @param operation Tipo de operação (enrollment, face-login, checkin)
     * @param employeeId ID do funcionário (pode ser null para login)
     * @return Resultado da verificação de liveness
     */
    LivenessVerificationResult verify(String faceImageBase64, LivenessOperation operation, UUID employeeId);
}
