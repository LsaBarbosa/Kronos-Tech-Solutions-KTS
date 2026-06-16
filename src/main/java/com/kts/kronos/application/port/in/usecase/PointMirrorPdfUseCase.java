package com.kts.kronos.application.port.in.usecase;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public interface PointMirrorPdfUseCase {
    byte[] generateMirror(UUID employeeId, LocalDate startDate, LocalDate endDate);

    /**
     * Gera o espelho com um carimbo visível indicando assinatura eletrônica do colaborador.
     * O PDF retornado AINDA NÃO está assinado digitalmente pelo certificado da empresa —
     * isso é responsabilidade do {@code DigitalSignatureService.signPdf}.
     */
    byte[] generateMirrorWithSignatureStamp(
            UUID employeeId,
            LocalDate startDate,
            LocalDate endDate,
            SignatureStamp stamp
    );

    record SignatureStamp(
            String signerFullName,
            Instant signedAt,
            String declarationVersion,
            String recordsSnapshotHashSha256
    ) {}
}
