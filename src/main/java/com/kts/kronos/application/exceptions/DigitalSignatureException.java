package com.kts.kronos.application.exceptions;

/**
 * Lançada quando o serviço de assinatura digital não pode operar
 * (certificado ausente, senha inválida, keystore corrompido ou erro
 * criptográfico interno). É mapeada em {@code RestExceptionHandler}
 * para HTTP 503 com código {@code DIGITAL_SIGNATURE_UNAVAILABLE}.
 *
 * Mensagens devem ser genéricas no caminho usuário; detalhes técnicos
 * permanecem apenas no log do servidor para não vazar path nem senha.
 */
public class DigitalSignatureException extends RuntimeException {
    public DigitalSignatureException(String message) {
        super(message);
    }

    public DigitalSignatureException(String message, Throwable cause) {
        super(message, cause);
    }
}
