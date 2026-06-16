package com.kts.kronos.application.port.in.usecase;

import com.kts.kronos.adapter.in.web.dto.timesheetsignature.AdminTimesheetSignaturePageResponse;
import com.kts.kronos.adapter.in.web.dto.timesheetsignature.PreviousMonthSignatureStatusResponse;
import com.kts.kronos.adapter.in.web.dto.timesheetsignature.SignPreviousMonthTimesheetRequest;
import com.kts.kronos.adapter.in.web.dto.timesheetsignature.SignPreviousMonthTimesheetResponse;
import com.kts.kronos.domain.model.enuns.TimesheetSignatureStatus;

import java.util.UUID;

public interface TimesheetSignatureUseCase {

    PreviousMonthSignatureStatusResponse getPreviousMonthStatus();

    byte[] previewPreviousMonthMirror();

    SignPreviousMonthTimesheetResponse signPreviousMonth(
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
