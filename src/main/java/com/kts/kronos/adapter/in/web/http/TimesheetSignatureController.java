package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.timesheetsignature.AdminTimesheetSignaturePageResponse;
import com.kts.kronos.adapter.in.web.dto.timesheetsignature.PreviousMonthSignatureStatusResponse;
import com.kts.kronos.adapter.in.web.dto.timesheetsignature.SignPreviousMonthTimesheetRequest;
import com.kts.kronos.adapter.in.web.dto.timesheetsignature.SignPreviousMonthTimesheetResponse;
import com.kts.kronos.application.port.in.usecase.TimesheetSignatureUseCase;
import com.kts.kronos.application.security.ClientIpResolver;
import com.kts.kronos.domain.model.enuns.TimesheetSignatureStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.kts.kronos.constants.ApiPaths.RECORDS;
import static com.kts.kronos.constants.ApiPaths.TIMESHEET_SIGNATURES;
import static com.kts.kronos.constants.ApiPaths.TIMESHEET_SIGNATURE_ADMIN;
import static com.kts.kronos.constants.ApiPaths.TIMESHEET_SIGNATURE_DOCUMENT;
import static com.kts.kronos.constants.ApiPaths.TIMESHEET_SIGNATURE_PREVIEW;
import static com.kts.kronos.constants.ApiPaths.TIMESHEET_SIGNATURE_SIGN;
import static com.kts.kronos.constants.ApiPaths.TIMESHEET_SIGNATURE_STATUS;

@RestController
@RequestMapping(RECORDS + TIMESHEET_SIGNATURES)
@RequiredArgsConstructor
public class TimesheetSignatureController {

    private final TimesheetSignatureUseCase useCase;
    private final ClientIpResolver clientIpResolver;

    @PreAuthorize("isAuthenticated()")
    @GetMapping(TIMESHEET_SIGNATURE_STATUS)
    public ResponseEntity<PreviousMonthSignatureStatusResponse> status(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        return ResponseEntity.ok(useCase.getMonthStatus(year, month));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping(TIMESHEET_SIGNATURE_PREVIEW)
    public ResponseEntity<byte[]> preview(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            HttpServletRequest httpServletRequest
    ) {
        String ip = clientIpResolver.resolve(httpServletRequest);
        String ua = httpServletRequest.getHeader(HttpHeaders.USER_AGENT);
        byte[] pdf = useCase.previewMonthMirror(year, month, ip, ua);
        String fileName = (year != null && month != null)
                ? String.format("espelho_preview_%04d-%02d.pdf", year, month)
                : "espelho_preview.pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(pdf);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping(TIMESHEET_SIGNATURE_SIGN)
    public ResponseEntity<SignPreviousMonthTimesheetResponse> sign(
            @Valid @RequestBody SignPreviousMonthTimesheetRequest request,
            HttpServletRequest httpServletRequest
    ) {
        String ip = clientIpResolver.resolve(httpServletRequest);
        String userAgent = httpServletRequest.getHeader(HttpHeaders.USER_AGENT);
        return ResponseEntity.ok(useCase.signMonth(request, ip, userAgent));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping(TIMESHEET_SIGNATURE_DOCUMENT)
    public ResponseEntity<byte[]> downloadDocument(@PathVariable UUID signatureId, HttpServletRequest httpServletRequest) {
        String ip = clientIpResolver.resolve(httpServletRequest);
        String ua = httpServletRequest.getHeader(HttpHeaders.USER_AGENT);
        TimesheetSignatureUseCase.SignedDocumentDownload download = useCase.downloadSignatureDocument(signatureId, ip, ua);
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(download.contentType());
        } catch (Exception ignored) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + download.fileName() + "\"")
                .body(download.data());
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'CTO')")
    @GetMapping(TIMESHEET_SIGNATURE_ADMIN)
    public ResponseEntity<AdminTimesheetSignaturePageResponse> admin(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) TimesheetSignatureStatus status,
            @RequestParam(required = false) String employeeName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(useCase.findAdmin(year, month, status, employeeName, page, size));
    }
}
