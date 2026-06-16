package com.kts.kronos.application.port.in.usecase;

import com.kts.kronos.adapter.in.web.dto.timesheetsignature.AdminTimesheetSignaturePageResponse;
import com.kts.kronos.adapter.in.web.dto.timesheetsignature.PreviousMonthSignatureStatusResponse;
import com.kts.kronos.adapter.in.web.dto.timesheetsignature.SignPreviousMonthTimesheetRequest;
import com.kts.kronos.adapter.in.web.dto.timesheetsignature.SignPreviousMonthTimesheetResponse;
import com.kts.kronos.domain.model.enuns.TimesheetSignatureStatus;

import java.util.UUID;

public interface TimesheetSignatureUseCase {

    /**
     * Status da assinatura para um mês específico (anterior ao vigente).
     * Se {@code year} e {@code month} forem nulos, usa o mês imediatamente anterior.
     */
    PreviousMonthSignatureStatusResponse getMonthStatus(Integer year, Integer month);

    /**
     * Pré-visualização do espelho de um mês específico (anterior ao vigente).
     * Se {@code year} e {@code month} forem nulos, usa o mês imediatamente anterior.
     */
    byte[] previewMonthMirror(Integer year, Integer month);

    /**
     * Assina o espelho de ponto do mês indicado em {@code request.referenceYear/Month}.
     * O mês deve ser estritamente anterior ao vigente (America/Sao_Paulo).
     */
    SignPreviousMonthTimesheetResponse signMonth(
            SignPreviousMonthTimesheetRequest request,
            String ipAddress,
            String userAgent
    );

    SignedDocumentDownload downloadSignatureDocument(UUID signatureId);

    AdminTimesheetSignaturePageResponse findAdmin(
            Integer year,
            Integer month,
            TimesheetSignatureStatus status,
            String employeeName,
            int page,
            int size
    );

    record SignedDocumentDownload(byte[] data, String fileName, String contentType) {}
}
